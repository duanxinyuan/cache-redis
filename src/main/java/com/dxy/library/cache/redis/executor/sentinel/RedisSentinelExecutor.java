package com.dxy.library.cache.redis.executor.sentinel;

import com.google.common.collect.Sets;
import com.dxy.library.cache.redis.exception.RedisCacheException;
import com.dxy.library.cache.redis.executor.JedisExecutor;
import com.dxy.library.cache.redis.inter.RedisConsumer;
import com.dxy.library.cache.redis.inter.RedisFunction;
import com.dxy.library.cache.redis.properties.RedisProperties;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;

import java.util.Set;

/**
 * Redis哨兵模式缓存器
 * @author duanxinyuan
 * 2018/8/8 18:28
 */
@Slf4j
@Getter
public class RedisSentinelExecutor extends JedisExecutor {

    private JedisSentinelPool jedisSentinelPool;

    public RedisSentinelExecutor(RedisProperties redisProperties) {
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
    public void executeVoid(RedisConsumer<Jedis> consumer) {
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            consumer.accept(jedis);
        } catch (Exception e) {
            throw new RedisCacheException(e);
        }
    }

    @Override
    public <T> T execute(RedisFunction<Jedis, T> function) {
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            return function.apply(jedis);
        } catch (Exception e) {
            throw new RedisCacheException(e);
        }
    }

}
