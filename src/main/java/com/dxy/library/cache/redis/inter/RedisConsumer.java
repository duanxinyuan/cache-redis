package com.dxy.library.cache.redis.inter;

/**
 * @author duanxinyuan
 * 2019/8/26 14:57
 */
public interface RedisConsumer<T> {

    void accept(T t) throws Exception;

}
