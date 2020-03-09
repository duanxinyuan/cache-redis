package com.dxy.library.cache.redis.inter;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.ShardedJedis;

/**
 * Redis操作回调，用于处理不同工作模式下的差异化操作
 * @author duanxinyuan
 * 2019/4/17 17:32
 */
public interface CommandsSupplier<R> {

    /**
     * 单机 / 主从模式
     */
    default R single(Jedis jedis) throws Exception {
        return null;
    }

    /**
     * 哨兵模式
     */
    default R sentinel(Jedis jedis) throws Exception {
        return null;
    }

    /**
     * 分片模式
     */
    default R sharded(ShardedJedis shardedJedis) throws Exception {
        return null;
    }

    /**
     * 集群模式
     */
    default R cluster(JedisCluster jedisCluster) throws Exception {
        return null;
    }

}
