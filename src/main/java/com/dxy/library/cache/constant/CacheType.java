package com.dxy.library.cache.constant;

/**
 * @author duanxinyuan
 * 2018/8/9 15:38
 */
public interface CacheType {

    /**
     * 内存缓存类型
     */
    interface Memory {
        String guava = "guava";

        String caffeine = "caffeine";
    }

    /**
     * Redis缓存类型
     */
    interface Redis {
        //单机模式
        String single = "single";

        //哨兵主从模式
        String sentinel="sentinel";

        //分片集群模式
        String shard = "shard";

        //官方集群模式
        String cluster = "cluster";
    }

}
