package com.dxy.library.cache.redis.executor;

import com.dxy.library.cache.redis.exception.RedisCacheException;
import com.dxy.library.cache.redis.inter.*;
import com.dxy.library.cache.redis.properties.RedisProperties;
import com.dxy.library.cache.redis.util.BitHashUtil;
import com.dxy.library.cache.redis.util.Serializer;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.dxy.library.cache.redis.constant.CacheType;
import com.dxy.library.cache.redis.inter.*;
import com.dxy.library.cache.redis.pool.BasePool;
import com.dxy.library.cache.redis.pool.cluster.RedisClusterPool;
import com.dxy.library.cache.redis.pool.sentinel.RedisSentinelPool;
import com.dxy.library.cache.redis.pool.sharded.RedisShardedPool;
import com.dxy.library.cache.redis.pool.single.RedisSinglePool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import redis.clients.jedis.*;
import redis.clients.jedis.params.geo.GeoRadiusParam;
import redis.clients.jedis.params.sortedset.ZAddParams;
import redis.clients.jedis.params.sortedset.ZIncrByParams;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author duanxinyuan
 * 2019/4/15 17:31
 */
@Slf4j
public class RedisCacheExecutor implements ICommands {

    private volatile BasePool basePool;
    private RedisProperties redisProperties;

    public RedisCacheExecutor(RedisProperties redisProperties) {
        this.redisProperties = redisProperties;
        if (StringUtils.isNotEmpty(redisProperties.getType())) {
            switch (redisProperties.getType()) {
                case CacheType.single:
                    basePool = new RedisSinglePool(redisProperties);
                    break;
                case CacheType.sentinel:
                    basePool = new RedisSentinelPool(redisProperties);
                    break;
                case CacheType.sharded:
                    basePool = new RedisShardedPool(redisProperties);
                    break;
                case CacheType.cluster:
                    basePool = new RedisClusterPool(redisProperties);
                    break;
                default:
                    break;
            }
        } else {
            log.error("redis cache init failed, redis type not configured");
        }
    }

