package com.dxy.library.cache.redis.constant;

/**
 * Redis缓存类型
 * @author duanxinyuan
 * 2018/8/9 15:38
 */
public interface CacheType {

    //单机模式
    String single = "single";

    //哨兵主从模式
    String sentinel = "sentinel";

    //分片集群模式
    String sharded = "sharded";

    //官方集群模式
    String cluster = "cluster";

}
