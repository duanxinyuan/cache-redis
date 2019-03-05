package com.dxy.library.cache.redis.sentinel;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.reflect.TypeToken;
import com.dxy.library.cache.exception.RedisCacheException;
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
 * Redis单机模式缓存器
 * @author duanxinyuan
 * 2018/8/8 18:28
 */
@Slf4j
public class RedisSentinelCache implements IRedis {

    private static final String LOCK_SUCCESS = "OK";
    private static final String SET_IF_NOT_EXIST = "NX";
    private static final String SET_WITH_EXPIRE_TIME = "PX";
    private static final Long RELEASE_SUCCESS = 1L;

    private JedisSentinelPool jedisSentinelPool;

    public RedisSentinelCache() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(NumberUtils.toInt(ConfigUtils.getConfig("cache.redis.connection.max.total"), 100));
        config.setMaxIdle(NumberUtils.toInt(ConfigUtils.getConfig("cache.redis.connection.max.idle"), 50));
        config.setMaxWaitMillis(NumberUtils.toInt(ConfigUtils.getConfig("cache.redis.max.wait.millis"), 5000));
        config.setTestOnBorrow(true);


        String hostsStr = ConfigUtils.getConfig("cache.redis.nodes");
        if (StringUtils.isEmpty(hostsStr)) {
            log.error("redis sentinel init failed, nodes not configured");
            return;
        }
        String[] hostPorts = hostsStr.split(",");
        Set<String> sentinels = Sets.newHashSet(Arrays.asList(hostPorts));

        String password = ConfigUtils.getConfig("cache.redis.password");
        String masterName = "CacheMaster";
        int timeout = 2000;
        //直接使用第0个database
        int database = 0;

