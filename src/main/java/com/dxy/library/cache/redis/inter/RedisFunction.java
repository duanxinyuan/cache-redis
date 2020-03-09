package com.dxy.library.cache.redis.inter;

/**
 * @author duanxinyuan
 * 2019/8/14 21:57
 */
public interface RedisFunction<T, R> {

    R apply(T t) throws Exception;

}
