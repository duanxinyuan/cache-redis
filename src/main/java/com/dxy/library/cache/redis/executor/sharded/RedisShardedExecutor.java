package com.dxy.library.cache.redis.executor.sharded;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.dxy.library.cache.redis.exception.RedisCacheException;
import com.dxy.library.cache.redis.exception.UnsupportedCommandException;
import com.dxy.library.cache.redis.executor.AbstractExecutor;
import com.dxy.library.cache.redis.inter.RedisConsumer;
import com.dxy.library.cache.redis.inter.RedisFunction;
import com.dxy.library.cache.redis.properties.RedisProperties;
import com.dxy.library.cache.redis.util.BitHashUtil;
import com.dxy.library.cache.redis.util.Serializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import redis.clients.jedis.*;
import redis.clients.jedis.params.GeoRadiusParam;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.params.ZAddParams;
import redis.clients.jedis.params.ZIncrByParams;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Redis分片模式缓存器
 * @author duanxinyuan
 * 2018/8/8 18:45
 */
@Slf4j
public class RedisShardedExecutor extends AbstractExecutor<ShardedJedis> {

    private ShardedJedisPool shardedJedisPool;

    public RedisShardedExecutor(RedisProperties redisProperties) {
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
        shardedJedisPool = new ShardedJedisPool(config, shards);
    }

    @Override
    public void executeVoid(RedisConsumer<ShardedJedis> consumer) {
        try (ShardedJedis shardedJedis = shardedJedisPool.getResource()) {
            consumer.accept(shardedJedis);
        } catch (Exception e) {
            throw new RedisCacheException(e);
        }
    }

    @Override
    public <T> T execute(RedisFunction<ShardedJedis, T> function) {
        try (ShardedJedis shardedJedis = shardedJedisPool.getResource()) {
            return function.apply(shardedJedis);
        } catch (Exception e) {
            throw new RedisCacheException(e);
        }
    }

    @Override
    public Set<String> keys(String pattern) {
        throw new UnsupportedCommandException("keys");
    }

    @Override
    public ScanResult<String> scan(String cursor, ScanParams params) {
        checkNotNull(cursor, params);
        throw new UnsupportedCommandException("scan");
    }

    @Override
    public String type(String key) {
        checkNotNull(key);
        return execute(j -> j.type(key));
    }

    @Override
    public Long ttl(String key) {
        checkNotNull(key);
        return execute(j -> j.ttl(key));
    }

