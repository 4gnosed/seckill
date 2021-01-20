package cn.gnosed.shopping.redis;

import cn.gnosed.shopping.base.AbstractClass;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;

import java.io.IOException;

/**
 * @author: LuDeSong
 * @Date: 2020-7-23 14:44
 * @Description: redis 客户端工具类
 */

@Component
public class RedisClient extends AbstractClass {
    public <T> T invoke(JedisPool pool, RedisClientInvoker<T> clients) {
        T obj = null;
        Jedis jedis = null;
        try {
            jedis = pool.getResource();
            obj = clients.invoke(jedis);
        } catch (JedisException | IOException ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            if (jedis != null) {
                if (jedis.isConnected())
                    jedis.close();
            }
        }
        return obj;
    }

}
