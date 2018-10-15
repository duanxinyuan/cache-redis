package com.dxy.library.cache;

import com.dxy.library.cache.constant.CacheType;
import com.dxy.library.cache.memory.IMemory;
import com.dxy.library.cache.memory.caffeine.CacheCaffeine;
import com.dxy.library.cache.memory.guava.CacheGuava;
import com.dxy.library.cache.redis.IRedis;
import com.dxy.library.cache.redis.cluster.CacheRedisCluster;
import com.dxy.library.cache.redis.shard.CacheRedisShard;
import com.dxy.library.cache.redis.single.CacheRedisSingle;
import com.dxy.library.json.gson.GsonUtil;
import com.dxy.library.util.common.config.ConfigUtils;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import redis.clients.jedis.BitOP;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 缓存操作类，部分缓存交由内存+Redis的模式进行，但仅限于不频繁变更的内容
 * @author duanxinyuan
 * 2018/8/9 15:27
 */
public class Cache {
    private static boolean IS_MEMORY_ENABLE;

    private volatile static IMemory memory;
    private volatile static IRedis redis;

    static {
        IS_MEMORY_ENABLE = BooleanUtils.toBoolean(ConfigUtils.getConfig("cache.memory.enable", Boolean.class));
        if (IS_MEMORY_ENABLE) {
            String memoryCacheType = ConfigUtils.getConfig("cache.memory.type");
            switch (memoryCacheType) {
                case CacheType.Memory.caffeine:
                    memory = new CacheCaffeine();
                    break;
                case CacheType.Memory.guava:
                    memory = new CacheGuava();
                    break;
                default:
                    break;
            }
        }

        String redisCacheType = ConfigUtils.getConfig("cache.redis.type");
        switch (redisCacheType) {
            case CacheType.Redis.single:
                redis = new CacheRedisSingle();
                break;
            case CacheType.Redis.shard:
                redis = new CacheRedisShard();
                break;
            case CacheType.Redis.cluster:
                redis = new CacheRedisCluster();
                break;
            default:
                break;
        }
    }

    public static <T> String set(String key, T value) {
        if (IS_MEMORY_ENABLE) {
            memory.set(key, value);
        }
        return redis.set(key, value);
    }

    public static <T> String set(String key, T value, int seconds) {
        if (IS_MEMORY_ENABLE) {
            memory.set(key, value);
        }
        return redis.set(key, value, seconds);
    }


    public static <T> Long setnx(String key, T value) {
        return redis.setnx(key, value);
    }

    public static <T> Long setnx(String key, T value, int seconds) {
        return redis.setnx(key, value, seconds);
    }

    public static String get(String key) {
        String value;
        if (IS_MEMORY_ENABLE) {
            value = memory.get(key);
            if (StringUtils.isNotEmpty(value)) {
                return value;
            }
        }

        value = redis.get(key);
        if (IS_MEMORY_ENABLE && StringUtils.isNotEmpty(value)) {
            memory.set(key, value);
        }
        return value;
    }

    public static <T> T get(String key, Class<T> c) {
        String value;
        if (IS_MEMORY_ENABLE) {
            value = memory.get(key);
            if (StringUtils.isNotEmpty(value)) {
                return GsonUtil.from(value, c);
            }
        }

        value = redis.get(key);
        if (IS_MEMORY_ENABLE && StringUtils.isNotEmpty(value)) {
            memory.set(key, value);
        }
        return GsonUtil.from(value, c);
    }

    public static <T> T get(String key, TypeToken<T> typeToken) {
        String value = null;
        if (IS_MEMORY_ENABLE) {
            value = memory.get(key);
        }
        if (StringUtils.isNotEmpty(value)) {
            return GsonUtil.from(value, typeToken);
        }

        value = redis.get(key);
        if (IS_MEMORY_ENABLE && StringUtils.isNotEmpty(value)) {
            memory.set(key, value);
        }
        return GsonUtil.from(value, typeToken);
    }

    public static Long incr(String key, Integer value, int seconds) {
        if (IS_MEMORY_ENABLE) {//清除内存中的数据，防止脏读
            memory.del(key);
        }
        return redis.incr(key, value, seconds);
    }

    public static Long incr(String key, Integer value) {
        if (IS_MEMORY_ENABLE) {//清除内存中的数据，防止脏读
            memory.del(key);
        }
        return redis.incr(key, value);
    }

    public static Long decr(String key, Integer value) {
        if (IS_MEMORY_ENABLE) {//清除内存中的数据，防止脏读
            memory.del(key);
        }
        return redis.decr(key, value);
    }

