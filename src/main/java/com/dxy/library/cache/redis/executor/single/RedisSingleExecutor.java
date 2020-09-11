package com.dxy.library.cache.redis.executor.single;

import com.dxy.library.cache.redis.exception.RedisCacheException;
import com.dxy.library.cache.redis.executor.JedisExecutor;
import com.dxy.library.cache.redis.inter.RedisConsumer;
import com.dxy.library.cache.redis.inter.RedisFunction;
import com.dxy.library.cache.redis.properties.RedisProperties;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Redis单机模式缓存器
 * @author duanxinyuan
 * 2018/8/8 18:28
 */
@Slf4j
@Getter
public class RedisSingleExecutor extends JedisExecutor {

    private JedisPool jedisPool;

    public RedisSingleExecutor(RedisProperties redisProperties) {
        super(redisProperties);
    }

    @Override
    public void init(RedisProperties redisProperties) {
        JedisPoolConfig config = initJedisPoolConfig(redisProperties);
        if (redisProperties.getNodes() == null || redisProperties.getNodes().isEmpty()) {
            log.error("redis single init failed, nodes not configured");
            return;
        }
        String[] strings = redisProperties.getNodes().get(0).split(":");
        String host = strings[0];
        int port = strings.length > 1 ? NumberUtils.toInt(strings[1].trim(), 6379) : 6379;

        jedisPool = new JedisPool(config, host, port, redisProperties.getTimeoutMillis(), redisProperties.getPassword(), redisProperties.getDatabase());
    }

    @Override
    public void executeVoid(RedisConsumer<Jedis> consumer) {
        try (Jedis jedis = jedisPool.getResource()) {
            consumer.accept(jedis);
        } catch (Exception e) {
            throw new RedisCacheException(e);
        }
    }

    @Override
    public <T> T execute(RedisFunction<Jedis, T> function) {
        try (Jedis jedis = jedisPool.getResource()) {
            return function.apply(jedis);
        } catch (Exception e) {
            throw new RedisCacheException(e);
        }
    }

}
