package com.dxy.library.cache.redis.pool;

import com.dxy.library.cache.redis.inter.IConnection;
import com.dxy.library.cache.redis.properties.RedisProperties;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @author duanxinyuan
 * 2019/4/16 21:58
 */
public abstract class BasePool implements IConnection {

    private RedisProperties redisProperties;

    public BasePool(RedisProperties redisProperties) {
        this.redisProperties = redisProperties;
        init(redisProperties);
    }

    public RedisProperties getRedisProperties() {
        return redisProperties;
    }

    public void setRedisProperties(RedisProperties redisProperties) {
        this.redisProperties = redisProperties;
        init(redisProperties);
    }

    public abstract void init(RedisProperties redisProperties);

    public JedisPoolConfig initJedisPoolConfig(RedisProperties redisProperties) {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(redisProperties.getMaxTotal());
        config.setMaxIdle(redisProperties.getMaxIdle());
        config.setMaxWaitMillis(redisProperties.getMaxWaitMillis());
        config.setTestOnBorrow(true);
        config.setTestWhileIdle(true);
        return config;
    }

}