    public static Long decr(String key, Integer value, int seconds) {
        if (IS_MEMORY_ENABLE) {//清除内存中的数据，防止脏读
            memory.del(key);
        }
        return redis.decr(key, value, seconds);
    }

    public static Long expire(String key, int seconds) {
        return redis.expire(key, seconds);
    }

    public static Long persist(String key) {
        return redis.persist(key);
    }

    public static boolean exist(String key) {
        //redis数据类型太多，判断是否存在之后无法回设内存
        return redis.exist(key);
    }

    public static Long del(String key) {
        if (IS_MEMORY_ENABLE) {
            memory.del(key);
        }
        return redis.del(key);
    }

    public static void del(String... keys) {
        if (IS_MEMORY_ENABLE) {
            memory.del(keys);
        }
        redis.del(keys);
    }

    public static <T> Long lpush(String key, T value) {
        return redis.lpush(key, value);
    }

    public static <T> Long lpush(String key, T value, int seconds) {
        return redis.lpush(key, value, seconds);
    }

    public static <T> Long lpush(String key, List<T> values) {
        return redis.lpush(key, values);
    }

    public static <T> Long lpush(String key, List<T> values, int seconds) {
        return redis.lpush(key, values, seconds);
    }

    public static <T> Long rpush(String key, T value) {
        return redis.rpush(key, value);
    }

    public static <T> Long rpush(String key, T value, int seconds) {
        return redis.rpush(key, value, seconds);
    }

    public static <T> Long rpush(String key, List<T> values) {
        return redis.rpush(key, values);
    }

    public static <T> Long rpush(String key, List<T> values, int seconds) {
        return redis.rpush(key, values, seconds);
    }


    public static List<String> lrange(String key) {
        return redis.lrange(key);
    }

    public static <T> List<T> lrange(String key, Class<T> c) {
        return redis.lrange(key, c);
    }

    public static List<String> lrange(String key, long end) {
        return redis.lrange(key, end);
    }

    public static <T> List<T> lrange(String key, long end, Class<T> c) {
        return redis.lrange(key, end, c);
    }

    public static List<String> lrange(String key, long start, long end) {
        return redis.lrange(key, start, end);
    }

    public static <T> List<T> lrange(String key, long start, long end, Class<T> c) {
        return redis.lrange(key, start, end, c);
    }

    public static List<String> lrangePage(String key, int pageNo, int pageSize) {
        return redis.lrangePage(key, pageNo, pageSize);
    }

    public static <T> List<T> lrangePage(String key, int pageNo, int pageSize, Class<T> c) {
        return redis.lrangePage(key, pageNo, pageSize, c);
    }

    public static String lindex(String key, int index) {
        return redis.lindex(key, index);
    }

    public static <T> T lindex(String key, int index, Class<T> c) {
        return redis.lindex(key, index, c);
    }

    public static Long llen(String key) {
        return redis.llen(key);
    }

    public static void lclear(String key) {
        redis.lclear(key);
    }

    public static Long lrem(String key, String value) {
        return redis.lrem(key, value);
    }

    public static <T> Long lrem(String key, T value) {
        return redis.lrem(key, value);
    }

    public static Long lrem(String key, long count, String value) {
        return redis.lrem(key, count, value);
    }

    public static <T> Long lrem(String key, long count, T value) {
        return redis.lrem(key, count, value);
    }

    public static String ltrim(String key, long start, long end) {
        return redis.ltrim(key, start, end);
    }

    public static String lpop(String key) {
        return redis.lpop(key);
    }

    public static String rpop(String key) {
        return redis.rpop(key);
    }

    public static Long sadd(String key, String... values) {
        if (IS_MEMORY_ENABLE) {//清除内存中的数据，防止脏读
            memory.del(key);
        }
        return redis.sadd(key, values);
    }

    public static Long sadd(String key, int seconds, String... values) {
        if (IS_MEMORY_ENABLE) {//清除内存中的数据，防止脏读
            memory.del(key);
        }
        return redis.sadd(key, seconds, values);
    }

    public static boolean sismember(String key, String value) {
        return redis.sismember(key, value);
    }

    public static Set<String> smembers(String key) {
        Set<String> set;
        if (IS_MEMORY_ENABLE) {
            set = memory.get(key);
            if (set != null && set.size() != 0) {
                return set;
            }
        }

        set = redis.smembers(key);
        if (IS_MEMORY_ENABLE && set != null && set.size() != 0) {
            memory.set(key, set);
        }
        return set;
    }