    @Override
    public void execute(CommandsConsumer consumer) {
        JedisCommands jedisCommands = basePool.getJedis();
        try {
            switch (redisProperties.getType()) {
                case CacheType.single:
                    consumer.single((Jedis) jedisCommands);
                    break;
                case CacheType.sentinel:
                    consumer.sentinel((Jedis) jedisCommands);
                    break;
                case CacheType.sharded:
                    consumer.sharded((ShardedJedis) jedisCommands);
                    break;
                case CacheType.cluster:
                    consumer.cluster((JedisCluster) jedisCommands);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            throw new RedisCacheException(e);
        } finally {
            basePool.close(jedisCommands);
        }
    }

    @Override
    public <T> T execute(CommandsSupplier<T> supplier) {
        JedisCommands jedisCommands = basePool.getJedis();
        try {
            switch (redisProperties.getType()) {
                case CacheType.single:
                    return supplier.single((Jedis) jedisCommands);
                case CacheType.sentinel:
                    return supplier.sentinel((Jedis) jedisCommands);
                case CacheType.sharded:
                    return supplier.sharded((ShardedJedis) jedisCommands);
                case CacheType.cluster:
                    return supplier.cluster((JedisCluster) jedisCommands);
                default:
                    return null;
            }
        } catch (Exception e) {
            throw new RedisCacheException(e);
        } finally {
            basePool.close(jedisCommands);
        }
    }

    @Override
    public <T> T execute(RedisFunction<JedisCommands, T> function) {
        JedisCommands jedis = basePool.getJedis();
        try {
            return function.apply(jedis);
        } catch (Exception e) {
            throw new RedisCacheException(e);
        } finally {
            basePool.close(jedis);
        }
    }

    @Override
    public void single(RedisConsumer<Jedis> consumer) {
        execute(new CommandsConsumer() {
            @Override
            public void single(Jedis jedis) throws Exception {
                consumer.accept(jedis);
            }
        });
    }

    @Override
    public <T> T single(RedisFunction<Jedis, T> function) {
        return execute(new CommandsSupplier<T>() {
            @Override
            public T single(Jedis jedis) throws Exception {
                return function.apply(jedis);
            }
        });
    }

    @Override
    public void sentinel(RedisConsumer<Jedis> consumer) {
        execute(new CommandsConsumer() {
            @Override
            public void sentinel(Jedis jedis) throws Exception {
                consumer.accept(jedis);
            }
        });
    }

    @Override
    public <T> T sentinel(RedisFunction<Jedis, T> function) {
        return execute(new CommandsSupplier<T>() {
            @Override
            public T sentinel(Jedis jedis) throws Exception {
                return function.apply(jedis);
            }
        });
    }

    @Override
    public void sharded(RedisConsumer<ShardedJedis> consumer) {
        execute(new CommandsConsumer() {
            @Override
            public void sharded(ShardedJedis shardedJedis) throws Exception {
                consumer.accept(shardedJedis);
            }
        });
    }

    @Override
    public <T> T sharded(RedisFunction<ShardedJedis, T> function) {
        return execute(new CommandsSupplier<T>() {
            @Override
            public T sharded(ShardedJedis shardedJedis) throws Exception {
                return function.apply(shardedJedis);
            }
        });
    }

    @Override
    public void cluster(RedisConsumer<JedisCluster> consumer) {
        execute(new CommandsConsumer() {
            @Override
            public void cluster(JedisCluster jedisCluster) throws Exception {
                consumer.accept(jedisCluster);
            }
        });
    }

    @Override
    public <T> T cluster(RedisFunction<JedisCluster, T> function) {
        return execute(new CommandsSupplier<T>() {
            @Override
            public T cluster(JedisCluster jedisCluster) throws Exception {
                return function.apply(jedisCluster);
            }
        });
    }

    @Override
    public Set<String> keys(String pattern) {
        checkNotNull(pattern);
        return execute(new CommandsSupplier<Set<String>>() {
            @Override
            public Set<String> single(Jedis jedis) {
                return jedis.keys(pattern);
            }

            @Override
            public Set<String> sentinel(Jedis jedis) {
                return jedis.keys(pattern);
            }

            @Override
            public Set<String> sharded(ShardedJedis shardedJedis) {
                throw new UnsupportedOperationException("the command 'keys' is currently not supported for ShardedJedisConnection");
            }

            @Override
            public Set<String> cluster(JedisCluster jedisCluster) {
                return jedisCluster.keys(pattern);
            }
        });
    }

    @Override
    public ScanResult<String> scan(String cursor, ScanParams params) {
        checkNotNull(cursor, params);
        return execute(new CommandsSupplier<ScanResult<String>>() {
            @Override
            public ScanResult<String> single(Jedis jedis) {
                return jedis.scan(cursor, params);
            }

            @Override
            public ScanResult<String> sentinel(Jedis jedis) {
                return jedis.scan(cursor, params);
            }

            @Override
            public ScanResult<String> sharded(ShardedJedis shardedJedis) {
                throw new UnsupportedOperationException("the command 'scan' is currently not supported for ShardedJedisConnection");
            }

            @Override
            public ScanResult<String> cluster(JedisCluster jedisCluster) {
                return jedisCluster.scan(cursor, params);
            }
        });
    }

    @Override
    public String type(String key) {
        checkNotNull(key);
        return execute(j -> j.type(key));
    }

    @Override
    public Long expire(String key, int seconds) {
        checkNotNull(key);
        return execute(j -> j.expire(key, seconds));
    }

    @Override
    public Long expireAt(String key, long unixTime) {
        checkNotNull(key);
        return execute(j -> j.expireAt(key, unixTime));
    }

    @Override
    public Long persist(String key) {
        checkNotNull(key);
        return execute(j -> j.persist(key));
    }

    @Override
    public boolean exists(String key) {
        checkNotNull(key);
        return execute(j -> BooleanUtils.toBoolean(j.exists(key)));
    }

    @Override
    public Long del(String key) {
        checkNotNull(key);
        return execute(j -> j.del(key));
    }

    @Override
    public Map<String, Long> del(String... keys) {
        checkNotNull(keys);
        return del(Lists.newArrayList(keys));
    }

    @Override
    public Map<String, Long> del(List<String> keys) {
        checkNotNull(keys);
        return execute(new CommandsSupplier<Map<String, Long>>() {

            private Map<String, Response<Long>> del(PipelineBase pipeline, List<String> keys) {
                Map<String, Response<Long>> stringResponseMap = new HashMap<>(keys.size());
                keys.forEach(key -> stringResponseMap.put(key, pipeline.del(key)));
                return stringResponseMap;
            }

            private Map<String, Long> transform(Map<String, Response<Long>> map) {
                Map<String, Long> result = new HashMap<>(map.size());
                map.forEach((key, response) -> result.put(key, response.get()));
                return result;
            }

            @Override
            public Map<String, Long> single(Jedis jedis) {
                Pipeline pipeline = jedis.pipelined();
                Map<String, Response<Long>> del = del(pipeline, keys);
                pipeline.sync();
                return transform(del);
            }

            @Override
            public Map<String, Long> sentinel(Jedis jedis) {
                return single(jedis);
            }

            @Override
            public Map<String, Long> sharded(ShardedJedis shardedJedis) {
                ShardedJedisPipeline pipeline = shardedJedis.pipelined();
                Map<String, Response<Long>> del = del(pipeline, keys);
                pipeline.sync();
                return transform(del);
            }

            @Override
            public Map<String, Long> cluster(JedisCluster jedisCluster) {
                Map<String, Long> result = new HashMap<>(keys.size());
                keys.forEach(key -> result.put(key, jedisCluster.del(key)));
                return result;
            }
        });
    }

    @Override
    public Long unlink(String key) {
        checkNotNull(key);
        return execute(j -> j.unlink(key));
    }

    @Override
    public String rename(String oldkey, String newkey) {
        return execute(new CommandsSupplier<String>() {
            @Override
            public String single(Jedis jedis) {
                return jedis.rename(oldkey, newkey);
            }

            @Override
            public String sentinel(Jedis jedis) {
                return jedis.rename(oldkey, newkey);
            }

            @Override
            public String sharded(ShardedJedis shardedJedis) {
                return shardedJedis.getShard(oldkey).rename(oldkey, newkey);
            }

            @Override
            public String cluster(JedisCluster jedisCluster) {
                return jedisCluster.rename(oldkey, newkey);
            }
        });
    }

    @Override
    public Long renamenx(String oldkey, String newkey) {
        return execute(new CommandsSupplier<Long>() {
            @Override
            public Long single(Jedis jedis) {
                return jedis.renamenx(oldkey, newkey);
            }

            @Override
            public Long sentinel(Jedis jedis) {
                return jedis.renamenx(oldkey, newkey);
            }

            @Override
            public Long sharded(ShardedJedis shardedJedis) {
                return shardedJedis.getShard(oldkey).renamenx(oldkey, newkey);
            }

            @Override
            public Long cluster(JedisCluster jedisCluster) {
                return jedisCluster.renamenx(oldkey, newkey);
            }
        });
    }

    @Override
    public <T> String set(String key, T value) {
        checkNotNull(key, value);
        return execute(j -> j.set(key, Serializer.serialize(value)));
    }

    @Override
    public <T> String set(String key, T value, String nxxx, String expx, long time) {
        checkNotNull(key, value, nxxx, expx);
        return execute(j -> j.set(key, Serializer.serialize(value), nxxx, expx, time));
    }

    @Override
    public <T> String set(String key, T value, String expx, long time) {
        checkNotNull(key, value, expx);
        return execute(j -> j.set(key, Serializer.serialize(value), expx, time));
    }

    @Override
    public <T> String set(String key, T value, String nxxx) {
        checkNotNull(key, value, nxxx);
        return execute(j -> j.set(key, Serializer.serialize(value), nxxx));
    }

    @Override
    public <T> Long setnx(String key, T value) {
        checkNotNull(key, value);
        return execute(j -> j.setnx(key, Serializer.serialize(value)));
    }

    @Override
    public <T> String setex(String key, int seconds, T value) {
        checkNotNull(key, value);
        return execute(j -> j.setex(key, seconds, Serializer.serialize(value)));
    }

    @Override
    public <T> String setex(String key, long time, TimeUnit timeUnit, T value) {
        checkNotNull(key, value);
        return execute(j -> j.psetex(key, timeUnit.toMillis(time), Serializer.serialize(value)));
    }

    @Override
    public <T> String psetex(String key, long milliseconds, T value) {
        checkNotNull(key, value);
        return execute(j -> j.psetex(key, milliseconds, Serializer.serialize(value)));
    }

    private Map<String, String> transformSetResult(Map<String, Response<String>> map) {
        Map<String, String> result = new HashMap<>(map.size());
        map.forEach((key, response) -> result.put(key, response.get()));
        return result;
    }

    @Override
    public <T> Map<String, String> mset(Map<String, T> map) {
        checkNotNull(map);
        return execute(new CommandsSupplier<Map<String, String>>() {

            private Map<String, Response<String>> mset(PipelineBase pipeline, Map<String, T> map) {
                Map<String, Response<String>> stringResponseMap = new HashMap<>(map.size());
                map.forEach((key, value) -> stringResponseMap.put(key, pipeline.set(key, Serializer.serialize(value))));
                return stringResponseMap;
            }

            @Override
            public Map<String, String> single(Jedis jedis) {
                Pipeline pipeline = jedis.pipelined();
                Map<String, Response<String>> mset = mset(pipeline, map);
                pipeline.sync();
                return transformSetResult(mset);
            }

            @Override
            public Map<String, String> sentinel(Jedis jedis) {
                return single(jedis);
            }

            @Override
            public Map<String, String> sharded(ShardedJedis shardedJedis) {
                ShardedJedisPipeline pipeline = shardedJedis.pipelined();
                Map<String, Response<String>> mset = mset(pipeline, map);
                pipeline.sync();
                return transformSetResult(mset);
            }

            @Override
            public Map<String, String> cluster(JedisCluster jedisCluster) {
                Map<String, String> result = new HashMap<>(map.size());
                map.forEach((key, value) -> result.put(key, jedisCluster.set(key, Serializer.serialize(value))));
                return result;
            }

        });
    }

    @Override
    public <T> Map<String, String> msetex(int seconds, HashMap<String, T> map) {
        checkNotNull(map);
        return msetex(seconds, TimeUnit.SECONDS, map);
    }

    @Override
    public <T> Map<String, String> msetex(long time, TimeUnit timeUnit, HashMap<String, T> map) {
        checkNotNull(timeUnit, map);
        return execute(new CommandsSupplier<Map<String, String>>() {

            private Map<String, Response<String>> msetex(long milliseconds, PipelineBase pipeline, Map<String, T> map) {
                Map<String, Response<String>> stringResponseMap = new HashMap<>(map.size());
                map.forEach((key, value) -> stringResponseMap.put(key, pipeline.psetex(key, milliseconds, Serializer.serialize(value))));
                return stringResponseMap;
            }

            @Override
            public Map<String, String> single(Jedis jedis) {
                Pipeline pipeline = jedis.pipelined();
                Map<String, Response<String>> mset = msetex(timeUnit.toMillis(time), pipeline, map);
                pipeline.sync();
                return transformSetResult(mset);
            }

            @Override
            public Map<String, String> sentinel(Jedis jedis) {
                return single(jedis);
            }

            @Override
            public Map<String, String> sharded(ShardedJedis shardedJedis) {
                ShardedJedisPipeline pipeline = shardedJedis.pipelined();
                Map<String, Response<String>> mset = msetex(timeUnit.toMillis(time), pipeline, map);
                pipeline.sync();
                return transformSetResult(mset);
            }

            @Override
            public Map<String, String> cluster(JedisCluster jedisCluster) {
                Map<String, String> result = new HashMap<>(map.size());
                long millis = timeUnit.toMillis(time);
                map.forEach((key, value) -> result.put(key, jedisCluster.psetex(key, millis, Serializer.serialize(value))));
                return result;
            }
        });
    }

    @Override
    public String get(String key) {
        checkNotNull(key);
        return execute(j -> j.get(key));
    }

    @Override
    public <T> T get(String key, Class<T> type) {
        checkNotNull(key);
        return execute(j -> Serializer.deserialize(j.get(key), type));
    }

    @Override
    public Map<String, String> mget(String... keys) {
        checkNotNull(keys);
        return mget(Lists.newArrayList(keys), String.class);
    }

    @Override
    public Map<String, String> mget(List<String> keys) {
        checkNotNull(keys);
        return mget(keys, String.class);
    }

    @Override
    public <T> Map<String, T> mget(List<String> keys, Class<T> type) {
        checkNotNull(keys);
        return execute(new CommandsSupplier<Map<String, T>>() {

            private Map<String, Response<String>> mget(PipelineBase pipeline, List<String> keys) {
                Map<String, Response<String>> map = new HashMap<>(keys.size());
                for (String key : keys) {
                    map.put(key, pipeline.get(key));
                }
                return map;
            }

            private Map<String, T> transform(Map<String, Response<String>> map) {
                Map<String, T> result = new HashMap<>(map.size());
                map.forEach((k, v) -> result.put(k, Serializer.deserialize(v.get(), type)));
                return result;
            }

            @Override
            public Map<String, T> single(Jedis jedis) {
                Pipeline pipeline = jedis.pipelined();
                Map<String, Response<String>> stringResponseMap = mget(pipeline, keys);
                pipeline.sync();
                return transform(stringResponseMap);
            }

            @Override
            public Map<String, T> sentinel(Jedis jedis) {
                return single(jedis);
            }

            @Override
            public Map<String, T> sharded(ShardedJedis shardedJedis) {
                ShardedJedisPipeline pipeline = shardedJedis.pipelined();
                Map<String, Response<String>> stringResponseMap = mget(pipeline, keys);
                pipeline.sync();
                return transform(stringResponseMap);
            }

            @Override
            public Map<String, T> cluster(JedisCluster jedisCluster) {
                Map<String, T> result = new HashMap<>(keys.size());
                for (String key : keys) {
                    result.put(key, Serializer.deserialize(jedisCluster.get(key), type));
                }
                return result;
            }
        });
    }

    @Override
    public Long incr(String key) {
        checkNotNull(key);
        return execute(j -> j.incr(key));
    }

    @Override
    public Long incrBy(String key, long increment) {
        checkNotNull(key);
        return execute(j -> j.incrBy(key, increment));
    }

    @Override
    public Double incrByFloat(String key, double increment) {
        checkNotNull(key);
        return execute(j -> j.incrByFloat(key, increment));
    }

    @Override
    public Long decr(String key) {
        checkNotNull(key);
        return execute(j -> j.decr(key));
    }

    @Override
    public Long decrBy(String key, long decrement) {
        checkNotNull(key);
        return execute(j -> j.decrBy(key, decrement));
    }

    @Override
    public Long append(String key, String value) {
        checkNotNull(key, value);
        return execute(j -> j.append(key, value));
    }

    @Override
    public Long strlen(String key) {
        checkNotNull(key);
        return execute(j -> j.strlen(key));
    }

    @Override
    public <P, T> Long hset(String key, P field, T value) {
        checkNotNull(key, field, value);
        return execute(j -> j.hset(key, Serializer.serialize(field), Serializer.serialize(value)));
    }

    @Override
    public <P, T> String hmset(String key, Map<P, T> hash) {
        checkNotNull(key, hash);
        Map<String, String> valueMap = Maps.newHashMap();
        hash.forEach((field, value) -> valueMap.put(Serializer.serialize(field), Serializer.serialize(value)));
        return execute(j -> j.hmset(key, valueMap));
    }

    @Override
    public <P, T> Long hsetnx(String key, P field, T value) {
        checkNotNull(key, field);
        return execute(j -> j.hsetnx(key, Serializer.serialize(field), Serializer.serialize(value)));
    }

    @Override
    public <P> String hget(String key, P field) {
        checkNotNull(key, field);
        return execute(j -> j.hget(key, Serializer.serialize(field)));
    }

    @Override
    public <P, T> T hget(String key, P field, Class<T> type) {
        checkNotNull(key, field);
        return execute(j -> Serializer.deserialize(j.hget(key, Serializer.serialize(field)), type));
    }

    @Override
    public <P> List<String> hmget(String key, P... fields) {
        checkNotNull(key, fields);
        return execute(j -> j.hmget(key, Serializer.serialize(fields)));
    }

    @Override
    public <P> List<String> hmget(String key, List<P> fields) {
        checkNotNull(key, fields);
        return execute(j -> j.hmget(key, Serializer.serialize(fields).toArray(new String[0])));
    }

    @Override
    public <P, T> List<T> hmget(String key, List<P> fields, Class<T> type) {
        checkNotNull(key, fields);
        return execute(j -> {
            List<String> hmget = j.hmget(key, Serializer.serialize(fields).toArray(new String[0]));
            List<T> ts = new ArrayList<>(hmget.size());
            hmget.forEach(s -> ts.add(Serializer.deserialize(s, type)));
            return ts;
        });
    }

    @Override
    public <P> Long hincrBy(String key, P field, long value) {
        checkNotNull(key, field);
        return execute(j -> j.hincrBy(key, Serializer.serialize(field), value));
    }

    @Override
    public <P> Double hincrByFloat(String key, P field, double value) {
        checkNotNull(key, field);
        return execute(j -> j.hincrByFloat(key, Serializer.serialize(field), value));
    }

    @Override
    public Set<String> hkeys(String key) {
        checkNotNull(key);
        return execute(j -> j.hkeys(key));
    }

    @Override
    public List<String> hvals(String key) {
        checkNotNull(key);
        return execute(j -> j.hvals(key));
    }

    @Override
    public <T> List<T> hvals(String key, Class<T> type) {
        checkNotNull(key);
        List<String> strings = execute(j -> j.hvals(key));
        return Serializer.deserialize(strings, type);
    }

    @Override
    public Map<String, String> hgetAll(String key) {
        checkNotNull(key);
        return execute(j -> j.hgetAll(key));
    }

    @Override
    public <T> Map<String, T> hgetAll(String key, Class<T> type) {
        checkNotNull(key);
        Map<String, String> stringMap = execute(j -> j.hgetAll(key));
        Map<String, T> result = new LinkedHashMap<>(stringMap.size());
        stringMap.forEach((k, v) -> result.put(k, Serializer.deserialize(v, type)));
        return result;
    }

    @Override
    public ScanResult<Map.Entry<String, String>> hscan(String key, String cursor) {
        checkNotNull(key, cursor);
        return execute(j -> j.hscan(key, cursor));
    }

    @Override
    public ScanResult<Map.Entry<String, String>> hscan(String key, String cursor, ScanParams params) {
        checkNotNull(key, cursor, params);
        return execute(j -> j.hscan(key, cursor, params));
    }

    @Override
    public <P> boolean hexists(String key, P field) {
        checkNotNull(key, field);
        return execute(j -> j.hexists(key, Serializer.serialize(field)));
    }

    @Override
    public <P> Long hdel(String key, P... fields) {
        checkNotNull(key, fields);
        return execute(j -> j.hdel(key, Serializer.serialize(fields)));
    }

    @Override
    public <P> Long hdel(String key, List<P> fields) {
        checkNotNull(key, fields);
        return execute(j -> j.hdel(key, fields.toArray(new String[0])));
    }

    @Override
    public <P> Long hstrlen(String key, P field) {
        checkNotNull(key, field);
        return execute(j -> j.hstrlen(key, Serializer.serialize(field)));
    }

    @Override
    public <T> Long lpush(String key, T... values) {
        checkNotNull(key, values);
        return execute(j -> j.lpush(key, Serializer.serialize(values)));
    }

    @Override
    public <T> Long lpush(String key, List<T> values) {
        checkNotNull(key, values);
        List<String> strings = Serializer.serialize(values);
        return execute(j -> j.lpush(key, strings.toArray(new String[0])));
    }

    @Override
    public <T> Long rpush(String key, T... values) {
        checkNotNull(key, values);
        return execute(j -> j.rpush(key, Serializer.serialize(values)));
    }

    @Override
    public <T> Long rpush(String key, List<T> values) {
        checkNotNull(key, values);
        List<String> strings = Serializer.serialize(values);
        return execute(j -> j.rpush(key, strings.toArray(new String[0])));
    }

    @Override
    public <T> Long lpushx(String key, T... values) {
        checkNotNull(key, values);
        return execute(j -> j.lpushx(key, Serializer.serialize(values)));
    }

    @Override
    public <T> Long lpushx(String key, List<T> values) {
        checkNotNull(key, values);
        List<String> strings = Serializer.serialize(values);
        return execute(j -> j.lpushx(key, strings.toArray(new String[0])));
    }

    @Override
    public <T> Long rpushx(String key, T... values) {
        checkNotNull(key, values);
        return execute(j -> j.rpushx(key, Serializer.serialize(values)));
    }

    @Override
    public <T> Long rpushx(String key, List<T> values) {
        checkNotNull(key, values);
        List<String> strings = Serializer.serialize(values);
        return execute(j -> j.rpushx(key, strings.toArray(new String[0])));
    }

    @Override
    public <T> String lset(String key, long index, T value) {
        checkNotNull(key, value);
        return execute(j -> j.lset(key, index, Serializer.serialize(value)));
    }

    @Override
    public <T> Long linsert(String key, ListPosition where, String pivot, T value) {
        checkNotNull(key, value);
        return execute(j -> j.linsert(key, where, pivot, Serializer.serialize(value)));
    }

    @Override
    public List<String> lrange(String key, long start, long end) {
        checkNotNull(key);
        return execute(j -> j.lrange(key, start, end));
    }

    @Override
    public <T> List<T> lrange(String key, long start, long end, Class<T> type) {
        checkNotNull(key);
        List<String> strings = execute(j -> j.lrange(key, start, end));
        return Serializer.deserialize(strings, type);
    }

    @Override
    public List<String> lrangePage(String key, int pageNo, int pageSize) {
        checkNotNull(key);
        return execute(j -> j.lrange(key, pageNo * pageSize, (pageNo + 1) * pageSize));
    }

    @Override
    public <T> List<T> lrangePage(String key, int pageNo, int pageSize, Class<T> type) {
        checkNotNull(key);
        List<String> strings = execute(j -> j.lrange(key, pageNo * pageSize, (pageNo + 1) * pageSize));
        return Serializer.deserialize(strings, type);
    }

    @Override
    public List<String> lrangeAll(String key) {
        checkNotNull(key);
        return execute(j -> j.lrange(key, 0, -1));
    }

    @Override
    public <T> List<T> lrangeAll(String key, Class<T> type) {
        checkNotNull(key);
        List<String> strings = execute(j -> j.lrange(key, 0, -1));
        return Serializer.deserialize(strings, type);
    }

    @Override
    public String lindex(String key, int index) {
        checkNotNull(key);
        return execute(j -> j.lindex(key, index));
    }

    @Override
    public <T> T lindex(String key, int index, Class<T> type) {
        checkNotNull(key);
        String s = execute(j -> j.lindex(key, index));
        return Serializer.deserialize(s, type);
    }

    @Override
    public Long llen(String key) {
        checkNotNull(key);
        return execute(j -> j.llen(key));
    }

    @Override
    public String lpop(String key) {
        checkNotNull(key);
        return execute(j -> j.lpop(key));
    }

    @Override
    public <T> T lpop(String key, Class<T> type) {
        checkNotNull(key);
        String s = execute(j -> j.lpop(key));
        return Serializer.deserialize(s, type);
    }

    @Override
    public String rpop(String key) {
        checkNotNull(key);
        return execute(j -> j.rpop(key));
    }

    @Override
    public <T> T rpop(String key, Class<T> type) {
        checkNotNull(key);
        String s = execute(j -> j.rpop(key));
        return Serializer.deserialize(s, type);
    }

    @Override
    public List<String> blpop(int timeout, String key) {
        checkNotNull(key);
        return execute(j -> j.blpop(timeout, key));
    }

    @Override
    public List<String> brpop(int timeout, String key) {
        checkNotNull(key);
        return execute(j -> j.brpop(timeout, key));
    }

    @Override
    public <T> Long lrem(String key, long count, T value) {
        checkNotNull(key);
        return execute(j -> j.lrem(key, count, Serializer.serialize(value)));
    }

    @Override
    public String ltrim(String key, long start, long end) {
        checkNotNull(key);
        return execute(j -> j.ltrim(key, start, end));
    }

    @Override
    public <T> Long sadd(String key, T... values) {
        checkNotNull(key, values);
        return execute(j -> j.sadd(key, Serializer.serialize(values)));
    }

    @Override
    public <T> Long sadd(String key, List<T> values) {
        checkNotNull(key, values);
        List<String> strings = Serializer.serialize(values);
        return execute(j -> j.sadd(key, strings.toArray(new String[0])));
    }

    @Override
    public Set<String> smembers(String key) {
        checkNotNull(key);
        return execute(j -> j.smembers(key));
    }

    @Override
    public <T> Set<T> smembers(String key, Class<T> type) {
        checkNotNull(key);
        Set<String> strings = execute(j -> j.smembers(key));
        return Serializer.deserialize(strings, type);
    }

    @Override
    public ScanResult<String> sscan(String key, String cursor) {
        checkNotNull(key, cursor);
        return execute(j -> j.sscan(key, cursor));
    }

    @Override
    public <T> ScanResult<T> sscan(String key, String cursor, Class<T> type) {
        checkNotNull(key, cursor);
        ScanResult<String> strings = execute(j -> j.sscan(key, cursor));
        return new ScanResult<>(cursor, Serializer.deserialize(strings.getResult(), type));
    }

    @Override
    public ScanResult<String> sscan(String key, String cursor, ScanParams params) {
        checkNotNull(key, cursor, params);
        return execute(j -> j.sscan(key, cursor, params));
    }

    @Override
    public <T> ScanResult<T> sscan(String key, String cursor, ScanParams params, Class<T> type) {
        checkNotNull(key, cursor, params);
        ScanResult<String> strings = execute(j -> j.sscan(key, cursor, params));
        return new ScanResult<>(cursor, Serializer.deserialize(strings.getResult(), type));
    }

    @Override
    public <T> Long srem(String key, T... values) {
        checkNotNull(key, values);
        return execute(j -> j.srem(key, Serializer.serialize(values)));
    }

    @Override
    public <T> Long srem(String key, List<T> values) {
        checkNotNull(key, values);
        List<String> strings = Serializer.serialize(values);
        return execute(j -> j.srem(key, strings.toArray(new String[0])));
    }

    @Override
    public String spop(String key) {
        checkNotNull(key);
        return execute(j -> j.spop(key));
    }

    @Override
    public <T> T spop(String key, Class<T> type) {
        checkNotNull(key);
        String s = execute(j -> j.spop(key));
        return Serializer.deserialize(s, type);
    }

    @Override
    public Long scard(String key) {
        checkNotNull(key);
        return execute(j -> j.scard(key));
    }

    @Override
    public <T> boolean sismember(String key, T value) {
        checkNotNull(key, value);
        return execute(j -> j.sismember(key, Serializer.serialize(value)));
    }

    @Override
    public List<String> srandmember(String key, int count) {
        checkNotNull(key);
        return execute(j -> j.srandmember(key, count));
    }

    @Override
    public <T> List<T> srandmember(String key, int count, Class<T> type) {
        checkNotNull(key);
        return execute(j -> Serializer.deserialize(j.srandmember(key, count), type));
    }

    @Override
    public <T> Long zadd(String key, double score, T member) {
        checkNotNull(key, member);
        return execute(j -> j.zadd(key, score, Serializer.serialize(member)));
    }

    @Override
    public <T> Long zadd(String key, double score, T member, ZAddParams params) {
        checkNotNull(key, member, params);
        return execute(j -> j.zadd(key, score, Serializer.serialize(member), params));
    }

    @Override
    public Long zadd(String key, Map<String, Double> scoreMembers) {
        checkNotNull(key, scoreMembers);
        return execute(j -> j.zadd(key, scoreMembers));
    }

    @Override
    public Long zadd(String key, Map<String, Double> scoreMembers, ZAddParams params) {
        checkNotNull(key, scoreMembers, params);
        return execute(j -> j.zadd(key, scoreMembers, params));
    }

    @Override
    public Set<String> zrange(String key, long start, long stop) {
        checkNotNull(key);
        return execute(j -> j.zrange(key, start, stop));
    }

    @Override
    public <T> Set<T> zrange(String key, long start, long stop, Class<T> type) {
        checkNotNull(key);
        Set<String> strings = execute(j -> j.zrange(key, start, stop));
        return Serializer.deserialize(strings, type);
    }

    @Override
    public Set<String> zrevrange(String key, long start, long stop) {
        checkNotNull(key);
        return execute(j -> j.zrevrange(key, start, stop));
    }

    @Override
    public <T> Set<T> zrevrange(String key, long start, long stop, Class<T> type) {
        checkNotNull(key);
        Set<String> strings = execute(j -> j.zrevrange(key, start, stop));
        return Serializer.deserialize(strings, type);
    }

    @Override
    public <T> Long zrem(String key, T... members) {
        checkNotNull(key, members);
        return execute(j -> j.zrem(key, Serializer.serialize(members)));
    }

    @Override
    public <T> Long zrem(String key, List<T> members) {
        checkNotNull(key, members);
        return execute(j -> j.zrem(key, Serializer.serialize(members).toArray(new String[0])));
    }

    @Override
    public <T> Double zincrby(String key, double increment, T member) {
        checkNotNull(key, member);
        return execute(j -> j.zincrby(key, increment, Serializer.serialize(member)));
    }

    @Override
    public <T> Double zincrby(String key, double increment, T member, ZIncrByParams params) {
        checkNotNull(key, member, params);
        return execute(j -> j.zincrby(key, increment, Serializer.serialize(member), params));
    }

    @Override
    public Set<String> zrangeByScore(String key, double min, double max) {
        checkNotNull(key, max, min);
        return execute(j -> j.zrangeByScore(key, min, max));
    }

    @Override
    public <T> Set<T> zrangeByScore(String key, double min, double max, Class<T> type) {
        checkNotNull(key, max, min);
        Set<String> strings = execute(j -> j.zrangeByScore(key, min, max));
        return Serializer.deserialize(strings, type);
    }

    @Override
    public Set<String> zrangeByScore(String key, String min, String max) {
        checkNotNull(key, max, min);
        return execute(j -> j.zrangeByScore(key, min, max));
    }

    @Override
    public <T> Set<T> zrangeByScore(String key, String min, String max, Class<T> type) {
        checkNotNull(key, max, min);
        Set<String> strings = execute(j -> j.zrangeByScore(key, min, max));
        return Serializer.deserialize(strings, type);
    }

    @Override
    public Set<String> zrevrangeByScore(String key, double max, double min) {
        checkNotNull(key, max, min);
        return execute(j -> j.zrevrangeByScore(key, min, max));
    }

    @Override
    public <T> Set<T> zrevrangeByScore(String key, double max, double min, Class<T> type) {
        checkNotNull(key, max, min);
        Set<String> strings = execute(j -> j.zrevrangeByScore(key, min, max));
        return Serializer.deserialize(strings, type);
    }

    @Override
    public Set<String> zrevrangeByScore(String key, String max, String min) {
        checkNotNull(key, max, min);
        return execute(j -> j.zrevrangeByScore(key, min, max));
    }

    @Override
    public <T> Set<T> zrevrangeByScore(String key, String max, String min, Class<T> type) {
        checkNotNull(key, max, min);
        Set<String> strings = execute(j -> j.zrevrangeByScore(key, min, max));
        return Serializer.deserialize(strings, type);
    }

    @Override
    public Set<String> zrangeByLex(String key, String min, String max) {
        checkNotNull(key, max, min);
        return execute(j -> j.zrangeByLex(key, min, max));
    }

    @Override
    public <T> Set<T> zrangeByLex(String key, String min, String max, Class<T> type) {
        checkNotNull(key, max, min);
        Set<String> strings = execute(j -> j.zrangeByLex(key, min, max));
        return Serializer.deserialize(strings, type);
    }

    @Override
    public Set<String> zrevrangeByLex(String key, String max, String min) {
        checkNotNull(key, max, min);
        return execute(j -> j.zrevrangeByLex(key, min, max));
    }

    @Override
    public <T> Set<T> zrevrangeByLex(String key, String max, String min, Class<T> type) {
        checkNotNull(key, max, min);
        Set<String> strings = execute(j -> j.zrevrangeByLex(key, min, max));
        return Serializer.deserialize(strings, type);
    }

    @Override
    public ScanResult<Tuple> zscan(String key, String cursor) {
        checkNotNull(key, cursor);
        return execute(j -> j.zscan(key, cursor));
    }

    @Override
    public ScanResult<Tuple> zscan(String key, String cursor, ScanParams params) {
        checkNotNull(key, cursor);
        return execute(j -> j.zscan(key, cursor, params));
    }

    @Override
    public Long zremrangeByRank(String key, long start, long stop) {
        checkNotNull(key);
        return execute(j -> j.zremrangeByRank(key, start, stop));
    }

    @Override
    public Long zremrangeByScore(String key, double min, double max) {
        checkNotNull(key);
        return execute(j -> j.zremrangeByScore(key, min, max));
    }

    @Override
    public Long zremrangeByScore(String key, String min, String max) {
        checkNotNull(key);
        return execute(j -> j.zremrangeByScore(key, min, max));
    }

    @Override
    public Long zremrangeByLex(String key, String min, String max) {
        checkNotNull(key);
        return execute(j -> j.zremrangeByLex(key, min, max));
    }

    @Override
    public <T> Long zrank(String key, T member) {
        checkNotNull(key, member);
        return execute(j -> j.zrank(key, Serializer.serialize(member)));
    }

    @Override
    public <T> Long zrevrank(String key, T member) {
        checkNotNull(key, member);
        return execute(j -> j.zrevrank(key, Serializer.serialize(member)));
    }

    @Override
    public Long zcard(String key) {
        checkNotNull(key);
        return execute(j -> j.zcard(key));
    }

    @Override
    public <T> Double zscore(String key, T member) {
        checkNotNull(key, member);
        return execute(j -> j.zscore(key, Serializer.serialize(member)));
    }

    @Override
    public Long zcount(String key, double min, double max) {
        checkNotNull(key);
        return execute(j -> j.zcount(key, min, max));
    }

    @Override
    public Long zcount(String key, String min, String max) {
        checkNotNull(key);
        return execute(j -> j.zcount(key, min, max));
    }

    @Override
    public Long zlexcount(String key, String min, String max) {
        checkNotNull(key);
        return execute(j -> j.zlexcount(key, min, max));
    }

    @Override
    public <T> Long pfadd(String key, T... elements) {
        checkNotNull(key, elements);
        return execute(j -> j.pfadd(key, Serializer.serialize(elements)));
    }

    @Override
    public <T> Long pfadd(String key, List<T> elements) {
        checkNotNull(key, elements);
        return execute(j -> j.pfadd(key, Serializer.serialize(elements).toArray(new String[0])));
    }

    @Override
    public Long pfcount(String key) {
        checkNotNull(key);
        return execute(j -> j.pfcount(key));
    }

    @Override
    public boolean setbit(String key, long offset, boolean value) {
        checkNotNull(key);
        return execute(j -> j.setbit(key, offset, value));
    }

    @Override
    public boolean setbit(String key, long offset, String value) {
        checkNotNull(key);
        return execute(j -> j.setbit(key, offset, value));
    }

    @Override
    public boolean getbit(String key, long offset) {
        checkNotNull(key);
        return execute(j -> j.getbit(key, offset));
    }

    @Override
    public Long bitcount(String key) {
        checkNotNull(key);
        return execute(j -> j.bitcount(key));
    }

    @Override
    public Long bitcount(String key, long start, long end) {
        checkNotNull(key);
        return execute(j -> j.bitcount(key, start, end));
    }

    @Override
    public Long bitpos(String key, boolean value) {
        checkNotNull(key);
        return execute(j -> j.bitpos(key, value));
    }

    @Override
    public Long bitpos(String key, boolean value, long start, long end) {
        checkNotNull(key);
        BitPosParams bitPosParams = new BitPosParams(start, end);
        return execute(j -> j.bitpos(key, value, bitPosParams));
    }

    @Override
    public Long bitop(BitOP op, String destKey, String... srcKeys) {
        checkNotNull(op, destKey, srcKeys);
        return execute(new CommandsSupplier<Long>() {
            @Override
            public Long single(Jedis jedis) {
                return jedis.bitop(op, destKey, srcKeys);
            }

            @Override
            public Long sentinel(Jedis jedis) {
                return jedis.bitop(op, destKey, srcKeys);
            }

            @Override
            public Long sharded(ShardedJedis shardedJedis) {
                return shardedJedis.getShard(destKey).bitop(op, destKey, srcKeys);
            }

            @Override
            public Long cluster(JedisCluster jedisCluster) {
                return jedisCluster.bitop(op, destKey, srcKeys);
            }
        });
    }

    @Override
    public Long bitop(BitOP op, String destKey, List<String> srcKeys) {
        checkNotNull(op, destKey, srcKeys);
        return bitop(op, destKey, srcKeys.toArray(new String[0]));
    }

    @Override
    public List<Long> bitfield(String key, String... arguments) {
        checkNotNull(key, arguments);
        return execute(j -> j.bitfield(key, arguments));
    }

    @Override
    public List<Long> bitfield(String key, List<String> arguments) {
        checkNotNull(key, arguments);
        return execute(j -> j.bitfield(key, arguments.toArray(new String[0])));
    }

    @Override
    public <T> Long geoadd(String key, double longitude, double latitude, T member) {
        checkNotNull(key, member);
        return execute(j -> j.geoadd(key, longitude, latitude, Serializer.serialize(member)));
    }

    @Override
    public Long geoadd(String key, Map<String, GeoCoordinate> memberCoordinateMap) {
        checkNotNull(key, memberCoordinateMap);
        return execute(j -> j.geoadd(key, memberCoordinateMap));
    }

    @Override
    public <T> Double geodist(String key, T member1, T member2) {
        checkNotNull(key, member1, member2);
        return execute(j -> j.geodist(key, Serializer.serialize(member1), Serializer.serialize(member2)));
    }

    @Override
    public <T> Double geodist(String key, T member1, T member2, GeoUnit unit) {
        checkNotNull(key, member1, member2);
        return execute(j -> j.geodist(key, Serializer.serialize(member1), Serializer.serialize(member2), unit));
    }

    @Override
    public <T> List<String> geohash(String key, T... members) {
        checkNotNull(key, members);
        return execute(j -> j.geohash(key, Serializer.serialize(members)));
    }

    @Override
    public <T> List<String> geohash(String key, List<T> members) {
        checkNotNull(key, members);
        return execute(j -> j.geohash(key, Serializer.serialize(members).toArray(new String[0])));
    }

    @Override
    public <T> List<GeoCoordinate> geopos(String key, T... members) {
        checkNotNull(key, members);
        return execute(j -> j.geopos(key, Serializer.serialize(members)));
    }

    @Override
    public <T> List<GeoCoordinate> geopos(String key, List<T> members) {
        checkNotNull(key, members);
        return execute(j -> j.geopos(key, Serializer.serialize(members).toArray(new String[0])));
    }

    @Override
    public List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit) {
        checkNotNull(key);
        return execute(j -> j.georadius(key, longitude, latitude, radius, unit));
    }

