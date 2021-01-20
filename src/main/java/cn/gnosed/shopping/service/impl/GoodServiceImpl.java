package cn.gnosed.shopping.service.impl;

import cn.gnosed.shopping.constant.Constant;
import cn.gnosed.shopping.entity.Good;
import cn.gnosed.shopping.mapper.GoodMapper;
import cn.gnosed.shopping.redis.RedisCache;
import cn.gnosed.shopping.redis.RedisDistributedLock;
import cn.gnosed.shopping.service.IGoodService;
import cn.gnosed.shopping.service.IOrderService;
import cn.gnosed.shopping.util.DateTimeUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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

    @Autowired
    IOrderService iOrderService;
    @Autowired
    RedisCache redisCache;

    /**
     * 获取商品记录（缓存或者数据库）——> 商品库存更新（缓存与数据库） ——> 订单生存（数据库）
     * 从redis缓存层面上，必须保证这三个步骤的缓存一致性
     * 从业务层面上，必须保证后两个步骤的数据库事物原子性
     *
     * @param goodId
     * @param quantity
     * @return
     */
    @Transactional
    @Override
    public boolean buy(Integer goodId, Integer quantity) {
        Good good = null;
        String goodKey = Integer.toString(goodId);

        // 缓存击穿：某一个key在高并发量前正好失效，则所有请求将会转到数据库
        // 解决：分布式锁
        RedisDistributedLock redisDistributedLock = null;
        String unLockIdentify = null;
        String lockKey = Constant.LOCK + goodKey;
        try {
            redisDistributedLock = new RedisDistributedLock(redisCache, lockKey);

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
                logger.info("获取商品缓存成功，" + good.toString());
            } else {
                // 缓存不存在该商品则请求数据库，并添加到缓存中
                QueryWrapper<Good> q = new QueryWrapper<>();
                q.eq(Constant.ID, goodId).last("limit 1");
                good = getOne(q);

                // 缓存穿透：数据库中无记录，之后该key的每次请求都会转发到数据库
                // 解决：缓存该商品value为null并且设置过期时间
                logger.info("请求数据库成功");
                logger.info("商品：" + good.toString());

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
                u.eq(Constant.ID, goodId).set(Constant.STOCK, newStock);
                update(u);
                logger.info("商品记录持久化成功");

                iOrderService.successPlace(goodId, quantity, newStock);
                logger.info("订单记录持久化成功");

                return true;
            } else {
                //否则下单失败
                iOrderService.failPlace(goodId, quantity, stock);
                logger.warn("库存不足，下单失败");
                return false;
            }
        }catch (Exception e){
            e.printStackTrace();
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }finally {
            redisDistributedLock.release(unLockIdentify);
        }

    }
}
