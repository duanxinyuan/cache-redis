package com.dxy.library.cache.redis.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Redis缓存类型
 * @author duanxinyuan
 * 2018/8/9 15:38
 */
@Getter
@AllArgsConstructor
public enum CacheType {

    //单机模式
    single("single"),

    //哨兵主从模式
    sentinel("sentinel"),

    //分片集群模式
    sharded("sharded"),

    //官方集群模式
    cluster("cluster");

    String type;

    public static CacheType getType(String type) {
        CacheType[] values = values();
        for (CacheType value : values) {
            if (value.getType().equals(type)) {
                return value;
            }
        }
        //默认单机模式
        return single;
    }

}
