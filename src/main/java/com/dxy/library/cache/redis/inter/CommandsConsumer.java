package com.dxy.library.cache.redis.inter;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.ShardedJedis;

/**
 * @author duanxinyuan
 * 2019/8/26 14:38
 */
public interface CommandsConsumer {
    /**
     * 单机 / 主从模式
     */
    default void single(Jedis jedis) throws Exception {
    }

    /**
     * 哨兵模式
     */
    default void sentinel(Jedis jedis) throws Exception {
    }

    /**
     * 分片模式
     */
    default void sharded(ShardedJedis shardedJedis) throws Exception {
    }

    /**
     * 集群模式
     */
    default void cluster(JedisCluster jedisCluster) throws Exception {
    }

}