    @Override
    public Long pttl(String key) {
        checkNotNull(key);
        return execute(j -> j.pttl(key));
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
    public Long del(String... keys) {
        checkNotNull(keys);
        return del(Lists.newArrayList(keys));
    }

    @Override
    public Long del(List<String> keys) {
        checkNotNull(keys);
        return execute(j -> {
            ShardedJedisPipeline pipeline = j.pipelined();
            Map<String, Response<Long>> stringResponseMap = new HashMap<>(keys.size());
            keys.forEach(key -> stringResponseMap.put(key, pipeline.del(key)));
            pipeline.sync();
            List<Long> result = transformResponse(stringResponseMap);
            return result.stream().mapToLong(l -> NumberUtils.toLong(String.valueOf(l))).sum();
        });
    }

    @Override
    public Long unlink(String key) {
        checkNotNull(key);
        return execute(j -> j.unlink(key));
    }

    @Override
    public String rename(String oldkey, String newkey) {
        throw new UnsupportedCommandException("rename");
    }

    @Override
    public Long renamenx(String oldkey, String newkey) {
        throw new UnsupportedCommandException("renamenx");
    }

    @Override
    public <T> String set(String key, T value) {
        checkNotNull(key, value);
        return execute(j -> j.set(key, Serializer.serialize(value)));
    }

    @Override
    public <T> String set(String key, T value, SetParams setParams) {
        checkNotNull(key, value, setParams);
        return execute(j -> j.set(key, Serializer.serialize(value), setParams));
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

    @Override
    public <T> String mset(Map<String, T> map) {
        checkNotNull(map);
        return execute(j -> {
            ShardedJedisPipeline pipeline = j.pipelined();
            map.forEach((key, value) -> pipeline.set(key, Serializer.serialize(value)));
            pipeline.sync();
            return "OK";
        });
    }

    @Override
    public String get(String key) {
        checkNotNull(key);
        return execute(j -> j.get(key));
    }

    @Override
    public <T> T get(String key, Class<T> type) {
        return execute(j -> Serializer.deserialize(j.get(key), type));
    }

    @Override
    public List<String> mget(String... keys) {
        checkNotNull(keys);
        return mget(Lists.newArrayList(keys));
    }

    @Override
    public List<String> mget(List<String> keys) {
        checkNotNull(keys);
        return execute(j -> {
            ShardedJedisPipeline pipeline = j.pipelined();
            return keys.stream().map(pipeline::get).map(Response::get).collect(Collectors.toList());
        });
    }

    @Override
    public <T> List<T> mget(List<String> keys, Class<T> type) {
        checkNotNull(keys);
        return execute(j -> {
            ShardedJedisPipeline pipeline = j.pipelined();
            return Serializer.deserialize(keys.stream().map(pipeline::get).map(Response::get).collect(Collectors.toList()), type);
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
        checkNotNull(key, field, value);
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
        return execute(j -> j.hincrByFloat(key.getBytes(), Serializer.serialize(field).getBytes(), value));
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
        return execute(j -> j.hdel(key, Serializer.serialize(fields).toArray(new String[0])));
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
    public String rpoplpush(String srckey, String dstkey) {
        throw new IllegalStateException("cluster redis not support rpoplpush");
    }

    @Override
    public String brpoplpush(String source, String destination, int timeout) {
        throw new IllegalStateException("sharding redis not support brpoplpush");
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
        return execute(j -> {
            ScanResult<byte[]> scanResult = j.sscan(key.getBytes(), cursor.getBytes(), params);
            List<String> strings = scanResult.getResult().stream().map(String::new).collect(Collectors.toList());
            return new ScanResult<>(cursor, strings);
        });
    }

    @Override
    public <T> ScanResult<T> sscan(String key, String cursor, ScanParams params, Class<T> type) {
        checkNotNull(key, cursor, params);
        ScanResult<String> strings = sscan(key, cursor, params);
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
    public String srandmember(String key) {
        checkNotNull(key);
        return execute(j -> j.srandmember(key));
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
        checkNotNull(key);
        return execute(j -> j.zrangeByScore(key, min, max));
    }

    @Override
    public <T> Set<T> zrangeByScore(String key, double min, double max, Class<T> type) {
        checkNotNull(key);
        Set<String> strings = execute(j -> j.zrangeByScore(key, min, max));
        return Serializer.deserialize(strings, type);
    }

    @Override
    public Set<String> zrangeByScore(String key, String min, String max) {
        checkNotNull(key);
        return execute(j -> j.zrangeByScore(key, min, max));
    }

    @Override
    public <T> Set<T> zrangeByScore(String key, String min, String max, Class<T> type) {
        checkNotNull(key);
        Set<String> strings = execute(j -> j.zrangeByScore(key, min, max));
        return Serializer.deserialize(strings, type);
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(String key, double min, double max) {
        checkNotNull(key);
        return execute(j -> j.zrangeByScoreWithScores(key, min, max));
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(String key, double min, double max, int offset, int count) {
        checkNotNull(key);
        return execute(j -> j.zrangeByScoreWithScores(key, min, max, offset, count));
    }

    @Override
    public Set<String> zrevrangeByScore(String key, double max, double min) {
        checkNotNull(key);
        return execute(j -> j.zrevrangeByScore(key, min, max));
    }

    @Override
    public <T> Set<T> zrevrangeByScore(String key, double max, double min, Class<T> type) {
        checkNotNull(key);
        Set<String> strings = execute(j -> j.zrevrangeByScore(key, min, max));
        return Serializer.deserialize(strings, type);
    }

    @Override
    public Set<String> zrevrangeByScore(String key, String max, String min) {
        checkNotNull(key);
        return execute(j -> j.zrevrangeByScore(key, min, max));
    }

    @Override
    public <T> Set<T> zrevrangeByScore(String key, String max, String min, Class<T> type) {
        checkNotNull(key);
        Set<String> strings = execute(j -> j.zrevrangeByScore(key, min, max));
        return Serializer.deserialize(strings, type);
    }

    @Override
    public Set<String> zrangeByLex(String key, String min, String max) {
        checkNotNull(key);
        return execute(j -> j.zrangeByLex(key, min, max));
    }

    @Override
    public <T> Set<T> zrangeByLex(String key, String min, String max, Class<T> type) {
        checkNotNull(key);
        Set<String> strings = execute(j -> j.zrangeByLex(key, min, max));
        return Serializer.deserialize(strings, type);
    }

    @Override
    public Set<String> zrevrangeByLex(String key, String max, String min) {
        checkNotNull(key);
        return execute(j -> j.zrevrangeByLex(key, min, max));
    }

    @Override
    public <T> Set<T> zrevrangeByLex(String key, String max, String min, Class<T> type) {
        checkNotNull(key);
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
        checkNotNull(key, cursor, params);
        return execute(j -> j.zscan(key.getBytes(), cursor.getBytes(), params));
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
        return execute(j -> j.bitpos(key, value, new BitPosParams(start, end)));
    }

    @Override
    public Long bitop(BitOP op, String destKey, String... srcKeys) {
        throw new UnsupportedCommandException("bitop");
    }

    @Override
    public Long bitop(BitOP op, String destKey, List<String> srcKeys) {
        throw new UnsupportedCommandException("bitop");
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
        checkNotNull(key, value);
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
        checkNotNull(key, value);
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
        SetParams setParams = SetParams.setParams();
        setParams.nx();
        setParams.px(expireTime);
        String result = execute(j -> j.set(lockKey, requestId, setParams));
        return "OK".equals(result);
    }

    @Override
    public boolean releaseDistributedLock(String lockKey, String requestId) {
        checkNotNull(lockKey, requestId);
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Object result = execute(j -> j.getShard(lockKey).eval(script, Collections.singletonList(lockKey), Collections.singletonList(requestId)));
        Long success = 1L;
        return success.equals(result);
    }

    @Override
    public <P, T> T eval(String script, int keyCount, List<P> params, Class<T> type) {
        throw new UnsupportedCommandException("eval");
    }

    @Override
    public <P, T> T eval(String script, int keyCount, Class<T> type, P... params) {
        throw new UnsupportedCommandException("eval");
    }

    @Override
    public <P, T, R> R eval(String script, List<P> keys, List<T> args, Class<R> type) {
        throw new UnsupportedCommandException("eval");
    }

}
