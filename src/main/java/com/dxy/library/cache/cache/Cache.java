package com.dxy.library.cache.cache;

import com.dxy.common.util.ConfigUtil;
import com.dxy.library.cache.cache.memory.IMemory;
import com.dxy.library.cache.cache.memory.caffeine.CacheCaffeine;
import com.dxy.library.cache.cache.memory.guava.CacheGuava;
import com.dxy.library.cache.cache.redis.IRedis;
import com.dxy.library.cache.cache.redis.cluster.CacheRedisCluster;
import com.dxy.library.cache.cache.redis.shard.CacheRedisShard;
import com.dxy.library.cache.cache.redis.single.CacheRedisSingle;
import com.dxy.library.cache.constant.CacheType;
import com.dxy.library.json.GsonUtil;
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
public class Cache implements IRedis {
    private IMemory memory;
    private IRedis redis;

    private final boolean isEnableMenory;

    private volatile static Cache cache;

    public static Cache getInstance() {
        if (cache == null) {
            synchronized (Cache.class) {
                if (cache == null) {
                    cache = new Cache();
                }
            }
        }
        return cache;
    }

    public Cache() {
        isEnableMenory = BooleanUtils.toBoolean(ConfigUtil.getConfig("cache.memory.enable", Boolean.class));
        if (isEnableMenory) {
            String memoryCacheType = ConfigUtil.getConfig("cache.memory.type");
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

        String redisCacheType = ConfigUtil.getConfig("cache.redis.type");
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

    @Override
    public <T> String set(String key, T value) {
        if (isEnableMenory) {
            memory.set(key, value);
        }
        return redis.set(key, value);
    }

    @Override
    public <T> String set(String key, T value, int seconds) {
        if (isEnableMenory) {
            memory.set(key, value);
        }
        return redis.set(key, value, seconds);
    }

    @Override
    public <T> Long setnx(String key, T value) {
        return redis.setnx(key, value);
    }

    @Override
    public <T> Long setnx(String key, T value, int seconds) {
        return redis.setnx(key, value, seconds);
    }

    @Override
    public String get(String key) {
        String value;
        if (isEnableMenory) {
            value = memory.get(key);
            if (StringUtils.isNotEmpty(value)) {
                return value;
            }
        }

        value = redis.get(key);
        if (isEnableMenory && StringUtils.isNotEmpty(value)) {
            memory.set(key, value);
        }
        return value;
    }

    @Override
    public <T> T get(String key, Class<T> c) {
        String value;
        if (isEnableMenory) {
            value = memory.get(key);
            if (StringUtils.isNotEmpty(value)) {
                return GsonUtil.from(value, c);
            }
        }

        value = redis.get(key);
        if (isEnableMenory && StringUtils.isNotEmpty(value)) {
            memory.set(key, value);
        }
        return GsonUtil.from(value, c);
    }

    @Override
    public <T> T get(String key, TypeToken<T> typeToken) {
        String value = null;
        if (isEnableMenory) {
            value = memory.get(key);
        }
        if (StringUtils.isNotEmpty(value)) {
            return GsonUtil.from(value, typeToken);
        }

        value = redis.get(key);
        if (isEnableMenory && StringUtils.isNotEmpty(value)) {
            memory.set(key, value);
        }
        return GsonUtil.from(value, typeToken);
    }

    @Override
    public Long incr(String key, Integer value, int seconds) {
        if (isEnableMenory) {//清除内存中的数据，防止脏读
            memory.del(key);
        }
        return redis.incr(key, value, seconds);
    }

    @Override
    public Long incr(String key, Integer value) {
        if (isEnableMenory) {//清除内存中的数据，防止脏读
            memory.del(key);
        }
        return redis.incr(key, value);
    }

    @Override
    public Long decr(String key, Integer value) {
        if (isEnableMenory) {//清除内存中的数据，防止脏读
            memory.del(key);
        }
        return redis.decr(key, value);
    }

    @Override
    public Long decr(String key, Integer value, int seconds) {
        if (isEnableMenory) {//清除内存中的数据，防止脏读
            memory.del(key);
        }
        return redis.decr(key, value, seconds);
    }

    @Override
    public Long expire(String key, int seconds) {
        return redis.expire(key, seconds);
    }

    @Override
    public Long persist(String key) {
        return redis.persist(key);
    }

    @Override
    public boolean exist(String key) {
        //redis数据类型太多，判断是否存在之后无法回设内存
        return redis.exist(key);
    }

    @Override
    public Long del(String key) {
        if (isEnableMenory) {
            memory.del(key);
        }
        return redis.del(key);
    }

    @Override
    public void del(String... keys) {
        if (isEnableMenory) {
            memory.del(keys);
        }
        redis.del(keys);
    }

    @Override
    public <T> Long lpush(String key, T value) {
        return redis.lpush(key, value);
    }

    @Override
    public <T> Long lpush(String key, T value, int seconds) {
        return redis.lpush(key, value, seconds);
    }

    @Override
    public <T> Long lpush(String key, List<T> values) {
        return redis.lpush(key, values);
    }

    @Override
    public <T> Long lpush(String key, List<T> values, int seconds) {
        return redis.lpush(key, values, seconds);
    }

    @Override
    public <T> Long rpush(String key, T value) {
        return redis.rpush(key, value);
    }

    @Override
    public <T> Long rpush(String key, T value, int seconds) {
        return redis.rpush(key, value, seconds);
    }

    @Override
    public <T> Long rpush(String key, List<T> values) {
        return redis.rpush(key, values);
    }

    @Override
    public <T> Long rpush(String key, List<T> values, int seconds) {
        return redis.rpush(key, values, seconds);
    }

    @Override
    public List<String> lrange(String key) {
        return redis.lrange(key);
    }

    @Override
    public <T> List<T> lrange(String key, Class<T> c) {
        return redis.lrange(key, c);
    }

    @Override
    public List<String> lrange(String key, long end) {
        return redis.lrange(key, end);
    }

    @Override
    public <T> List<T> lrange(String key, long end, Class<T> c) {
        return redis.lrange(key, end, c);
    }

    @Override
    public List<String> lrange(String key, long start, long end) {
        return redis.lrange(key, start, end);
    }

    @Override
    public <T> List<T> lrange(String key, long start, long end, Class<T> c) {
        return redis.lrange(key, start, end, c);
    }

    @Override
    public List<String> lrangePage(String key, int pageNo, int pageSize) {
        return redis.lrangePage(key, pageNo, pageSize);
    }

    @Override
    public <T> List<T> lrangePage(String key, int pageNo, int pageSize, Class<T> c) {
        return redis.lrangePage(key, pageNo, pageSize, c);
    }

    @Override
    public String lindex(String key, int index) {
        return redis.lindex(key, index);
    }

    @Override
    public <T> T lindex(String key, int index, Class<T> c) {
        return redis.lindex(key, index, c);
    }

    @Override
    public Long llen(String key) {
        return redis.llen(key);
    }

    @Override
    public void lclear(String key) {
        redis.lclear(key);
    }

    @Override
    public Long lrem(String key, String value) {
        return redis.lrem(key, value);
    }

    @Override
    public <T> Long lrem(String key, T value) {
        return redis.lrem(key, value);
    }

    @Override
    public Long lrem(String key, long count, String value) {
        return redis.lrem(key, count, value);
    }

    @Override
    public <T> Long lrem(String key, long count, T value) {
        return redis.lrem(key, count, value);
    }

    @Override
    public String ltrim(String key, long start, long end) {
        return redis.ltrim(key, start, end);
    }

    @Override
    public String lpop(String key) {
        return redis.lpop(key);
    }

    @Override
    public String rpop(String key) {
        return redis.rpop(key);
    }

    @Override
    public Long sadd(String key, String... values) {
        if (isEnableMenory) {//清除内存中的数据，防止脏读
            memory.del(key);
        }
        return redis.sadd(key, values);
    }

    @Override
    public Long sadd(String key, int seconds, String... values) {
        if (isEnableMenory) {//清除内存中的数据，防止脏读
            memory.del(key);
        }
        return redis.sadd(key, seconds, values);
    }

    @Override
    public boolean sismember(String key, String value) {
        return redis.sismember(key, value);
    }

    @Override
    public Set<String> smembers(String key) {
        Set<String> set;
        if (isEnableMenory) {
            set = memory.get(key);
            if (set != null && set.size() != 0) {
                return set;
            }
        }

        set = redis.smembers(key);
        if (isEnableMenory && set != null && set.size() != 0) {
            memory.set(key, set);
        }
        return set;
    }

    @Override
    public <T> Long hset(String key, String field, T value) {
        if (isEnableMenory) {//清除内存中的数据，防止脏读
            memory.del(key);
        }
        return redis.hset(key, field, value);
    }

    @Override
    public String hmset(String key, String... values) {
        if (isEnableMenory) {//清除内存中的数据，防止脏读
            memory.del(key);
        }
        return redis.hmset(key, values);
    }

    @Override
    public <T> Long hset(String key, String field, T value, int seconds) {
        if (isEnableMenory) {//清除内存中的数据，防止脏读
            memory.del(key);
        }
        return redis.hset(key, field, value, seconds);
    }

    @Override
    public String hmset(String key, int seconds, String... values) {
        if (isEnableMenory) {//清除内存中的数据，防止脏读
            memory.del(key);
        }
        return redis.hmset(key, seconds, values);
    }

    @Override
    public String hget(String key, String field) {
        Map<String, String> map;
        if (isEnableMenory) {
            map = memory.get(key);
            if (map != null && map.containsKey(field)) {
                return map.get(field);
            }
        }

        String value = redis.hget(key, field);
        if (isEnableMenory && StringUtils.isNotEmpty(value)) {
            memory.set(key, redis.hgetAll(key));
        }
        return value;
    }

    @Override
    public Long hincr(String key, String field, Integer value) {
        return redis.hincr(key, field, value);
    }

    @Override
    public Long hdecr(String key, String field, Integer value) {
        return redis.hdecr(key, field, value);
    }

    @Override
    public Map<String, String> hgetAll(String key) {
        Map<String, String> map;
        if (isEnableMenory) {
            map = memory.get(key);
            if (map != null && !map.isEmpty()) {
                return map;
            }
        }
        map = redis.hgetAll(key);
        if (isEnableMenory && map != null && !map.isEmpty()) {
            memory.set(key, map);
        }
        return map;
    }

    @Override
    public Long pfadd(String key, String value) {
        return redis.pfadd(key, value);
    }

    @Override
    public Long pfcount(String key) {
        return redis.pfcount(key);
    }

    @Override
    public boolean setbit(String key, long offset, boolean value) {
        return redis.setbit(key, offset, value);
    }

    @Override
    public boolean setbit(String key, long offset, String value) {
        return redis.setbit(key, offset, value);
    }

    @Override
    public boolean getbit(String key, long offset) {
        return redis.getbit(key, offset);
    }

    @Override
    public Long bitcount(String key) {
        return redis.bitcount(key);
    }

    @Override
    public Long bitcount(String key, long start, long end) {
        return redis.bitcount(key, start, end);
    }

    @Override
    public Long bitop(BitOP op, String destKey, String... srcKeys) {
        return redis.bitop(op, destKey, srcKeys);
    }

    @Override
    public List<Long> bitfield(String key, String... arguments) {
        return redis.bitfield(key, arguments);
    }

    @Override
    public Long bitpos(String key, boolean value) {
        return redis.bitpos(key, value);
    }

    @Override
    public Long bitpos(String key, boolean value, long start, long end) {
        return redis.bitpos(key, value, start, end);
    }

    @Override
    public <T> boolean bloomadd(String key, T value) {
        boolean bloomadd = redis.bloomadd(key, value);
        if (isEnableMenory && bloomadd) {
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

    @Override
    public <T> boolean bloomcons(String key, T value) {
        Boolean bloomcons;
        if (isEnableMenory) {
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

    @Override
    public Long pfadd(String key, String value, int seconds) {
        return redis.pfadd(key, value, seconds);
    }

    @Override
    public boolean getDistributedLock(String lockKey, String requestId, int expireTime) {
        return redis.getDistributedLock(lockKey, requestId, expireTime);
    }

    @Override
    public boolean releaseDistributedLock(String lockKey, String requestId) {
        return redis.releaseDistributedLock(lockKey, requestId);
    }
}
