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
        String single = "single";

        String shard = "shard";

        String cluster = "cluster";
    }

}
