package com.dxy.library.cache.redis.pool.sentinel;

import com.dxy.library.cache.redis.properties.RedisProperties;
import com.google.common.collect.Sets;
import com.dxy.library.cache.redis.pool.BasePool;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCommands;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;

import java.util.Set;

/**
 * Redis哨兵模式缓存器
 * @author duanxinyuan
 * 2018/8/8 18:28
 */
@Slf4j
public class RedisSentinelPool extends BasePool {

    private JedisSentinelPool jedisSentinelPool;

    public RedisSentinelPool(RedisProperties redisProperties) {
        super(redisProperties);
    }

    @Override
    public void init(RedisProperties redisProperties) {
        JedisPoolConfig config = initJedisPoolConfig(redisProperties);
        if (redisProperties.getNodes() == null || redisProperties.getNodes().isEmpty()) {
            log.error("redis sentinel init failed, nodes not configured");
            return;
        }
        Set<String> sentinels = Sets.newHashSet(redisProperties.getNodes());

        String masterName = "CacheMaster";

        jedisSentinelPool = new JedisSentinelPool(masterName, sentinels, config, redisProperties.getTimeoutMillis(), redisProperties.getPassword(), redisProperties.getDatabase());
    }

    @Override
    public JedisCommands getJedis() {
        return jedisSentinelPool.getResource();
    }

    @Override
    public void close(JedisCommands jedisCommands) {
        ((Jedis) jedisCommands).close();
    }

}