    public static <T> Long hset(String key, String field, T value) {
        if (IS_MEMORY_ENABLE) {//清除内存中的数据，防止脏读
            memory.del(key);
        }
        return redis.hset(key, field, value);
    }

    public static String hmset(String key, String... values) {
        if (IS_MEMORY_ENABLE) {//清除内存中的数据，防止脏读
            memory.del(key);
        }
        return redis.hmset(key, values);
    }

    public static <T> Long hset(String key, String field, T value, int seconds) {
        if (IS_MEMORY_ENABLE) {//清除内存中的数据，防止脏读
            memory.del(key);
        }
        return redis.hset(key, field, value, seconds);
    }

    public static String hmset(String key, int seconds, String... values) {
        if (IS_MEMORY_ENABLE) {//清除内存中的数据，防止脏读
            memory.del(key);
        }
        return redis.hmset(key, seconds, values);
    }

    public static String hget(String key, String field) {
        Map<String, String> map;
        if (IS_MEMORY_ENABLE) {
            map = memory.get(key);
            if (map != null && map.containsKey(field)) {
                return map.get(field);
            }
        }

        String value = redis.hget(key, field);
        if (IS_MEMORY_ENABLE && StringUtils.isNotEmpty(value)) {
            memory.set(key, redis.hgetAll(key));
        }
        return value;
    }

    public static Long hincr(String key, String field, Integer value) {
        return redis.hincr(key, field, value);
    }

    public static Long hdecr(String key, String field, Integer value) {
        return redis.hdecr(key, field, value);
    }

    public static Map<String, String> hgetAll(String key) {
        Map<String, String> map;
        if (IS_MEMORY_ENABLE) {
            map = memory.get(key);
            if (map != null && !map.isEmpty()) {
                return map;
            }
        }
        map = redis.hgetAll(key);
        if (IS_MEMORY_ENABLE && map != null && !map.isEmpty()) {
            memory.set(key, map);
        }
        return map;
    }

    public static Long pfadd(String key, String value) {
        return redis.pfadd(key, value);
    }

    public static Long pfcount(String key) {
        return redis.pfcount(key);
    }

    public static boolean setbit(String key, long offset, boolean value) {
        return redis.setbit(key, offset, value);
    }

    public static boolean setbit(String key, long offset, String value) {
        return redis.setbit(key, offset, value);
    }

    public static boolean getbit(String key, long offset) {
        return redis.getbit(key, offset);
    }

    public static Long bitcount(String key) {
        return redis.bitcount(key);
    }

    public static Long bitcount(String key, long start, long end) {
        return redis.bitcount(key, start, end);
    }

    public static Long bitop(BitOP op, String destKey, String... srcKeys) {
        return redis.bitop(op, destKey, srcKeys);
    }

    public static List<Long> bitfield(String key, String... arguments) {
        return redis.bitfield(key, arguments);
    }

    public static Long bitpos(String key, boolean value) {
        return redis.bitpos(key, value);
    }

    public static Long bitpos(String key, boolean value, long start, long end) {
        return redis.bitpos(key, value, start, end);
    }

    public static <T> boolean bloomadd(String key, T value) {
        boolean bloomadd = redis.bloomadd(key, value);
        if (IS_MEMORY_ENABLE && bloomadd) {
            String valueStr;
            if (value instanceof String) {
                valueStr = (String) value;
            } else {
                valueStr = GsonUtil.to(value);
            }
            memory.set(key + valueStr, true);
        }
        return bloomadd;
    }

    public static <T> boolean bloomcons(String key, T value) {
        Boolean bloomcons;
        if (IS_MEMORY_ENABLE) {
            String valueStr;
            if (value instanceof String) {
                valueStr = (String) value;
            } else {
                valueStr = GsonUtil.to(value);
            }
            bloomcons = memory.get(key + valueStr);
            if (BooleanUtils.isTrue(bloomcons)) {
                return bloomcons;
            }
            bloomcons = redis.bloomcons(key, value);
            if (bloomcons) {
                memory.set(key + valueStr, bloomcons);
            }
            return bloomcons;
        } else {
            return redis.bloomcons(key, value);
        }
    }

    public static Long pfadd(String key, String value, int seconds) {
        return redis.pfadd(key, value, seconds);
    }

    public static boolean getDistributedLock(String lockKey, String requestId, int expireTime) {
        return redis.getDistributedLock(lockKey, requestId, expireTime);
    }

    public static boolean releaseDistributedLock(String lockKey, String requestId) {
        return redis.releaseDistributedLock(lockKey, requestId);
    }
}
