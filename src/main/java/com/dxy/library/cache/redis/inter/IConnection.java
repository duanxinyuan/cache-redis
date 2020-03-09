package com.dxy.library.cache.redis.inter;

import redis.clients.jedis.JedisCommands;

/**
 * Redis连接的获取和关闭
 * @author duanxinyuan
 * 2019/4/16 21:58
 */
public interface IConnection {

    JedisCommands getJedis();

    void close(JedisCommands jedisCommands);

}
