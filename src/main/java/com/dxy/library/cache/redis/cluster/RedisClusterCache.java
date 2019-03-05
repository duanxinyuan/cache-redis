package com.dxy.library.cache.redis.cluster;

import com.google.common.collect.Lists;
import com.google.gson.reflect.TypeToken;
import com.dxy.library.cache.redis.IRedis;
import com.dxy.library.cache.redis.util.BitHashUtil;
import com.dxy.library.json.gson.GsonUtil;
import com.dxy.library.util.common.ListUtils;
import com.dxy.library.util.common.config.ConfigUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import redis.clients.jedis.*;

import java.util.*;

/**
 * Redis集群模式缓存器
 * @author duanxinyuan
 * 2018/8/8 17:52
 */
@Slf4j
public class RedisClusterCache implements IRedis {

    private static final String LOCK_SUCCESS = "OK";
    private static final String SET_IF_NOT_EXIST = "NX";
    private static final String SET_WITH_EXPIRE_TIME = "PX";
    private static final Long RELEASE_SUCCESS = 1L;

    private JedisCluster jedisCluster;

    public RedisClusterCache() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(NumberUtils.toInt(ConfigUtils.getConfig("cache.redis.connection.max.total"), 100));
        config.setMaxIdle(NumberUtils.toInt(ConfigUtils.getConfig("cache.redis.connection.max.idle"), 50));
        config.setMaxWaitMillis(NumberUtils.toInt(ConfigUtils.getConfig("cache.redis.max.wait.millis"), 5000));
        config.setTestOnBorrow(true);
        String hostsStr = ConfigUtils.getConfig("cache.redis.nodes");
        if (StringUtils.isEmpty(hostsStr)) {
            log.error("redis cluster init failed, nodes not configured");
            return;
        }
        String[] hostPorts = hostsStr.split(",");
        HashSet<HostAndPort> hostSet = new HashSet<>();
        for (String hostPort : hostPorts) {
            String[] strings = hostPort.split(":");
            String host = strings[0];
            int port = strings.length > 1 ? NumberUtils.toInt(strings[1].trim(), 6379) : 6379;
            hostSet.add(new HostAndPort(host, port));
        }
        String password = ConfigUtils.getConfig("cache.redis.password");
        if (StringUtils.isEmpty(password)) {
            jedisCluster = new JedisCluster(hostSet, config);
        } else {
            jedisCluster = new JedisCluster(hostSet, 2000, 2000, 5, password, config);
        }
    }

    @Override
    public <T> String set(String key, T value) {
        return set(key, value, 0);
    }

    @Override
    public <T> String set(String key, T value, int seconds) {
        if (StringUtils.isEmpty(key) || value == null || seconds < 0) {
            return null;
        }
        String result;
        if (value instanceof String) {
            result = jedisCluster.set(key, (String) value);
        } else {
            result = jedisCluster.set(key, GsonUtil.to(value));
        }
        if (seconds > 0) {
            jedisCluster.expire(key, seconds);
        }
        return result;
    }

    @Override
    public <T> Long setnx(String key, T value) {
        return setnx(key, value, 0);
    }

    @Override
    public <T> Long setnx(String key, T value, int seconds) {
        if (StringUtils.isEmpty(key) || value == null || seconds < 0) {
            return null;
        }
        Long setnx;
        if (value instanceof String) {
            setnx = jedisCluster.setnx(key, (String) value);
        } else {
            setnx = jedisCluster.setnx(key, GsonUtil.to(value));
        }
        if (seconds > 0) {
            jedisCluster.expire(key, seconds);
        }
        return setnx;
    }

    @Override
    public String get(String key) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        return jedisCluster.get(key);
    }

    @Override
    public <T> T get(String key, Class<T> c) {
        if (StringUtils.isEmpty(key) || c == null) {
            return null;
        }
        String value = get(key);
        if (c == String.class) {
            return (T) value;
        } else {
            return GsonUtil.from(value, c);
        }
    }

    @Override
    public <T> T get(String key, TypeToken<T> typeToken) {
        if (StringUtils.isEmpty(key) || typeToken == null) {
            return null;
        }
        return GsonUtil.from(get(key), typeToken);
    }

    @Override
    public Long incr(String key, Integer value, int seconds) {
        if (StringUtils.isEmpty(key) || value == null || value == 0 || seconds < 0) {
            return null;
        }
        long total = jedisCluster.incrBy(key, value);
        if (seconds > 0) {
            jedisCluster.expire(key, seconds);
        }
        return total;
    }

    @Override
    public Long incr(String key, Integer value) {
        return incr(key, value, 0);
    }

    @Override
    public Long decr(String key, Integer value) {
        return jedisCluster.decrBy(key, value);
    }

    @Override
    public Long decr(String key, Integer value, int seconds) {
        if (StringUtils.isEmpty(key) || value == null || value == 0 || seconds < 0) {
            return null;
        }
        long total = jedisCluster.decrBy(key, value);
        if (seconds > 0) {
            jedisCluster.expire(key, seconds);
        }
        return total;
    }

    @Override
    public Long expire(String key, int seconds) {
        return jedisCluster.expire(key, seconds);
    }

    @Override
    public Long persist(String key) {
        return jedisCluster.persist(key);
    }

    @Override
    public boolean exist(String key) {
        return jedisCluster.exists(key);
    }

    @Override
    public Long del(String key) {
        return jedisCluster.del(key);
    }

    @Override
    public void del(String... keys) {
        jedisCluster.del(keys);
    }

    @Override
    public <T> Long lpush(String key, T value) {
        return lpush(key, value, 0);
    }

    @Override
    public <T> Long lpush(String key, T value, int seconds) {
        if (StringUtils.isEmpty(key) || value == null || seconds < 0) {
            return null;
        }
        Long lpush;
        if (value instanceof String) {
            lpush = jedisCluster.lpush(key, (String) value);
        } else {
            lpush = jedisCluster.lpush(key, GsonUtil.to(value));
        }
        if (seconds > 0) {
            jedisCluster.expire(key, seconds);
        }
        return lpush;
    }

    @Override
    public <T> Long lpush(String key, List<T> values) {
        return lpush(key, values, 0);
    }

    @Override
    public <T> Long lpush(String key, List<T> values, int seconds) {
        if (StringUtils.isEmpty(key) || ListUtils.isEmpty(values) || seconds < 0) {
            return null;
        }
        String[] strings = new String[values.size()];
        for (int i = 0; i < values.size(); i++) {
            T value = values.get(i);
            if (value instanceof String) {
                strings[i] = (String) value;
            } else {
                strings[i] = GsonUtil.to(value);
            }
        }
        Long lpush = jedisCluster.lpush(key, strings);
        if (seconds > 0) {
            jedisCluster.expire(key, seconds);
        }
        return lpush;
    }

    @Override
    public <T> Long rpush(String key, T value) {
        return rpush(key, value, 0);
    }

    @Override
    public <T> Long rpush(String key, T value, int seconds) {
        if (StringUtils.isEmpty(key) || value == null || seconds < 0) {
            return null;
        }
        Long lpush;
        if (value instanceof String) {
            lpush = jedisCluster.rpush(key, (String) value);
        } else {
            lpush = jedisCluster.rpush(key, GsonUtil.to(value));
        }
        if (seconds > 0) {
            jedisCluster.expire(key, seconds);
        }
        return lpush;
    }

    @Override
    public <T> Long rpush(String key, List<T> values) {
        return rpush(key, values, 0);
    }

    @Override
    public <T> Long rpush(String key, List<T> values, int seconds) {
        if (StringUtils.isEmpty(key) || ListUtils.isEmpty(values) || seconds < 0) {
            return null;
        }
        ArrayList<String> strings = Lists.newArrayList();
        for (int i = 0; i < values.size(); i++) {
            T value = values.get(i);
            if (value instanceof String) {
                strings.add((String) value);
            } else {
                strings.add(GsonUtil.to(value));
            }
        }
        Long lpush = jedisCluster.rpush(key, strings.toArray(new String[0]));
        if (seconds > 0) {
            jedisCluster.expire(key, seconds);
        }
        return lpush;
    }

    @Override
    public List<String> lrange(String key) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        return jedisCluster.lrange(key, 0, llen(key));
    }

    @Override
    public <T> List<T> lrange(String key, Class<T> c) {
        return lrange(key, 0, llen(key), c);
    }

    @Override
    public List<String> lrange(String key, long end) {
        if (StringUtils.isEmpty(key) || end < 0) {
            return null;
        }
        return jedisCluster.lrange(key, 0, end);
    }

    @Override
    public <T> List<T> lrange(String key, long end, Class<T> c) {
        return lrange(key, 0, end, c);
    }

    @Override
    public List<String> lrange(String key, long start, long end) {
        if (StringUtils.isEmpty(key) || start < 0 || end < 0) {
            return null;
        }
        return jedisCluster.lrange(key, start, end);
    }

    @Override
    public <T> List<T> lrange(String key, long start, long end, Class<T> c) {
        if (StringUtils.isEmpty(key) || start < 0 || end < 0 || c == null) {
            return null;
        }
        List<String> strings = jedisCluster.lrange(key, start, end);
        if (c == String.class) {
            return (List<T>) strings;
        }
        List<T> ts = Lists.newArrayList();
        strings.forEach(s -> ts.add(GsonUtil.from(s, c)));
        return ts;
    }

    @Override
    public List<String> lrangePage(String key, int pageNo, int pageSize) {
        return lrange(key, pageNo * pageSize, (pageNo + 1) * pageSize);
    }

    @Override
    public <T> List<T> lrangePage(String key, int pageNo, int pageSize, Class<T> c) {
        return lrange(key, pageNo * pageSize, (pageNo + 1) * pageSize, c);
    }

    @Override
    public String lindex(String key, int index) {
        if (StringUtils.isEmpty(key) || index < 0) {
            return null;
        }
        return jedisCluster.lindex(key, index);
    }

    @Override
    public <T> T lindex(String key, int index, Class<T> c) {
        if (StringUtils.isEmpty(key) || index < 0) {
            return null;
        }
        String s = jedisCluster.lindex(key, index);
        if (c == String.class) {
            return (T) s;
        } else {
            return GsonUtil.from(s, c);
        }
    }

    @Override
    public Long llen(String key) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        return jedisCluster.llen(key);
    }

    @Override
    public void lclear(String key) {
        if (StringUtils.isEmpty(key)) {
            return;
        }
        Long llen = llen(key);
        if (llen != null && llen > 0) {
            for (long i = 0; i < llen; i++) {
                lpop(key);
            }
        }
    }

    @Override
    public Long lrem(String key, String value) {
        if (StringUtils.isEmpty(key) || value == null) {
            return null;
        }
        return jedisCluster.lrem(key, 0, value);
    }

    @Override
    public <T> Long lrem(String key, T value) {
        if (StringUtils.isEmpty(key) || value == null) {
            return null;
        }
        return jedisCluster.lrem(key, 0, GsonUtil.to(value));
    }

    @Override
    public Long lrem(String key, long count, String value) {
        if (StringUtils.isEmpty(key) || value == null) {
            return null;
        }
        return jedisCluster.lrem(key, count, value);
    }

    @Override
    public <T> Long lrem(String key, long count, T value) {
        if (StringUtils.isEmpty(key) || value == null) {
            return null;
        }
        return jedisCluster.lrem(key, count, GsonUtil.to(value));
    }

    @Override
    public String ltrim(String key, long start, long end) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        return jedisCluster.ltrim(key, start, end);
    }

    @Override
    public String lpop(String key) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        return jedisCluster.lpop(key);
    }

    @Override
    public String rpop(String key) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        return jedisCluster.rpop(key);
    }

    @Override
    public Long sadd(String key, String... values) {
        if (StringUtils.isEmpty(key) || values == null || values.length == 0) {
            return null;
        }
        return sadd(key, 0, values);
    }

    @Override
    public Long sadd(String key, int seconds, String... values) {
        if (StringUtils.isEmpty(key) || values == null || values.length == 0 || seconds < 0) {
            return null;
        }
        Long sadd = jedisCluster.sadd(key, values);
        if (seconds > 0) {
            jedisCluster.expire(key, seconds);
        }
        return sadd;
    }

    @Override
    public boolean sismember(String key, String value) {
        if (StringUtils.isEmpty(key) || value == null) {
            return false;
        }
        return jedisCluster.sismember(key, value);
    }

    @Override
    public Set<String> smembers(String key) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        return jedisCluster.smembers(key);
    }

    @Override
    public <T> Long hset(String key, String field, T value) {
        if (StringUtils.isEmpty(key) || field == null || value == null) {
            return null;
        }
        return hset(key, field, value, 0);
    }

    @Override
    public String hmset(String key, String... values) {
        if (StringUtils.isEmpty(key) || values == null || values.length == 0) {
            return null;
        }
        return hmset(key, 0, values);
    }

    @Override
    public <T> Long hset(String key, String field, T value, int seconds) {
        if (StringUtils.isEmpty(key) || field == null || value == null || seconds < 0) {
            return null;
        }
        Long hset;
        if (value instanceof String) {
            hset = jedisCluster.hset(key, field, (String) value);
        } else {
            hset = jedisCluster.hset(key, field, GsonUtil.to(value));
        }
        if (seconds > 0) {
            jedisCluster.expire(key, seconds);
        }
        return hset;
    }

    @Override
    public String hmset(String key, int seconds, String... values) {
        if (StringUtils.isEmpty(key) || values == null || values.length == 0 || seconds < 0) {
            return null;
        }
        String hmset;
        int len = values.length;
        Map<String, String> map = new HashMap<>(len / 2);
        for (int i = 0; i < len; ) {
            map.put(values[i], values[i + 1]);
            i += 2;
        }
        hmset = jedisCluster.hmset(key, map);

        if (seconds > 0) {
            jedisCluster.expire(key, seconds);
        }
        return hmset;
    }

    @Override
    public String hget(String key, String field) {
        if (StringUtils.isEmpty(key) || field == null) {
            return null;
        }
        return jedisCluster.hget(key, field);
    }

    @Override
    public <T> T hget(String key, String field, Class<T> c) {
        String hget = hget(key, field);
        return GsonUtil.from(hget, c);
    }

    @Override
    public <T> T hget(String key, String field, TypeToken<T> typeToken) {
        String hget = hget(key, field);
        return GsonUtil.from(hget, typeToken);
    }

    @Override
    public Long hincr(String key, String field, Integer value) {
        if (StringUtils.isEmpty(key) || field == null || value == null || value == 0) {
            return null;
        }
        return jedisCluster.hincrBy(key, field, value);
    }

    @Override
    public Long hdecr(String key, String field, Integer value) {
        if (StringUtils.isEmpty(key) || field == null || value == null || value == 0) {
            return null;
        }
        return jedisCluster.hincrBy(key, field, -value);
    }

    @Override
    public Map<String, String> hgetAll(String key) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        return jedisCluster.hgetAll(key);
    }

    @Override
    public Long pfadd(String key, String value) {
        if (StringUtils.isEmpty(key) || value == null) {
            return null;
        }
        return jedisCluster.pfadd(key, value);
    }

    @Override
    public Long pfadd(String key, String value, int seconds) {
        if (StringUtils.isEmpty(key) || value == null) {
            return null;
        }
        Long pfadd = jedisCluster.pfadd(key, value);
        if (seconds > 0) {
            jedisCluster.expire(key, seconds);
        }
        return pfadd;
    }

    @Override
    public Long pfcount(String key) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        return jedisCluster.pfcount(key);
    }

    @Override
    public boolean setbit(String key, long offset, boolean value) {
        if (StringUtils.isEmpty(key)) {
            return false;
        }
        return BooleanUtils.toBoolean(jedisCluster.setbit(key, offset, value));
    }

    @Override
    public boolean setbit(String key, long offset, String value) {
        if (StringUtils.isEmpty(key)) {
            return false;
        }
        return BooleanUtils.toBoolean(jedisCluster.setbit(key, offset, value));
    }

    @Override
    public boolean getbit(String key, long offset) {
        if (StringUtils.isEmpty(key)) {
            return false;
        }
        return BooleanUtils.toBoolean(jedisCluster.getbit(key, offset));
    }

    @Override
    public Long bitcount(String key) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        return jedisCluster.bitcount(key);
    }

    @Override
    public Long bitcount(String key, long start, long end) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        return jedisCluster.bitcount(key, start, end);
    }

    @Override
    public Long bitop(BitOP op, String destKey, String... srcKeys) {
        if (op == null || StringUtils.isEmpty(destKey) || srcKeys == null || srcKeys.length == 0) {
            return null;
        }
        return jedisCluster.bitop(op, destKey, srcKeys);
    }

    @Override
    public List<Long> bitfield(String key, String... arguments) {
        if (StringUtils.isEmpty(key) || arguments == null || arguments.length == 0) {
            return null;
        }
        return jedisCluster.bitfield(key, arguments);
    }

    @Override
    public Long bitpos(String key, boolean value) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        return jedisCluster.bitpos(key, value);
    }

    @Override
    public Long bitpos(String key, boolean value, long start, long end) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        return jedisCluster.bitpos(key, value, new BitPosParams(start, end));
    }

    @Override
    public <T> boolean bloomadd(String key, T value) {
        if (StringUtils.isEmpty(key) || value == null) {
            return false;
        }
        boolean bloomconstains = bloomcons(key, value);
        if (bloomconstains) {
            return false;
        }
        long[] offsets = BitHashUtil.getBitOffsets(value);
        for (long offset : offsets) {
            jedisCluster.setbit(key, offset, true);
        }
        return true;
    }

    @Override
    public <T> boolean bloomcons(String key, T value) {
        if (StringUtils.isEmpty(key) || value == null) {
            return false;
        }
        long[] offsets = BitHashUtil.getBitOffsets(value);
        for (long offset : offsets) {
            if (!jedisCluster.getbit(key, offset)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean getDistributedLock(String lockKey, String requestId, int expireTime) {
        if (StringUtils.isEmpty(lockKey) || requestId == null) {
            return false;
        }
        String result = jedisCluster.set(lockKey, requestId, SET_IF_NOT_EXIST, SET_WITH_EXPIRE_TIME, expireTime);
        return LOCK_SUCCESS.equals(result);
    }

    @Override
    public boolean releaseDistributedLock(String lockKey, String requestId) {
        if (StringUtils.isEmpty(lockKey) || requestId == null) {
            return false;
        }
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Object result = jedisCluster.eval(script, Collections.singletonList(lockKey), Collections.singletonList(requestId));
        return RELEASE_SUCCESS.equals(result);
    }
}
