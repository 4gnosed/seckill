package cn.gnosed.shopping.redis;

import cn.gnosed.shopping.base.AbstractClass;
import cn.gnosed.shopping.entity.Good;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import redis.clients.jedis.Jedis;

import java.util.concurrent.TimeUnit;

/**
 * @author: LuDeSong
 * @Date: 2020-7-23 11:57
 * @Description:
 */

@SpringBootTest
@RunWith(SpringRunner.class)
public class RedisCacheTest extends AbstractClass {

    @Autowired
    private RedisCache redisCache;

    /**
     * 测试redis连接
     */
    @Test
    public void connectRedisByJedis() {
        //连接本地的redis
        Jedis jedis = new Jedis("localhost");
        logger.info(""+jedis.getClient().getPort());
        //查看服务是否运行
        logger.info("连接本地的Redis服务器成功,服务正在运行：" + jedis.ping());
    }

    /**
     * 存储字符串
     */
    @Test
    public void testPutString() {
        String key = "test_string_2";
        String value = "string_value";
        logger.info("存储字符串开始 key={} value={}", key, value);
        boolean putFlag = redisCache.set(key, "string_value", 0, TimeUnit.MINUTES);
        logger.info("存储字符串结束 putFlag={}", putFlag);
        String rvalue = redisCache.get(key);
        logger.info("存储字符串-> 查询缓存value={}", rvalue);
    }
    /**
     * 存储自定义对象
     */
    @Test
    public void testPutObject() {
        String key = "test_object_good_2";
        Good good = new Good();
        good.setId(1234);
        good.setName("商品");
        good.setStock(1000);

        logger.info("存储对象开始 key={} value={}", key, good);
        redisCache.set(key, good);
        logger.info("存储对象结束");
        Good value = redisCache.get(key, Good.class);
        logger.info("存储对象-> 查询缓存value={}", value);
    }

}