package cn.gnosed.shopping.redis;

import redis.clients.jedis.Jedis;

import java.io.IOException;

/**
 * redis 客户端接口
 * @param <T>
 */
public interface RedisClientInvoker<T> {
    T invoke(Jedis jedis) throws IOException;
}
