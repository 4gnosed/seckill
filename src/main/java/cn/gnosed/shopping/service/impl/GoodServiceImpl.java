package cn.gnosed.shopping.service.impl;

import cn.gnosed.shopping.constant.Constant;
import cn.gnosed.shopping.entity.Good;
import cn.gnosed.shopping.mapper.GoodMapper;
import cn.gnosed.shopping.redis.RedisCache;
import cn.gnosed.shopping.redis.RedisDistributedLock;
import cn.gnosed.shopping.result.Result;
import cn.gnosed.shopping.result.ResultFactory;
import cn.gnosed.shopping.service.IGoodService;
import cn.gnosed.shopping.service.IOrderService;
import cn.gnosed.shopping.util.DateTimeUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.base.Charsets;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author Gnosed Lu
 * @since 2020-07-22
 */
@Service
public class GoodServiceImpl extends ServiceImpl<GoodMapper, Good> implements IGoodService {
    private static Logger logger = LoggerFactory.getLogger(GoodServiceImpl.class);

    //布隆过滤器，记录不存在商品的key
    private static final int DEFAULT_INSERTIONS = 1000;
    private static BloomFilter<String> bf = BloomFilter.create(Funnels.stringFunnel(Charsets.UTF_8), DEFAULT_INSERTIONS);

    @Autowired
    IOrderService iOrderService;
    @Autowired
    RedisCache redisCache;

    /**
     * 1.获取商品记录（缓存或者数据库）
     * 2.商品库存更新（缓存与数据库）
     * 3.订单生存（数据库）
     * 从redis缓存层面上，必须保证这三个步骤的缓存一致性
     * 从业务层面上，必须保证后两个步骤的数据库事物原子性
     *
     * @param goodId
     * @param quantity
     * @param userId
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean buy(String goodId, Integer quantity, Integer userId) {
        Good good = null;
        String goodKey = goodId;


        RedisDistributedLock redisDistributedLock = null;
        String unLockIdentify = null;
        String lockKey = Constant.LOCK + goodKey;
        try {
            redisDistributedLock = new RedisDistributedLock(redisCache, lockKey);

            // 缓存击穿：某一个key在高并发量前正好失效，则所有请求将会转到数据库
            // 解决：分布式锁（redis互斥锁）
            // 获取锁，返回该redis客户端获取该锁的唯一标示，以便后续该客户端释放锁
            unLockIdentify = redisDistributedLock.acquire();

            if (unLockIdentify == null) {
                // 获取锁超时
                return false;
            }

            // 获取锁成功之后，检索商品记录
            boolean exists = redisCache.exists(goodKey);
            if (exists) {
                // 缓存中有该商品记录
                good = redisCache.get(goodKey, Good.class);
                logger.info("获取商品缓存成功:{}", JSON.toJSONString(good));
            } else {
                // 缓存穿透：数据库中无记录，之后该key的每次请求都会转发到数据库
                // 解决：1.缓存该商品value为默认值并且设置过期时间，
                // 2.布隆过滤器，有一定误判率，即返回key存在实际上不存在，查一次数据库可以忍受
                //查询布隆过滤器，key存在则直接返回**********
                if (bf.mightContain(goodKey)) {
                    iOrderService.failPlace(goodId, quantity, userId);
                    logger.warn("商品不存在");
                    return false;
                }

                // 查询数据库
                QueryWrapper<Good> q = new QueryWrapper<>();
                q.eq(Constant.GOOD_ID, goodId).last("limit 1");
                good = getOne(q);
                logger.info("请求数据库查询商品:{}", JSON.toJSONString(good));

                //**********商品为空，则插入key到布隆过滤器
                if (good == null) {
                    bf.put(goodKey);
                }

                // 缓存雪崩：缓存采用相同的过期时间，导致多个key在某一个时间失效，全部请求转发到数据库
                // 解决：过期时间设置为随机值
                Boolean putFlag = redisCache.set(goodKey, good,
                        DateTimeUtil.getRandomInt(RedisCache.LEAST_CACHE_TIME, RedisCache.MOST_CACHE_TIME), TimeUnit.MINUTES);

                if (!putFlag) {
                    logger.warn("更新缓存失败");
                } else {
                    logger.info("更新缓存成功");
                }
            }

            // 库存是否大于等于此订单商品数量
            Integer stock = good.getStock();
            int newStock = stock - quantity;
            if (newStock >= 0) {
                // 是则下单成功
                // 更新redis记录
                good.setStock(newStock);
                Boolean putFlag = redisCache.set(goodKey, good,
                        DateTimeUtil.getRandomInt(RedisCache.LEAST_CACHE_TIME, RedisCache.MOST_CACHE_TIME), TimeUnit.MINUTES);
                logger.info("商品" + goodKey + "库存减少" + quantity + "等于" + newStock + "，更新缓存成功");

                // 并持久化库存
                UpdateWrapper<Good> u = new UpdateWrapper<>();
                u.eq(Constant.GOOD_ID, goodId).set(Constant.STOCK, newStock);
                update(u);
                logger.info("商品记录持久化成功");

                iOrderService.successPlace(goodId, quantity, userId);
                logger.info("订单记录持久化成功");

                return true;
            } else {
                //否则下单失败
                iOrderService.failPlace(goodId, quantity, userId);
                logger.warn("库存不足");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        } finally {
            redisDistributedLock.release(unLockIdentify);
        }

    }

    @Override
    public Good getGood(String goodId) {
        QueryWrapper<Good> q = new QueryWrapper<>();
        q.eq(Constant.GOOD_ID, goodId);
        Good good = getOne(q);
        logger.info("查询商品:{}", JSON.toJSONString(good));
        return good;
    }


}
