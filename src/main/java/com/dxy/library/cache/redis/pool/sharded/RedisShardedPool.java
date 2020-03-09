package com.dxy.library.cache.redis.pool.sharded;

import com.dxy.library.cache.redis.properties.RedisProperties;
import com.dxy.library.cache.redis.pool.BasePool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import redis.clients.jedis.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis分片模式缓存器
 * @author duanxinyuan
 * 2018/8/8 18:45
 */
@Slf4j
public class RedisShardedPool extends BasePool {

    private ShardedJedisPool jedisPool;

    public RedisShardedPool(RedisProperties redisProperties) {
        super(redisProperties);
    }

    @Override
    public void init(RedisProperties redisProperties) {
        JedisPoolConfig config = initJedisPoolConfig(redisProperties);

        if (redisProperties.getNodes() == null || redisProperties.getNodes().isEmpty()) {
            log.error("redis sharded init failed, nodes not configured");
            return;
        }
        List<JedisShardInfo> shards = new ArrayList<>();
        for (String hostPort : redisProperties.getNodes()) {
            String[] strings = hostPort.split(":");
            String host = strings[0];
            int port = strings.length > 1 ? NumberUtils.toInt(strings[1].trim(), 6379) : 6379;
            JedisShardInfo jedisShardInfo = new JedisShardInfo(host, port, redisProperties.getTimeoutMillis());
            if (StringUtils.isNotEmpty(redisProperties.getPassword())) {
                jedisShardInfo.setPassword(redisProperties.getPassword());
            }
            shards.add(jedisShardInfo);
        }
        jedisPool = new ShardedJedisPool(config, shards);
    }

    @Override
    public JedisCommands getJedis() {
        return jedisPool.getResource();
    }

    @Override
    public void close(JedisCommands jedisCommands) {
        ((ShardedJedis) jedisCommands).close();
    }

}
