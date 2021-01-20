package cn.gnosed.shopping.redis;

import cn.gnosed.shopping.base.AbstractClass;

import java.util.Collections;
import java.util.UUID;

/**
 * @author: LuDeSong
 * @Date: 2020-7-24 11:28
 * @Description: Redis实现分布式锁
 * 加锁操作的正确姿势为：
 * 　　1. 使用setnx命令保证互斥性
 * 　　2. 需要设置锁的过期时间，避免死锁
 * 　　3. setnx和设置过期时间需要保持原子性，避免在设置setnx成功之后在设置过期时间客户端崩溃导致死锁
 * 　　4. 加锁的Value 值为一个唯一标示。可以采用UUID作为唯一标示。加锁成功后需要把唯一标示返回给客户端来用来客户端进行解锁操作
 * 解锁的正确姿势为：
 * 　　1. 需要拿加锁成功的唯一标示要进行解锁，从而保证加锁和解锁的是同一个客户端
 * 　　2. 解锁操作需要比较唯一标示是否相等，相等再执行删除操作。这2个操作可以采用Lua脚本方式使2个命令的原子性。
 */

public class RedisDistributedLock extends AbstractClass implements DistributedLock {

    private static final String LOCK_SUCCESS = "OK";
    private static final Long RELEASE_SUCCESS = 1L;
    private static final String SET_IF_NOT_EXIST = "NX";
    private static final String SET_WITH_EXPIRE_TIME = "PX";

    /**
     * jedis 工具类
     */
    private RedisCache redisCache;

    /**
     * 分布式锁的键值
     */
    private String lockKey;

    /**
     * 锁的有效时间
     */
    int expireTime = 10 * 1000;

    /**
     * 锁等待，防止线程饥饿
     */
    int acquireTimeout = 2 * 1000;

    /**
     * 获取指定键值的锁
     *
     * @param redisCache jedis 工具类
     * @param lockKey    锁的键值
     */
    public RedisDistributedLock(RedisCache redisCache, String lockKey) {
        this.redisCache = redisCache;
        this.lockKey = lockKey;
    }

    /**
     * 获取指定键值的锁,同时设置获取锁超时时间
     *
     * @param redisCache     jedis 工具类
     * @param lockKey        锁的键值
     * @param acquireTimeout 获取锁超时时间
     */
    public RedisDistributedLock(RedisCache redisCache, String lockKey, int acquireTimeout) {
        this.redisCache = redisCache;
        this.lockKey = lockKey;
        this.acquireTimeout = acquireTimeout;
    }

    /**
     * 获取指定键值的锁,同时设置获取锁超时时间和锁过期时间
     *
     * @param redisCache     jedis 工具类
     * @param lockKey        锁的键值
     * @param acquireTimeout 获取锁超时时间
     * @param expireTime     锁失效时间
     */
    public RedisDistributedLock(RedisCache redisCache, String lockKey, int acquireTimeout, int expireTime) {
        this.redisCache = redisCache;
        this.lockKey = lockKey;
        this.acquireTimeout = acquireTimeout;
        this.expireTime = expireTime;
    }

    /**
     * 获取锁
     *
     * @return 获取锁成功则返回唯一的标示
     */
    @Override
    public String acquire() {
        String requireToken = null;
        try {
            // 获取锁的超时时间，超过这个时间则放弃获取锁
            long end = System.currentTimeMillis() + acquireTimeout;
            // 随机生成一个value
            requireToken = UUID.randomUUID().toString();
            while (System.currentTimeMillis() < end) {
//                String result = jedis.set(lockKey, requireToken, SET_IF_NOT_EXIST, SET_WITH_EXPIRE_TIME, expireTime);
                String result = redisCache.set(lockKey, requireToken, SET_IF_NOT_EXIST, SET_WITH_EXPIRE_TIME, expireTime);
                if (LOCK_SUCCESS.equals(result)) {
                    logger.info("获取锁成功，lockKey：" + lockKey + "，唯一标示：" + requireToken);
                    return requireToken;
                } else {
                    logger.info("等待锁，lockKey：" + lockKey + "，唯一标示：" + requireToken);
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } catch (Exception e) {
            logger.error("获取锁出现异常", e);
        }
        logger.warn("获取锁超时，lockKey：" + lockKey + "，唯一标示：" + requireToken);
        return null;
    }

    /**
     * 释放锁
     *
     * @param identify
     * @return 返回释放结果
     */
    @Override
    public boolean release(String identify) {
        if (identify == null) {
            return false;
        }

        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Object result = null;
        try {
//            result = jedis.eval(script, Collections.singletonList(lockKey), Collections.singletonList(identify));
            result = redisCache.eval(script, Collections.singletonList(lockKey), Collections.singletonList(identify));
            if (RELEASE_SUCCESS.equals(result)) {
                logger.info("释放锁成功，lockKey：" + lockKey + "，唯一标示：" + identify);
                return true;
            }
        } catch (Exception e) {
            logger.error("释放锁出现异常：", e);
        }

        logger.error("释放锁失败，lockKey：" + lockKey + "，唯一标示：" + identify);
        return false;
    }
}
