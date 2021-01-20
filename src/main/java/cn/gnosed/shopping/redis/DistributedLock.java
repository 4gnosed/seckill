package cn.gnosed.shopping.redis;

/**
 * @author: LuDeSong
 * @Date: 2020-7-24 11:25
 * @Description:
 */

public interface DistributedLock {
    /**
     * 获取锁
     *
     * @return 锁标识
     */
    String acquire();

    /**
     * 释放锁
     *
     * @param indentifier
     * @return
     */
    boolean release(String indentifier);
}
