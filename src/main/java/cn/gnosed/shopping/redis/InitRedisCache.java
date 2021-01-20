package cn.gnosed.shopping.redis;

import cn.gnosed.shopping.entity.Good;
import cn.gnosed.shopping.service.IGoodService;
import cn.gnosed.shopping.util.DateTimeUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author: LuDeSong
 * @Date: 2020-7-23 16:05
 * @Description: Springboot中CommandLineRunner接口的 Component 会在所有 Spring Beans都初始化之后SpringApplication.run()之前执行，
 * 初始化商品记录作为缓存，默认有缓存DEFAULT_NUMBER条商品记录，过期时间DEFAULT_EXPIRE_MINUTES分钟
 */

@Component
public class InitRedisCache implements CommandLineRunner {
    private static Logger logger = LoggerFactory.getLogger(InitRedisCache.class);

    public static final int DEFAULT_NUMBER = 5;

    @Autowired
    IGoodService iGoodService;
    @Autowired
    RedisCache redisCache;

    @Override
    public void run(String... args) throws Exception {
        //获取商品记录
        QueryWrapper<Good> q = new QueryWrapper<>();
        q.last("limit 0," + DEFAULT_NUMBER);
        List<Good> goodList = iGoodService.list(q);
        logger.info("初始化缓存：商品记录" + goodList);
        //加载至redis，key为商品ID，value为商品记录对象
        for (Good good : goodList) {
            Boolean putFlag = redisCache.set(Integer.toString(good.getId()), good,
                    DateTimeUtil.getRandomInt(RedisCache.LEAST_CACHE_TIME, RedisCache.MOST_CACHE_TIME), TimeUnit.MINUTES);
            if (!putFlag) {
                return;
            }
        }
        logger.info("初始化完成……");
    }
}
