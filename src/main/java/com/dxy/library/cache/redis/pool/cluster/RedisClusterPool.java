package com.dxy.library.cache.redis.pool.cluster;

import com.dxy.library.cache.redis.properties.RedisProperties;
import com.dxy.library.cache.redis.pool.BasePool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisCommands;
import redis.clients.jedis.JedisPoolConfig;

import java.util.HashSet;

/**
 * Redis集群模式缓存器
 * @author duanxinyuan
 * 2018/8/8 17:52
 */
@Slf4j
public class RedisClusterPool extends BasePool {

    private JedisCluster jedisCluster;

    public RedisClusterPool(RedisProperties redisProperties) {
        super(redisProperties);
    }

    @Override
    public void init(RedisProperties redisProperties) {
        JedisPoolConfig config = initJedisPoolConfig(redisProperties);
        if (redisProperties.getNodes() == null || redisProperties.getNodes().isEmpty()) {
            log.error("redis cluster init failed, nodes not configured");
            return;
        }
        HashSet<HostAndPort> hostSet = new HashSet<>();
        for (String hostPort : redisProperties.getNodes()) {
            String[] strings = hostPort.split(":");
            int port = strings.length > 1 ? NumberUtils.toInt(strings[1].trim(), 6379) : 6379;
            hostSet.add(new HostAndPort(strings[0], port));
        }
        int connectionTimeout = redisProperties.getTimeoutMillis();
        int soTimeout = redisProperties.getTimeoutMillis();
        //重试次数，默认5次
        int maxAttempts = 5;
        if (StringUtils.isEmpty(redisProperties.getPassword())) {
            jedisCluster = new JedisCluster(hostSet, connectionTimeout, soTimeout, maxAttempts, config);
        } else {
            jedisCluster = new JedisCluster(hostSet, connectionTimeout, soTimeout, maxAttempts, redisProperties.getPassword(), config);
        }
    }

    @Override
    public JedisCommands getJedis() {
        return jedisCluster;
    }

    @Override
    public void close(JedisCommands jedisCommands) {
        //jedisCluster无需close
    }

}
