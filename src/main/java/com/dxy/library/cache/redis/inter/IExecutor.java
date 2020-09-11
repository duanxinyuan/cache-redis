package com.dxy.library.cache.redis.inter;

/**
 * Redis连接的获取和关闭
 * @author duanxinyuan
 * 2019/4/16 21:58
 */
public interface IExecutor<C> {

    /**
     * 自定义操作方法
     */
    void executeVoid(RedisConsumer<C> consumer);

    /**
     * 自定义操作方法
     */
    <T> T execute(RedisFunction<C, T> function);

}