    @Override
    public List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
        checkNotNull(key);
        return execute(j -> j.georadius(key, longitude, latitude, radius, unit, param));
    }

    @Override
    public <T> List<GeoRadiusResponse> georadiusByMember(String key, T member, double radius, GeoUnit unit) {
        checkNotNull(key);
        return execute(j -> j.georadiusByMember(key, Serializer.serialize(member), radius, unit));
    }

    @Override
    public <T> List<GeoRadiusResponse> georadiusByMember(String key, T member, double radius, GeoUnit unit, GeoRadiusParam param) {
        checkNotNull(key);
        return execute(j -> j.georadiusByMember(key, Serializer.serialize(member), radius, unit, param));
    }

    @Override
    public <T> boolean bloomadd(String key, T value) {
        checkNotNull(key);
        boolean bloomconstains = bloomcons(key, value);
        if (bloomconstains) {
            return false;
        }
        long[] offsets = BitHashUtil.getBitOffsets(value);
        for (long offset : offsets) {
            execute(j -> j.setbit(key, offset, true));
        }
        return true;
    }

    @Override
    public <T> boolean bloomcons(String key, T value) {
        checkNotNull(key);
        long[] offsets = BitHashUtil.getBitOffsets(value);
        for (long offset : offsets) {
            Boolean execute = execute(j -> j.getbit(key, offset));
            if (BooleanUtils.isFalse(execute)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean getDistributedLock(String lockKey, String requestId, int expireTime) {
        checkNotNull(lockKey, requestId);
        String result = execute(j -> j.set(lockKey, requestId, "NX", "PX", expireTime));
        return "OK".equals(result);
    }

    @Override
    @SuppressWarnings("Duplicates")
    public boolean releaseDistributedLock(String lockKey, String requestId) {
        checkNotNull(lockKey, requestId);
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Object result = execute(new CommandsSupplier<Object>() {

            @Override
            public Object single(Jedis jedis) {
                return jedis.eval(script, Collections.singletonList(lockKey), Collections.singletonList(requestId));
            }

            @Override
            public Object sentinel(Jedis jedis) {
                return jedis.eval(script, Collections.singletonList(lockKey), Collections.singletonList(requestId));
            }

            @Override
            public Object sharded(ShardedJedis shardedJedis) {
                return shardedJedis.getShard(lockKey).eval(script, Collections.singletonList(lockKey), Collections.singletonList(requestId));
            }

            @Override
            public Object cluster(JedisCluster jedisCluster) {
                return jedisCluster.eval(script, Collections.singletonList(lockKey), Collections.singletonList(requestId));
            }
        });
        Long RELEASE_SUCCESS = 1L;
        return RELEASE_SUCCESS.equals(result);
    }

    @Override
    @SuppressWarnings("Duplicates")
    public <P, T> T eval(String script, int keyCount, List<P> params, Class<T> type) {
        if (keyCount != 0) {
            checkNotNull(params);
        }
        String[] strings = Serializer.serialize(params).toArray(new String[0]);
        Object execute = execute(new CommandsSupplier<Object>() {
            @Override
            public Object single(Jedis jedis) {
                return jedis.eval(script, keyCount, strings);
            }

            @Override
            public Object sentinel(Jedis jedis) {
                return jedis.eval(script, keyCount, strings);
            }

            @Override
            public Object sharded(ShardedJedis shardedJedis) {
                throw new UnsupportedOperationException("the command 'eval' is currently not supported for ShardedJedisConnection");
            }

            @Override
            public Object cluster(JedisCluster jedisCluster) {
                return jedisCluster.eval(script, keyCount, strings);
            }
        });
        return Serializer.deserialize(execute, type);
    }

    @Override
    @SuppressWarnings("Duplicates")
    public <P, T> T eval(String script, int keyCount, Class<T> type, P... params) {
        if (keyCount != 0) {
            checkNotNull(params);
        }
        Object execute = execute(new CommandsSupplier<Object>() {
            @Override
            public Object single(Jedis jedis) {
                return jedis.eval(script, keyCount, Serializer.serialize(params));
            }

            @Override
            public Object sentinel(Jedis jedis) {
                return jedis.eval(script, keyCount, Serializer.serialize(params));
            }

            @Override
            public Object sharded(ShardedJedis shardedJedis) {
                throw new UnsupportedOperationException("the command 'eval' is currently not supported for ShardedJedisConnection");
            }

            @Override
            public Object cluster(JedisCluster jedisCluster) {
                return jedisCluster.eval(script, keyCount, Serializer.serialize(params));
            }
        });
        return Serializer.deserialize(execute, type);
    }

    @Override
    @SuppressWarnings("Duplicates")
    public <P, T, R> R eval(String script, List<P> keys, List<T> args, Class<R> type) {
        checkNotNull(keys, args);
        Object execute = execute(new CommandsSupplier<Object>() {
            @Override
            public Object single(Jedis jedis) {
                return jedis.eval(script, Serializer.serialize(keys), Serializer.serialize(args));
            }

            @Override
            public Object sentinel(Jedis jedis) {
                return jedis.eval(script, Serializer.serialize(keys), Serializer.serialize(args));
            }

            @Override
            public Object sharded(ShardedJedis shardedJedis) {
                throw new UnsupportedOperationException("the command 'eval' is currently not supported for ShardedJedisConnection");
            }

            @Override
            public Object cluster(JedisCluster jedisCluster) {
                return jedisCluster.eval(script, Serializer.serialize(keys), Serializer.serialize(args));
            }
        });
        return Serializer.deserialize(execute, type);
    }

    private void checkNotNull(Object object, Object... objects) {
        Objects.requireNonNull(object);
        for (Object o : objects) {
            Objects.requireNonNull(o);
        }
    }

}