        jedisSentinelPool = new JedisSentinelPool(masterName, sentinels, config, timeout, password, database);
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
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            String set;
            if (value instanceof String) {
                set = jedis.set(key, (String) value);
            } else {
                set = jedis.set(key, GsonUtil.to(value));
            }
            if (seconds > 0) {
                jedis.expire(key, seconds);
            }
            return set;
        } catch (Exception e) {
            log.error("set error, key: {}, value: {}, seconds: {}", key, value, seconds, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public <T> Long setnx(String key, T value) {
        return setnx(key, value, 0);
    }

    @Override
    public <T> Long setnx(String key, T value, int seconds) {
        if (StringUtils.isEmpty(key) || value == null) {
            return null;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            Long setnx;
            if (value instanceof String) {
                setnx = jedis.setnx(key, (String) value);
            } else {
                setnx = jedis.setnx(key, GsonUtil.to(value));
            }
            if (seconds > 0) {
                jedis.expire(key, seconds);
            }
            return setnx;
        } catch (Exception e) {
            log.error("setnx error, key: {}, value: {}, seconds: {}", key, value, seconds, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public String get(String key) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            return jedis.get(key);
        } catch (Exception e) {
            log.error("get error, key: {}", key, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public <T> T get(String key, Class<T> c) {
        if (StringUtils.isEmpty(key) || c == null) {
            return null;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            String value = jedis.get(key);
            if (c == String.class) {
                return (T) value;
            } else {
                return GsonUtil.from(value, c);
            }
        } catch (Exception e) {
            log.error("get error, key: {}", key, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public <T> T get(String key, TypeToken<T> typeToken) {
        if (StringUtils.isEmpty(key) || typeToken == null) {
            return null;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            return GsonUtil.from(jedis.get(key), typeToken);
        } catch (Exception e) {
            log.error("get error, key: {}", key, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public Long incr(String key, Integer value, int seconds) {
        if (StringUtils.isEmpty(key) || value == null || seconds < 0) {
            return null;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            Long total = jedis.incrBy(key, value);
            if (total.intValue() == value) {
                if (seconds > 0) {
                    jedis.expire(key, seconds);
                }
            }
            return total;
        } catch (Exception e) {
            log.error("incr error, key: {}, value: {}, seconds: {}", key, value, seconds, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public Long incr(String key, Integer value) {
        if (StringUtils.isEmpty(key) || value == null || value == 0) {
            return null;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            return jedis.incrBy(key, value);
        } catch (Exception e) {
            log.error("incr error, key: {}, value: {}", key, value, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public Long decr(String key, Integer value) {
        if (StringUtils.isEmpty(key) || value == null || value == 0) {
            return null;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            return jedis.decrBy(key, value);
        } catch (Exception e) {
            log.error("decr error, key: {}, value: {}", key, value, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public Long decr(String key, Integer value, int seconds) {
        if (StringUtils.isEmpty(key) || value == null || value == 0 || seconds < 0) {
            return null;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            Long total = jedis.decrBy(key, value);
            if (seconds > 0) {
                jedis.expire(key, seconds);
            }
            return total;
        } catch (Exception e) {
            log.error("decr error, key: {}, value: {}, seconds: {}", key, value, seconds, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public Long expire(String key, int seconds) {
        if (StringUtils.isEmpty(key) || seconds < 0) {
            return null;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            return jedis.expire(key, seconds);
        } catch (Exception e) {
            log.error("expired error, key: {}, seconds: {}", key, seconds, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public Long persist(String key) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            return jedis.persist(key);
        } catch (Exception e) {
            log.error("persist error, key: {}", key, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public boolean exist(String key) {
        if (StringUtils.isEmpty(key)) {
            return false;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            return jedis.exists(key);
        } catch (Exception e) {
            log.error("exists error, key: {}", key, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public Long del(String key) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            return jedis.del(key);
        } catch (Exception e) {
            log.error("del error, key: {}", key, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public void del(String... keys) {
        if (keys == null || keys.length == 0) {
            return;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            jedis.del(keys);
        } catch (Exception e) {
            log.error("del error, key: {}", keys, e);
            throw new RedisCacheException(e);
        }
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
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            Long lpush;
            if (value instanceof String) {
                lpush = jedis.lpush(key, (String) value);
            } else {
                lpush = jedis.lpush(key, GsonUtil.to(value));
            }
            if (seconds > 0) {
                jedis.expire(key, seconds);
            }
            return lpush;
        } catch (Exception e) {
            log.error("lpush error, key: {}, value: {}, seconds: {}", key, value, seconds, e);
            throw new RedisCacheException(e);
        }
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
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            String[] strings = new String[values.size()];
            for (int i = 0; i < values.size(); i++) {
                T value = values.get(i);
                if (value instanceof String) {
                    strings[i] = (String) value;
                } else {
                    strings[i] = GsonUtil.to(value);
                }
            }
            Long lpush = jedis.lpush(key, strings);
            if (seconds > 0) {
                jedis.expire(key, seconds);
            }
            return lpush;
        } catch (Exception e) {
            log.error("lpush error, key: {}, value: {}, seconds: {}", key, GsonUtil.to(values), seconds, e);
            throw new RedisCacheException(e);
        }
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
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            Long lpush;
            if (value instanceof String) {
                lpush = jedis.rpush(key, (String) value);
            } else {
                lpush = jedis.rpush(key, GsonUtil.to(value));
            }
            if (seconds > 0) {
                jedis.expire(key, seconds);
            }
            return lpush;
        } catch (Exception e) {
            log.error("rpush error, key: {}, value: {}, seconds: {}", key, GsonUtil.to(value), seconds, e);
            throw new RedisCacheException(e);
        }
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
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            ArrayList<String> strings = Lists.newArrayList();
            for (int i = 0; i < values.size(); i++) {
                T value = values.get(i);
                if (value instanceof String) {
                    strings.add((String) value);
                } else {
                    strings.add(GsonUtil.to(value));
                }
            }
            Long lpush = jedis.rpush(key, strings.toArray(new String[0]));
            if (seconds > 0) {
                jedis.expire(key, seconds);
            }
            return lpush;
        } catch (Exception e) {
            log.error("rpush error, key: {}, value: {}, seconds: {}", key, GsonUtil.to(values), seconds, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public List<String> lrange(String key) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            return jedis.lrange(key, 0, llen(key));
        } catch (Exception e) {
            log.error("lrange error, key: {}", key, e);
            throw new RedisCacheException(e);
        }
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
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            return jedis.lrange(key, 0, end);
        } catch (Exception e) {
            log.error("lrange error, key: {}, end: {}", key, end, e);
            throw new RedisCacheException(e);
        }
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
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            return jedis.lrange(key, start, end);
        } catch (Exception e) {
            log.error("lrange error, key: {}, start: {}, end: {}", key, start, end, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public <T> List<T> lrange(String key, long start, long end, Class<T> c) {
        if (StringUtils.isEmpty(key) || start < 0 || end < 0 || c == null) {
            return null;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            List<String> strings = jedis.lrange(key, start, end);
            if (c == String.class) {
                return (List<T>) strings;
            }
            List<T> ts = Lists.newArrayList();
            strings.forEach(s -> ts.add(GsonUtil.from(s, c)));
            return ts;
        } catch (Exception e) {
            log.error("lrange error, key: {}, start: {}, end: {}, class: {}", key, start, end, c, e);
            throw new RedisCacheException(e);
        }
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
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            return jedis.lindex(key, index);
        } catch (Exception e) {
            log.error("lindex error, key: {}, index: {}", key, index, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public <T> T lindex(String key, int index, Class<T> c) {
        if (StringUtils.isEmpty(key) || index < 0) {
            return null;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            String s = jedis.lindex(key, index);
            if (c == String.class) {
                return (T) s;
            } else {
                return GsonUtil.from(s, c);
            }
        } catch (Exception e) {
            log.error("lindex error, key: {}, index: {}, class: {}", key, index, c, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public Long llen(String key) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            return jedis.llen(key);
        } catch (Exception e) {
            log.error("llen error, key: {}", key, e);
            throw new RedisCacheException(e);
        }
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
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            return jedis.lrem(key, 0, value);
        } catch (Exception e) {
            log.error("lrem error, key: {}, value: {}", key, value, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public <T> Long lrem(String key, T value) {
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            return jedis.lrem(key, 0, GsonUtil.to(value));
        } catch (Exception e) {
            log.error("lrem error, key: {}, value: {}", key, GsonUtil.to(value), e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public Long lrem(String key, long count, String value) {
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            return jedis.lrem(key, count, value);
        } catch (Exception e) {
            log.error("lrem error, key: {}, count: {}, value: {}", key, count, value, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public <T> Long lrem(String key, long count, T value) {
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            return jedis.lrem(key, count, GsonUtil.to(value));
        } catch (Exception e) {
            log.error("lrem error, key: {}, count: {}, value: {}", key, count, GsonUtil.to(value), e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public String ltrim(String key, long start, long end) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            return jedis.ltrim(key, start, end);
        } catch (Exception e) {
            log.error("ltrim error, key: {}, start: {}, end: {}", key, start, end, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public String lpop(String key) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            return jedis.lpop(key);
        } catch (Exception e) {
            log.error("lpop error, key: {}", key, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public String rpop(String key) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            return jedis.rpop(key);
        } catch (Exception e) {
            log.error("rpop error, key: {}", key, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public Long sadd(String key, String... value) {
        return sadd(key, 0, value);
    }

    @Override
    public Long sadd(String key, int seconds, String... values) {
        if (StringUtils.isEmpty(key) || values == null || values.length == 0) {
            return null;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            Long sadd = jedis.sadd(key, values);
            if (seconds > 0) {
                jedis.expire(key, seconds);
            }
            return sadd;
        } catch (Exception e) {
            log.error("sadd error, key: {}, value: {}", key, GsonUtil.to(values), e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public boolean sismember(String key, String value) {
        if (value == null || StringUtils.isEmpty(key)) {
            return false;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            return jedis.sismember(key, value);
        } catch (Exception e) {
            log.error("sismember error, key: {}, value: {}", key, value, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public Set<String> smembers(String key) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            return jedis.smembers(key);
        } catch (Exception e) {
            log.error("smembers error, key: {}", key, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public <T> Long hset(String key, String field, T value) {
        return hset(key, field, value, 0);
    }

    @Override
    public String hmset(String key, String... values) {
        return hmset(key, 0, values);
    }

    @Override
    public <T> Long hset(String key, String field, T value, int seconds) {
        if (StringUtils.isEmpty(key) || field == null || value == null || seconds < 0) {
            return null;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            Long hset;
            if (value instanceof String) {
                hset = jedis.hset(key, field, (String) value);
            } else {
                hset = jedis.hset(key, field, GsonUtil.to(value));
            }
            if (seconds > 0) {
                jedis.expire(key, seconds);
            }
            return hset;
        } catch (Exception e) {
            log.error("hset error, key: {}, value: {}, seconds: {}", key, value, seconds, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public String hmset(String key, int seconds, String... values) {
        if (StringUtils.isEmpty(key) || values == null || values.length == 0) {
            return null;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            int len = values.length;
            Map<String, String> map = new HashMap<>(len / 2);
            for (int i = 0; i < len; ) {
                map.put(values[i], values[i + 1]);
                i += 2;
            }
            String hmset = jedis.hmset(key, map);
            if (seconds > 0) {
                jedis.expire(key, seconds);
            }
            return hmset;
        } catch (Exception e) {
            log.error("hset error, key: {}, values: {}, seconds: {}", key, GsonUtil.to(values), seconds, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public String hget(String key, String field) {
        if (field == null || StringUtils.isEmpty(key)) {
            return null;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            return jedis.hget(key, field);
        } catch (Exception e) {
            log.error("hget error, key: {}, field: {}", key, field, e);
            throw new RedisCacheException(e);
        }
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
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            return jedis.hincrBy(key, field, value);
        } catch (Exception e) {
            log.error("hincrBy error, key: {}, field: {}, value: {}", key, field, value, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public Long hdecr(String key, String field, Integer value) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            return jedis.hincrBy(key, field, -value);
        } catch (Exception e) {
            log.error("hincrBy error, key: {}, field: {}, value: {}", key, field, value, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public Map<String, String> hgetAll(String key) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            return jedis.hgetAll(key);
        } catch (Exception e) {
            log.error("hgetAll error, key: {}", key, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public Long pfadd(String key, String value) {
        if (StringUtils.isEmpty(key) || value == null) {
            return null;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            return jedis.pfadd(key, value);
        } catch (Exception e) {
            log.error("pfadd error, key: {}, value: {}", key, value, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public Long pfadd(String key, String value, int seconds) {
        if (StringUtils.isEmpty(key) || value == null || seconds < 0) {
            return null;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            Long pfadd = jedis.pfadd(key, value);
            if (seconds > 0) {
                jedis.expire(key, seconds);
            }
            return pfadd;
        } catch (Exception e) {
            log.error("pfadd error, key: {}, value: {}, seconds: {}", key, value, seconds, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public Long pfcount(String key) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            return jedis.pfcount(key);
        } catch (Exception e) {
            log.error("pfcount error, key: {}", key, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public boolean setbit(String key, long offset, boolean value) {
        if (StringUtils.isEmpty(key)) {
            return false;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            return BooleanUtils.toBoolean(jedis.setbit(key, offset, value));
        } catch (Exception e) {
            log.error("setbit error, key: {}, offset: {}, value: {}", key, offset, value, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public boolean setbit(String key, long offset, String value) {
        if (StringUtils.isEmpty(key)) {
            return false;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            return BooleanUtils.toBoolean(jedis.setbit(key, offset, value));
        } catch (Exception e) {
            log.error("setbit error, key: {}, offset: {}, value: {}", key, offset, value, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public boolean getbit(String key, long offset) {
        if (StringUtils.isEmpty(key)) {
            return false;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            return BooleanUtils.toBoolean(jedis.getbit(key, offset));
        } catch (Exception e) {
            log.error("getbit error, key: {}, offset: {}", key, offset, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public Long bitcount(String key) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            return jedis.bitcount(key);
        } catch (Exception e) {
            log.error("getbit error, key: {}", key, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public Long bitcount(String key, long start, long end) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            return jedis.bitcount(key, start, end);
        } catch (Exception e) {
            log.error("getbit error, key: {}, start: {}, end: {}", key, start, end, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public Long bitop(BitOP op, String destKey, String... srcKeys) {
        if (op == null || StringUtils.isEmpty(destKey) || srcKeys == null || srcKeys.length == 0) {
            return null;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            return jedis.bitop(op, destKey, srcKeys);
        } catch (Exception e) {
            log.error("bitop error, operate: {}, destKey: {}, srcKeys: {}", op.toString(), destKey, GsonUtil.to(srcKeys), e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public List<Long> bitfield(String key, String... arguments) {
        if (StringUtils.isEmpty(key) || arguments == null || arguments.length == 0) {
            return null;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            return jedis.bitfield(key, arguments);
        } catch (Exception e) {
            log.error("bitfield error, key: {}, arguments: {}", key, GsonUtil.to(arguments), e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public Long bitpos(String key, boolean value) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            return jedis.bitpos(key, value);
        } catch (Exception e) {
            log.error("bitpos error, key: {}, value: {}", key, value, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public Long bitpos(String key, boolean value, long start, long end) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            return jedis.bitpos(key, value, new BitPosParams(start, end));
        } catch (Exception e) {
            log.error("bitpos error, key: {}, value: {}, start: {}, end: {}", key, value, start, end, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public <T> boolean bloomadd(String key, T value) {
        if (StringUtils.isEmpty(key) || value == null) {
            return false;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            boolean bloomconstains = bloomcons(key, value);
            if (bloomconstains) {
                return false;
            }
            long[] offsets = BitHashUtil.getBitOffsets(value);
            for (long offset : offsets) {
                jedis.setbit(key, offset, true);
            }
            return true;
        } catch (Exception e) {
            log.error("bloomadd error, key: {}, value: {}", key, value, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public <T> boolean bloomcons(String key, T value) {
        if (StringUtils.isEmpty(key) || value == null) {
            return false;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            long[] offsets = BitHashUtil.getBitOffsets(value);
            for (long offset : offsets) {
                if (!jedis.getbit(key, offset)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.error("bloomcons error, key: {}, value: {}", key, value, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public boolean getDistributedLock(String lockKey, String requestId, int expireTime) {
        if (StringUtils.isEmpty(lockKey) || requestId == null) {
            return false;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            String result = jedis.set(lockKey, requestId, SET_IF_NOT_EXIST, SET_WITH_EXPIRE_TIME, expireTime);
            return LOCK_SUCCESS.equals(result);
        } catch (Exception e) {
            log.error("getDistributedLock error, key: {}", lockKey, e);
            throw new RedisCacheException(e);
        }
    }

    @Override
    public boolean releaseDistributedLock(String lockKey, String requestId) {
        if (StringUtils.isEmpty(lockKey) || requestId == null) {
            return false;
        }
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            Object result = jedis.eval(script, Collections.singletonList(lockKey), Collections.singletonList(requestId));
            return RELEASE_SUCCESS.equals(result);
        } catch (Exception e) {
            log.error("releaseDistributedLock error, key: {}", lockKey, e);
            throw new RedisCacheException(e);
        }
    }

}
