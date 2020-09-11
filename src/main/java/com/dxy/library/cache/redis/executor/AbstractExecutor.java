package com.dxy.library.cache.redis.executor;

import com.dxy.library.cache.redis.exception.RedisCacheException;
import com.dxy.library.cache.redis.inter.ICommands;
import com.dxy.library.cache.redis.inter.IExecutor;
import com.dxy.library.cache.redis.properties.RedisProperties;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author duanxinyuan
 * 2019/4/16 21:58
 */
public abstract class AbstractExecutor<C> implements IExecutor<C>, ICommands {

    private RedisProperties redisProperties;

    public AbstractExecutor(RedisProperties redisProperties) {
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

    public <T> List<T> transformResponse(Map<String, Response<T>> map) {
        List<T> result = new ArrayList<>(map.size());
        map.forEach((key, response) -> result.add(response.get()));
        return result;
    }

    public void checkNotNull(Object object, Object... objects) {
        checkNotNull(object);
        for (Object o : objects) {
            checkNotNull(o);
        }
    }

    private void checkNotNull(Object object) {
        Objects.requireNonNull(object);
        if (object instanceof Map<?, ?>) {
            if (((Map) object).isEmpty()) {
                throw new RedisCacheException("the params map cannot be null");
            }
        }
    }

}
