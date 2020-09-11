package com.dxy.library.cache.redis;

import com.dxy.library.cache.redis.properties.RedisProperties;
import com.google.common.collect.Maps;
import com.dxy.library.cache.redis.constant.CacheType;
import com.dxy.library.cache.redis.executor.AbstractExecutor;
import com.dxy.library.cache.redis.executor.cluster.RedisClusterExecutor;
import com.dxy.library.cache.redis.executor.sentinel.RedisSentinelExecutor;
import com.dxy.library.cache.redis.executor.sharded.RedisShardedExecutor;
import com.dxy.library.cache.redis.executor.single.RedisSingleExecutor;
import com.dxy.library.cache.redis.inter.RedisConsumer;
import com.dxy.library.cache.redis.inter.RedisFunction;
import com.dxy.library.util.config.dto.Config;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.*;
import redis.clients.jedis.params.GeoRadiusParam;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.params.ZAddParams;
import redis.clients.jedis.params.ZIncrByParams;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * Redis缓存操作（兼容single/sentinel/sharded/cluster四种模式）
 * @author duanxinyuan
 * 2018/8/9 15:27
 */
@Setter
@Slf4j
public class RedisCache {

    private static ConcurrentMap<String, AbstractExecutor<?>> executorMap = Maps.newConcurrentMap();

    private static AbstractExecutor<?> defaultExecutor;

    static {
        RedisProperties redisProperties = new RedisProperties();
        defaultExecutor = getExecutor(redisProperties);
        executorMap.put(Config.DEFAULT_NAME, defaultExecutor);
    }

    /**
     * 获取RedisCache执行器，用于支持多个Redis连接配置
     */
    public static AbstractExecutor<?> name(String name) {
        return executorMap.computeIfAbsent(name, k -> {
            RedisProperties redisProperties = new RedisProperties(name);
            return getExecutor(redisProperties);
        });
    }

    /**
     * 单机 / 主从模式的自定义操作方法
     */
    public static AbstractExecutor<Jedis> single() {
        return (AbstractExecutor<Jedis>) defaultExecutor;
    }

    public static AbstractExecutor<Jedis> single(String name) {
        return (AbstractExecutor<Jedis>) name(name);
    }

    /**
     * 哨兵模式的自定义操作方法
     */
    public static AbstractExecutor<Jedis> sentinel() {
        return (AbstractExecutor<Jedis>) defaultExecutor;
    }

    public static AbstractExecutor<Jedis> sentinel(String name) {
        return (AbstractExecutor<Jedis>) name(name);
    }

    /**
     * 分片模式的自定义操作方法
     */
    public static AbstractExecutor<ShardedJedis> sharded() {
        return (AbstractExecutor<ShardedJedis>) defaultExecutor;
    }

    public static AbstractExecutor<ShardedJedis> sharded(String name) {
        return (AbstractExecutor<ShardedJedis>) name(name);
    }

    /**
     * 集群模式的自定义操作方法
     */
    public static AbstractExecutor<JedisCluster> cluster() {
        return (AbstractExecutor<JedisCluster>) defaultExecutor;
    }

    public static AbstractExecutor<JedisCluster> cluster(String name) {
        return (AbstractExecutor<JedisCluster>) name(name);
    }

    public static AbstractExecutor<?> getExecutor(RedisProperties redisProperties) {
        CacheType cacheType = CacheType.getType(redisProperties.getType());
        switch (cacheType) {
            case sentinel:
                return new RedisSentinelExecutor(redisProperties);
            case sharded:
                return new RedisShardedExecutor(redisProperties);
            case cluster:
                return new RedisClusterExecutor(redisProperties);
            case single:
            default:
                return new RedisSingleExecutor(redisProperties);
        }
    }

    /**
     * 自定义操作方法
     */
    public static void executeVoid(RedisConsumer<?> consumer) {
        CacheType cacheType = CacheType.getType(defaultExecutor.getRedisProperties().getType());
        switch (cacheType) {
            case sharded:
                ((AbstractExecutor<ShardedJedis>) defaultExecutor).executeVoid((RedisConsumer<ShardedJedis>) consumer);
                break;
            case cluster:
                ((AbstractExecutor<JedisCluster>) defaultExecutor).executeVoid((RedisConsumer<JedisCluster>) consumer);
                break;
            case sentinel:
            case single:
            default:
                ((AbstractExecutor<Jedis>) defaultExecutor).executeVoid((RedisConsumer<Jedis>) consumer);
                break;
        }
    }

    /**
     * 自定义操作方法
     */
    public static <T> T execute(RedisFunction<?, T> function) {
        CacheType cacheType = CacheType.getType(defaultExecutor.getRedisProperties().getType());
        switch (cacheType) {
            case sharded:
                return ((AbstractExecutor<ShardedJedis>) defaultExecutor).execute((RedisFunction<ShardedJedis, T>) function);
            case cluster:
                return ((AbstractExecutor<JedisCluster>) defaultExecutor).execute((RedisFunction<JedisCluster, T>) function);
            case sentinel:
            case single:
            default:
                return ((AbstractExecutor<Jedis>) defaultExecutor).execute((RedisFunction<Jedis, T>) function);
        }
    }

    /**
     * 查找所有符合给定模式 pattern 的 key，返回符合给定模式的 key 列表
     * @param pattern key规则，支持正则
     */
    public static Set<String> keys(String pattern) {
        return defaultExecutor.keys(pattern);
    }

    /**
     * 迭代查找所有符合给定模式 pattern 的 key，返回 count 个符合给定模式的 key 列表
     * @param cursor 光标，从0开始，scan命令会返回一个新的游标，如果新的游标不是0，表示遍历还没有结束，继续遍历需要使用新的游标作为参数，继续输入获得后面的结果
     * @param params pattern key规则，以及迭代的 key 的个数
     */
    public static ScanResult<String> scan(String cursor, ScanParams params) {
        return defaultExecutor.scan(cursor, params);
    }

    /**
     * 返回 key 所储存的值的类型
     */
    public static String type(String key) {
        return defaultExecutor.type(key);
    }

    /**
     * 以秒为单位，返回给定 key 的剩余生存时间(TTL, time to live)
     * 当 key 不存在时，返回 -2
     * 当 key 存在但没有设置剩余生存时间时，返回 -1
     * 否则，以秒为单位，返回 key 的剩余生存时间
     */
    public static Long ttl(String key) {
        return defaultExecutor.ttl(key);
    }

    /**
     * 以毫秒为单位，返回给定 key 的剩余生存时间(TTL, time to live)
     * 当 key 不存在时，返回 -2
     * 当 key 存在但没有设置剩余生存时间时，返回 -1
     * 否则，以毫秒为单位，返回 key 的剩余生存时间
     */
    public static Long pttl(String key) {
        return defaultExecutor.ttl(key);
    }

    /**
     * 设置键过期时间，返回1表示设置成功
     */
    public static Long expire(String key, int seconds) {
        return defaultExecutor.expire(key, seconds);
    }

    /**
     * 以 UNIX 时间戳(unix timestamp)格式设置 key 的过期时间，返回1表示设置成功
     */
    public static Long expireAt(String key, long unixTime) {
        return defaultExecutor.expireAt(key, unixTime);
    }

    /**
     * 清除设置的过期时间，将Key设置为永久有效，返回1表示设置成功
     */
    public static Long persist(String key) {
        return defaultExecutor.persist(key);
    }

    /**
     * key是否存在，返回true表示成功
     */
    public static boolean exists(String key) {
        return defaultExecutor.exists(key);
    }

    /**
     * 删除key，返回删除的 key 的数量
     */
    public static Long del(String key) {
        return defaultExecutor.del(key);
    }

    /**
     * 批量删除key，返回删除的 key 的数量
     */
    public static Long del(String... keys) {
        return defaultExecutor.del(keys);
    }

    /**
     * 批量删除key，返回删除的 key 的数量
     */
    public static Long del(List<String> keys) {
        return defaultExecutor.del(keys);
    }

    /**
     * 异步删除key，返回删除的 key 的数量
     */
    public static Long unlink(String key) {
        return defaultExecutor.unlink(key);
    }

    /**
     * 将 oldkey 重命名为 newkey，返回OK表示成功
     * @param oldkey 旧的Key名称
     * @param newkey 新的Key名称
     */
    public static String rename(String oldkey, String newkey) {
        return defaultExecutor.rename(oldkey, newkey);
    }

    /**
     * 将 oldkey 重命名为 newkey（newkey 不存在才设置，原子方法），返回1表示成功
     * @param oldkey 旧的Key名称
     * @param newkey 新的Key名称
     */
    public static Long renamenx(String oldkey, String newkey) {
        return defaultExecutor.renamenx(oldkey, newkey);
    }

    /********** string相关操作 ************/

    /**
     * 为指定的 key 设置值，返回OK表示成功
     */
    public static <T> String set(String key, T value) {
        return defaultExecutor.set(key, value);
    }

    /**
     * 为指定的 key 设置值，返回OK表示成功
     * @param key 键
     * @param value 值
     * @param setParams set命令的其他参数
     */
    public static <T> String set(String key, T value, SetParams setParams) {
        return defaultExecutor.set(key, value, setParams);
    }

    /**
     * 为指定的 key 设置值（不存在才设置，原子方法），返回1表示成功
     */
    public static <T> Long setnx(String key, T value) {
        return defaultExecutor.setnx(key, value);
    }

    /**
     * 为指定的 key 设置值及其过期时间（单位为秒），返回OK表示成功
     */
    public static <T> String setex(String key, int seconds, T value) {
        return defaultExecutor.setex(key, seconds, value);
    }

    /**
     * 为指定的 key 设置值及其过期时间（单位为秒），返回OK表示成功
     */
    public static <T> String setex(String key, long time, TimeUnit timeUnit, T value) {
        return defaultExecutor.setex(key, time, timeUnit, value);
    }

    /**
     * 为指定的 key 设置值及其过期时间（单位为毫秒），返回OK表示成功
     */
    public static <T> String psetex(String key, long milliseconds, T value) {
        return defaultExecutor.psetex(key, milliseconds, value);
    }

    /**
     * 为批量的 key 设置值，返回OK表示成功
     */
    public static <T> String mset(Map<String, T> map) {
        return defaultExecutor.mset(map);
    }

    /**
     * 返回指定的 key 值
     */
    public static String get(String key) {
        return defaultExecutor.get(key);
    }

    /**
     * 返回指定的 key 值
     */
    public static <T> T get(String key, Class<T> type) {
        return defaultExecutor.get(key, type);
    }

    /**
     * 批量返回的 key 值
     */
    public static List<String> mget(String... keys) {
        return defaultExecutor.mget(keys);
    }

    /**
     * 批量返回的 key 值
     */
    public static List<String> mget(List<String> keys) {
        return defaultExecutor.mget(keys);
    }

    /**
     * 批量返回的 key 值
     */
    public static <T> List<T> mget(List<String> keys, Class<T> type) {
        return defaultExecutor.mget(keys, type);
    }

    /**
     * 将存储的数字key加1，返回key增量后的值
     */
    public static Long incr(String key) {
        return defaultExecutor.incr(key);
    }

    /**
     * 对数值增加指定值，返回key增量后的值
     */
    public static Long incrBy(String key, long increment) {
        return defaultExecutor.incrBy(key, increment);
    }

    /**
     * 为 key 中所储存的值加上指定的浮点数增量值，执行命令之后 key 的值
     */
    public static Double incrByFloat(String key, double increment) {
        return defaultExecutor.incrByFloat(key, increment);
    }

    /**
     * 将 key 中储存的数字值减一，返回执行命令之后 key 的值
     */
    public static Long decr(String key) {
        return defaultExecutor.decr(key);
    }

    /**
     * 将 key 所储存的值减去指定的减量值，返回减去指定减量值之后key 的值
     */
    public static Long decrBy(String key, long decrement) {
        return defaultExecutor.decrBy(key, decrement);
    }

    /**
     * 为指定的 key 追加值，返回追加指定值之后 key 中字符串的长度
     */
    public static Long append(String key, String value) {
        return defaultExecutor.append(key, value);
    }

    /**
     * 返回键 key 储存的字符串值的长度
     */
    public static Long strlen(String key) {
        return defaultExecutor.strlen(key);
    }


    /********** hash相关操作 ************/

    /**
     * 添加键值对到map，返回1表示成功
     */
    public static <P, T> Long hset(String key, P field, T value) {
        return defaultExecutor.hset(key, field, value);
    }

    /**
     * 添加键值对到map，返回OK表示成功
     */
    public static <P, T> String hmset(String key, Map<P, T> hash) {
        return defaultExecutor.hmset(key, hash);
    }

    /**
     * 只有在字段 field 不存在时，设置哈希表字段的值，返回1表示field是散列中的新字段并且value已设置
     */
    public static <P, T> Long hsetnx(String key, P field, T value) {
        return defaultExecutor.hsetnx(key, field, value);
    }

    /**
     * 返回哈希表中给定域的值
     */
    public static <P> String hget(String key, P field) {
        return defaultExecutor.hget(key, field);
    }

    /**
     * 返回哈希表中给定域的值
     */
    public static <P, T> T hget(String key, P field, Class<T> type) {
        return defaultExecutor.hget(key, field, type);
    }

    /**
     * 返回哈希表 key 中一个或多个给定域的值，返回list
     */
    public static <P> List<String> hmget(String key, P... fields) {
        return defaultExecutor.hmget(key, fields);
    }

    /**
     * 返回哈希表 key 中一个或多个给定域的值，返回list
     */
    public static <P> List<String> hmget(String key, List<P> fields) {
        return defaultExecutor.hmget(key, fields);
    }

    /**
     * 返回哈希表 key 中一个或多个给定域的值，返回list
     */
    public static <P, T> List<T> hmget(String key, List<P> fields, Class<T> type) {
        return defaultExecutor.hmget(key, fields, type);
    }

    /**
     * 为哈希表 key 中的域 field 的值加上增量 increment
     */
    public static <P> Long hincrBy(String key, P field, long value) {
        return defaultExecutor.hincrBy(key, field, value);
    }

    /**
     * 为哈希表 key 中的域 field 加上浮点数增量，返回执行加法操作之后 field 域的值
     */
    public static <P> Double hincrByFloat(String key, P field, double value) {
        return defaultExecutor.hincrByFloat(key, field, value);
    }

    /**
     * 返回哈希表 key 中的所有域
     */
    public static Set<String> hkeys(String key) {
        return defaultExecutor.hkeys(key);
    }

    /**
     * 返回哈希表 key 中所有域的值
     */
    public static List<String> hvals(String key) {
        return defaultExecutor.hvals(key);
    }

    /**
     * 返回哈希表 key 中所有域的值
     */
    public static <T> List<T> hvals(String key, Class<T> type) {
        return defaultExecutor.hvals(key, type);
    }

    /**
     * 返回哈希表 key 中，所有的域和值
     */
    public static Map<String, String> hgetAll(String key) {
        return defaultExecutor.hgetAll(key);
    }

    /**
     * 返回哈希表 key 中，所有的域和值
     */
    public static <T> Map<String, T> hgetAll(String key, Class<T> type) {
        return defaultExecutor.hgetAll(key, type);
    }

    /**
     * 从哈希表 key 中迭代查找所有符合给定模式 pattern 的域和值
     * @param cursor 游标名
     */
    public static ScanResult<Map.Entry<String, String>> hscan(String key, String cursor) {
        return defaultExecutor.hscan(key, cursor);
    }

    /**
     * 从哈希表 key 中迭代查找 count 个符合给定模式 pattern 的域和值
     * @param cursor 游标名
     * @param params pattern key规则，以及迭代的 key 的个数
     */
    public static ScanResult<Map.Entry<String, String>> hscan(String key, String cursor, ScanParams params) {
        return defaultExecutor.hscan(key, cursor, params);
    }

    /**
     * 检查给定域 field 是否存在于哈希表 hash 当中，返回1表示存在
     */
    public static <P> boolean hexists(String key, P field) {
        return defaultExecutor.hexists(key, field);
    }

    /**
     * 删除哈希表 key 中的一个或多个指定域，返回被成功移除的域的数量
     */
    public static <P> Long hdel(String key, P... fields) {
        return defaultExecutor.hdel(key, fields);
    }

    /**
     * 删除哈希表 key 中的一个或多个指定域，返回被成功移除的域的数量
     */
    public static <P> Long hdel(String key, List<P> fields) {
        return defaultExecutor.hdel(key, fields);
    }

    /**
     * 返回哈希表 key 中， 与给定域 field 相关联的值的字符串长度
     * @return 字符串长度
     */
    public static <P> Long hstrlen(String key, P field) {
        return defaultExecutor.hstrlen(key, field);
    }


    /********** list相关操作 ************/

    /**
     * 将一个或多个值 value 插入到列表 key 的表头，返回命令执行后列表的长度
     */
    public static <T> Long lpush(String key, T... values) {
        return defaultExecutor.lpush(key, values);
    }

    /**
     * 将一个或多个值 value 插入到列表 key 的表头，返回命令执行后列表的长度
     */
    public static <T> Long lpush(String key, List<T> values) {
        return defaultExecutor.lpush(key, values);
    }

    /**
     * 将一个或多个值 value 插入到列表 key 的表尾(最右边)，返回命令执行后列表的长度
     */
    public static <T> Long rpush(String key, T... values) {
        return defaultExecutor.rpush(key, values);
    }

    /**
     * 将一个或多个值 value 插入到列表 key 的表尾(最右边)，返回命令执行后列表的长度
     */
    public static <T> Long rpush(String key, List<T> values) {
        return defaultExecutor.rpush(key, values);
    }

    /**
     * 将值 value 插入到列表 key 的表头，当且仅当 key 存在并且是一个列表，返回命令执行后列表的长度
     */
    public static <T> Long lpushx(String key, T... values) {
        return defaultExecutor.lpushx(key, values);
    }

    /**
     * 将值 value 插入到列表 key 的表头，当且仅当 key 存在并且是一个列表，返回命令执行后列表的长度
     */
    public static <T> Long lpushx(String key, List<T> values) {
        return defaultExecutor.lpushx(key, values);
    }

    /**
     * 将值 value 插入到列表 key 的表尾，当且仅当 key 存在并且是一个列表，返回命令执行后列表的长度
     */
    public static <T> Long rpushx(String key, T... values) {
        return defaultExecutor.rpushx(key, values);
    }

    /**
     * 将值 value 插入到列表 key 的表尾，当且仅当 key 存在并且是一个列表，返回命令执行后列表的长度
     */
    public static <T> Long rpushx(String key, List<T> values) {
        return defaultExecutor.rpushx(key, values);
    }

    /**
     * 将列表 key 下标为 index 的元素的值设置为 value，操作成功返回 ok
     */
    public static <T> String lset(String key, long index, T value) {
        return defaultExecutor.lset(key, index, value);
    }

    /**
     * 将值 value 插入到列表 key 当中，位于值 pivot 之前或之后，返回返回插入操作完成之后，列表的长度
     */
    public static <T> Long linsert(String key, ListPosition where, String pivot, T value) {
        return defaultExecutor.linsert(key, where, pivot, value);
    }

    /**
     * 返回列表 key 中指定区间内的元素，起始下标为start，结束下标为end
     * 下标参数 start 和 stop 都以 0 为底，以 0 表示有序集第一个成员，以 -1 表示最后一个成员
     */
    public static List<String> lrange(String key, long start, long end) {
        return defaultExecutor.lrange(key, start, end);
    }

    /**
     * 返回列表 key 中指定区间内的元素，起始下标为start，结束下标为end
     * 下标参数 start 和 stop 都以 0 为底，以 0 表示有序集第一个成员，以 -1 表示最后一个成员
     */
    public static <T> List<T> lrange(String key, long start, long end, Class<T> type) {
        return defaultExecutor.lrange(key, start, end, type);
    }

    /**
     * 返回列表 key 中指定区间内的元素，起始下标为pageNo*pageSize，结束下标为(pageNo+1)*pageSize
     */
    public static List<String> lrangePage(String key, int pageNo, int pageSize) {
        return defaultExecutor.lrangePage(key, pageNo, pageSize);
    }

    /**
     * 返回列表 key 中指定区间内的元素，起始下标为pageNo*pageSize，结束下标为(pageNo+1)*pageSize
     */
    public static <T> List<T> lrangePage(String key, int pageNo, int pageSize, Class<T> type) {
        return defaultExecutor.lrangePage(key, pageNo, pageSize, type);
    }

    /**
     * 返回列表 key 中的所有元素
     */
    public static List<String> lrangeAll(String key) {
        return defaultExecutor.lrangeAll(key);
    }

    /**
     * 返回列表 key 中的所有元素
     */
    public static <T> List<T> lrangeAll(String key, Class<T> type) {
        return defaultExecutor.lrangeAll(key, type);
    }

    /**
     * 返回列表 key 中，下标为 index 的元素
     */
    public static String lindex(String key, int index) {
        return defaultExecutor.lindex(key, index);
    }

    /**
     * 返回列表 key 中，下标为 index 的元素
     */
    public static <T> T lindex(String key, int index, Class<T> type) {
        return defaultExecutor.lindex(key, index, type);
    }

    /**
     * 返回列表 key 的长度
     */
    public static Long llen(String key) {
        return defaultExecutor.llen(key);
    }

    /**
     * 移除并返回列表 key 的头元素，返回列表的头元素。 当 key 不存在时，返回 null
     */
    public static String lpop(String key) {
        return defaultExecutor.lpop(key);
    }

    /**
     * 移除并返回列表 key 的头元素，返回列表的头元素。 当 key 不存在时，返回 null
     */
    public static <T> T lpop(String key, Class<T> type) {
        return defaultExecutor.lpop(key, type);
    }

    /**
     * 移除并返回列表 key 的尾元素，返回列表的尾元素。 当 key 不存在时，返回 null
     */
    public static String rpop(String key) {
        return defaultExecutor.rpop(key);
    }

    /**
     * 移除并返回列表 key 的尾元素，返回列表的尾元素。 当 key 不存在时，返回 null
     */
    public static <T> T rpop(String key, Class<T> type) {
        return defaultExecutor.rpop(key, type);
    }

    /**
     * 移除并返回列表 key 的头元素，返回列表的头元素。 如果列表没有元素会阻塞列表直到等待超时或发现可弹出元素为止
     */
    public static List<String> blpop(int timeout, String key) {
        return defaultExecutor.blpop(timeout, key);
    }

    /**
     * 移除并返回列表 key 的尾元素，返回列表的尾元素。 如果列表没有元素会阻塞列表直到等待超时或发现可弹出元素为止
     * @param timeout 超时时间，单位为秒
     */
    public static List<String> brpop(int timeout, String key) {
        return defaultExecutor.brpop(timeout, key);
    }

    /**
     * 根据参数 count 的值，移除列表中与参数 value 相等的元素
     * @param count 表示动作或者查找方向 ，
     * count=0，移除所有匹配的元素
     * count<0，从表尾开始向表头搜索，移除与 value 相等的元素，数量为 count 的绝对值
     * count>0，从表头开始向表尾搜索，移除与 value 相等的元素，数量为 count
     * @param value 匹配的元素
     */
    public static <T> Long lrem(String key, long count, T value) {
        return defaultExecutor.lrem(key, count, value);
    }

    /**
     * 对一个列表进行修剪(trim)，让列表只保留指定区间内的元素，不在指定区间之内的元素都将被删除。返回OK表示修剪成功
     * @param key 键
     * @param start 可以为负数（-1是列表的最后一个元素，-2是列表倒数第二的元素。）
     * 如果start大于end，则返回一个空的列表，即列表被清空
     * @param end 可以为负数（-1是列表的最后一个元素，-2是列表倒数第二的元素。）可以超出索引，不影响结果
     */
    public static String ltrim(String key, long start, long end) {
        return defaultExecutor.ltrim(key, start, end);
    }


    /********** set相关操作 ************/

    /**
     * 将一个或多个 member 元素加入到集合 key 当中，返回被添加到集合中的新元素的数量
     */
    public static <T> Long sadd(String key, T... values) {
        return defaultExecutor.sadd(key, values);
    }

    /**
     * 将一个或多个 member 元素加入到集合 key 当中，返回被添加到集合中的新元素的数量
     */
    public static <T> Long sadd(String key, List<T> values) {
        return defaultExecutor.sadd(key, values);
    }

    /**
     * 返回集合中的所有成员
     */
    public static Set<String> smembers(String key) {
        return defaultExecutor.smembers(key);
    }

    /**
     * 返回集合中的所有成员
     */
    public static <T> Set<T> smembers(String key, Class<T> type) {
        return defaultExecutor.smembers(key, type);
    }

    /**
     * 从集合 key 中迭代查找所有符合给定模式 pattern 的元素
     * @param cursor 游标名
     */
    public static ScanResult<String> sscan(String key, String cursor) {
        return defaultExecutor.sscan(key, cursor);
    }

    /**
     * 从集合 key 中迭代查找所有符合给定模式 pattern 的元素
     * @param cursor 游标名
     */
    public static <T> ScanResult<T> sscan(String key, String cursor, Class<T> type) {
        return defaultExecutor.sscan(key, cursor, type);
    }

    /**
     * 从集合 key 中迭代查找 count 个符合给定模式 pattern 的元素
     * @param cursor 游标名
     * @param params pattern key规则，以及迭代的 key 的个数
     */
    public static ScanResult<String> sscan(String key, String cursor, ScanParams params) {
        return defaultExecutor.sscan(key, cursor, params);
    }

    /**
     * 从集合 key 中迭代查找 count 个符合给定模式 pattern 的元素
     * @param cursor 游标名
     * @param params pattern key规则，以及迭代的 key 的个数
     */
    public static <T> ScanResult<T> sscan(String key, String cursor, ScanParams params, Class<T> type) {
        return defaultExecutor.sscan(key, cursor, params, type);
    }

    /**
     * 移除集合 key 中的一个或多个 member 元素，返回被成功移除的元素的数量
     */
    public static <T> Long srem(String key, T... values) {
        return defaultExecutor.srem(key, values);
    }

    /**
     * 移除集合 key 中的一个或多个 member 元素，返回被成功移除的元素的数量
     */
    public static <T> Long srem(String key, List<T> values) {
        return defaultExecutor.srem(key, values);
    }

    /**
     * 移除并返回集合 key 中的一个随机元素，返回被移除的随机元素
     */
    public static String spop(String key) {
        return defaultExecutor.spop(key);
    }

    /**
     * 移除并返回集合 key 中的一个随机元素，返回被移除的随机元素
     */
    public static <T> T spop(String key, Class<T> type) {
        return defaultExecutor.spop(key, type);
    }

    /**
     * 返回集合 key 中元素的数量
     */
    public static Long scard(String key) {
        return defaultExecutor.scard(key);
    }

    /**
     * 判断 member 元素是否是集合 key 的成员，如果 member 元素是集合的成员，返回 1
     */
    public static <T> boolean sismember(String key, T value) {
        return defaultExecutor.sismember(key, value);
    }

    /**
     * 返回集合 key 中的一个随机元素
     */
    public static String srandmember(String key) {
        return defaultExecutor.srandmember(key);
    }

    /**
     * 回集合 key 中的 count 个随机元素
     * count>0，且小于集合基数，那么命令返回一个包含 count 个元素的数组，数组中的元素各不相同。如果 count 大于等于集合基数，那么返回整个集合
     * count<0，那么命令返回一个数组，数组中的元素可能会重复出现多次，而数组的长度为 count 的绝对值
     */
    public static List<String> srandmember(String key, int count) {
        return defaultExecutor.srandmember(key, count);
    }

    /**
     * 回集合 key 中的 count 个随机元素
     * count>0，且小于集合基数，那么命令返回一个包含 count 个元素的数组，数组中的元素各不相同。如果 count 大于等于集合基数，那么返回整个集合
     * count<0，那么命令返回一个数组，数组中的元素可能会重复出现多次，而数组的长度为 count 的绝对值
     */
    public <T> List<T> srandmember(String key, int count, Class<T> type) {
        return defaultExecutor.srandmember(key, count, type);
    }


    /********** sortedset相关操作 ************/

    /**
     * 将一个 member 元素及其 score 值加入到有序集 key 当中，返回被成功添加的新成员的数量，不包括那些被更新的、已经存在的成员
     * @param score 排序参数，可以是整数值或双精度浮点数
     */
    public static <T> Long zadd(String key, double score, T member) {
        return defaultExecutor.zadd(key, score, member);
    }

    /**
     * 将一个 member 元素及其 score 值加入到有序集 key 当中，返回被成功添加的新成员的数量，不包括那些被更新的、已经存在的成员
     * @param score 排序参数，可以是整数值或双精度浮点数
     */
    public static <T> Long zadd(String key, double score, T member, ZAddParams params) {
        return defaultExecutor.zadd(key, score, member, params);
    }

    /**
     * 将多个 member 元素及其 score 值加入到有序集 key 当中，返回被成功添加的新成员的数量，不包括那些被更新的、已经存在的成员
     */
    public static Long zadd(String key, Map<String, Double> scoreMembers) {
        return defaultExecutor.zadd(key, scoreMembers);
    }

    /**
     * 将多个 member 元素及其 score 值加入到有序集 key 当中，返回被成功添加的新成员的数量，不包括那些被更新的、已经存在的成员
     */
    public static Long zadd(String key, Map<String, Double> scoreMembers, ZAddParams params) {
        return defaultExecutor.zadd(key, scoreMembers, params);
    }

    /**
     * 返回有序集 key 中指定区间内的成员，其中成员的位置按 score 升序排序
     * 起始下标为start，结束下标为end
     * 下标参数 start 和 stop 都以 0 为底，以 0 表示有序集第一个成员，以 -1 表示最后一个成员
     * 返回指定区间内，带有 score 值(可选)的有序集成员的列表
     */
    public static Set<String> zrange(String key, long start, long stop) {
        return defaultExecutor.zrange(key, start, stop);
    }

    /**
     * 返回有序集 key 中指定区间内的成员，其中成员的位置按 score 升序排序
     * 起始下标为start，结束下标为end
     * 下标参数 start 和 stop 都以 0 为底，以 0 表示有序集第一个成员，以 -1 表示最后一个成员
     * 返回指定区间内，带有 score 值(可选)的有序集成员的列表
     */
    public static <T> Set<T> zrange(String key, long start, long stop, Class<T> type) {
        return defaultExecutor.zrange(key, start, stop, type);
    }

    /**
     * 返回有序集 key 中指定区间内的成员，其中成员的位置按 score 降序排列
     * 起始下标为start，结束下标为end
     * 下标参数 start 和 stop 都以 0 为底，以 0 表示有序集第一个成员，以 -1 表示最后一个成员
     * 返回指定区间内，带有 score 值(可选)的有序集成员的列表
     */
    public static Set<String> zrevrange(String key, long start, long stop) {
        return defaultExecutor.zrevrange(key, start, stop);
    }

    /**
     * 返回有序集 key 中指定区间内的成员，其中成员的位置按 score 降序排列
     * 起始下标为start，结束下标为end
     * 下标参数 start 和 stop 都以 0 为底，以 0 表示有序集第一个成员，以 -1 表示最后一个成员
     * 返回指定区间内，带有 score 值(可选)的有序集成员的列表
     */
    public static <T> Set<T> zrevrange(String key, long start, long stop, Class<T> type) {
        return defaultExecutor.zrevrange(key, start, stop, type);
    }

    /**
     * 移除有序集 key 中的一个或多个成员，返回被成功移除的成员的数量
     */
    public static <T> Long zrem(String key, T... members) {
        return defaultExecutor.zrem(key, members);
    }

    /**
     * 移除有序集 key 中的一个或多个成员，返回被成功移除的成员的数量
     */
    public static <T> Long zrem(String key, List<T> members) {
        return defaultExecutor.zrem(key, members);
    }

    /**
     * 为有序集 key 的成员 member 的 score 值加上增量 increment，返回member 成员的新 score 值
     */
    public static <T> Double zincrby(String key, double increment, T member) {
        return defaultExecutor.zincrby(key, increment, member);
    }

    /**
     * 为有序集 key 的成员 member 的 score 值加上增量 increment，返回member 成员的新 score 值
     */
    public static <T> Double zincrby(String key, double increment, T member, ZIncrByParams params) {
        return defaultExecutor.zincrby(key, increment, member, params);
    }

    /**
     * 返回有序集 key 中， score 值介于 max 和 min 之间(默认包括等于 max 或 min )的所有的成员
     * 有序集成员按 score 值升序排列
     */
    public static Set<String> zrangeByScore(String key, double min, double max) {
        return defaultExecutor.zrangeByScore(key, min, max);
    }

    /**
     * 返回有序集 key 中， score 值介于 max 和 min 之间(默认包括等于 max 或 min )的所有的成员
     * 有序集成员按 score 值升序排列
     */
    public static <T> Set<T> zrangeByScore(String key, double min, double max, Class<T> type) {
        return defaultExecutor.zrangeByScore(key, min, max, type);
    }

    /**
     * 返回有序集 key 中， score 值介于 max 和 min 之间(默认包括等于 max 或 min )的所有的成员
     * 有序集成员按 score 值升序排列
     */
    public static Set<String> zrangeByScore(String key, String min, String max) {
        return defaultExecutor.zrangeByScore(key, min, max);
    }

    /**
     * 返回有序集 key 中， score 值介于 max 和 min 之间(默认包括等于 max 或 min )的所有的成员
     * 有序集成员按 score 值升序排列
     */
    public static <T> Set<T> zrangeByScore(String key, String min, String max, Class<T> type) {
        return defaultExecutor.zrangeByScore(key, min, max, type);
    }

    /**
     * 返回有序集 key 中， score 值介于 max 和 min 之间(默认包括等于 max 或 min )的所有的成员及其分数
     * 有序集成员按 score 值升序排列
     */
    public static Set<Tuple> zrangeByScoreWithScores(String key, double min, double max) {
        return defaultExecutor.zrangeByScoreWithScores(key, min, max);
    }

    /**
     * 返回有序集 key 中， score 值介于 max 和 min 之间(默认包括等于 max 或 min )的所有的成员
     * 有序集成员按 score 值降序排列
     */
    public static Set<String> zrevrangeByScore(String key, double max, double min) {
        return defaultExecutor.zrevrangeByScore(key, min, max);
    }

    /**
     * 返回有序集 key 中， score 值介于 max 和 min 之间(默认包括等于 max 或 min )的所有的成员
     * 有序集成员按 score 值降序排列
     */
    public static <T> Set<T> zrevrangeByScore(String key, double max, double min, Class<T> type) {
        return defaultExecutor.zrevrangeByScore(key, min, max, type);
    }

    /**
     * 返回有序集 key 中， score 值介于 max 和 min 之间(默认包括等于 max 或 min )的所有的成员
     * 有序集成员按 score 值降序排列
     */
    public static Set<String> zrevrangeByScore(String key, String max, String min) {
        return defaultExecutor.zrevrangeByScore(key, min, max);
    }

    /**
     * 返回有序集 key 中， score 值介于 max 和 min 之间(默认包括等于 max 或 min )的所有的成员
     * 有序集成员按 score 值降序排列
     */
    public static <T> Set<T> zrevrangeByScore(String key, String max, String min, Class<T> type) {
        return defaultExecutor.zrevrangeByScore(key, min, max, type);
    }

    /**
     * 返回给定的有序集合键 key 中， 值介于 min 和 max 之间的成员
     * 有序集成员按 score 值升序排列
     * 合法的 min 和 max 参数必须包含 ( 或者 [ ， 其中 ( 表示开区间（指定的值不会被包含在范围之内）， 而 [ 则表示闭区间（指定的值会被包含在范围之内）
     * 特殊值 + 和 - 在 min 参数以及 max 参数中具有特殊的意义， 其中 + 表示正无限， 而 - 表示负无限。
     * 因此， 向一个所有成员的分值都相同的有序集合发送命令 ZRANGEBYLEX <zset> - + ， 命令将返回有序集合中的所有元素
     */
    public static Set<String> zrangeByLex(String key, String min, String max) {
        return defaultExecutor.zrangeByLex(key, min, max);
    }

    /**
     * 返回给定的有序集合键 key 中， 值介于 min 和 max 之间的成员
     * 有序集成员按 score 值升序排列
     * 合法的 min 和 max 参数必须包含 ( 或者 [ ， 其中 ( 表示开区间（指定的值不会被包含在范围之内）， 而 [ 则表示闭区间（指定的值会被包含在范围之内）
     * 特殊值 + 和 - 在 min 参数以及 max 参数中具有特殊的意义， 其中 + 表示正无限， 而 - 表示负无限。
     * 因此， 向一个所有成员的分值都相同的有序集合发送命令 ZRANGEBYLEX <zset> - + ， 命令将返回有序集合中的所有元素
     */
    public static <T> Set<T> zrangeByLex(String key, String min, String max, Class<T> type) {
        return defaultExecutor.zrangeByLex(key, min, max, type);
    }

    /**
     * 返回给定的有序集合键 key 中， 值介于 min 和 max 之间的成员
     * 有序集成员按 score 值降序排列
     * 合法的 min 和 max 参数必须包含 ( 或者 [ ， 其中 ( 表示开区间（指定的值不会被包含在范围之内）， 而 [ 则表示闭区间（指定的值会被包含在范围之内）
     * 特殊值 + 和 - 在 min 参数以及 max 参数中具有特殊的意义， 其中 + 表示正无限， 而 - 表示负无限。
     * 因此， 向一个所有成员的分值都相同的有序集合发送命令 ZRANGEBYLEX <zset> - + ， 命令将返回有序集合中的所有元素
     */
    public static Set<String> zrevrangeByLex(String key, String max, String min) {
        return defaultExecutor.zrevrangeByLex(key, min, max);
    }

    /**
     * 返回给定的有序集合键 key 中， 值介于 min 和 max 之间的成员
     * 有序集成员按 score 值降序排列
     * 合法的 min 和 max 参数必须包含 ( 或者 [ ， 其中 ( 表示开区间（指定的值不会被包含在范围之内）， 而 [ 则表示闭区间（指定的值会被包含在范围之内）
     * 特殊值 + 和 - 在 min 参数以及 max 参数中具有特殊的意义， 其中 + 表示正无限， 而 - 表示负无限。
     * 因此， 向一个所有成员的分值都相同的有序集合发送命令 ZRANGEBYLEX <zset> - + ， 命令将返回有序集合中的所有元素
     */
    public static <T> Set<T> zrevrangeByLex(String key, String max, String min, Class<T> type) {
        return defaultExecutor.zrevrangeByLex(key, min, max, type);
    }

    /**
     * 从有序集 key 中迭代查找所有符合给定模式 pattern 的元素
     * @param cursor 游标名
     */
    public static ScanResult<Tuple> zscan(String key, String cursor) {
        return defaultExecutor.zscan(key, cursor);
    }

    /**
     * 从有序集 key 中迭代查找 count 个符合给定模式 pattern 的元素
     * @param cursor 游标名
     * @param params pattern key规则，以及迭代的 key 的个数
     */
    public static ScanResult<Tuple> zscan(String key, String cursor, ScanParams params) {
        return defaultExecutor.zscan(key, cursor, params);
    }

    /**
     * 移除有序集 key 中，指定排名(rank)区间内的所有成员，返回被移除成员的数量
     */
    public static Long zremrangeByRank(String key, long start, long stop) {
        return defaultExecutor.zremrangeByRank(key, start, stop);
    }

    /**
     * 移除有序集 key 中，所有 score 值介于 min 和 max 之间(包括等于 min 或 max )的成员，返回被移除成员的数量
     */
    public static Long zremrangeByScore(String key, double min, double max) {
        return defaultExecutor.zremrangeByScore(key, min, max);
    }

    /**
     * 移除有序集 key 中，所有 score 值介于 min 和 max 之间(包括等于 min 或 max )的成员，返回被移除成员的数量
     */
    public static Long zremrangeByScore(String key, String min, String max) {
        return defaultExecutor.zremrangeByScore(key, min, max);
    }

    /**
     * 移除该集合中， 成员介于 min 和 max 范围内的所有元素，返回被移除成员的数量
     */
    public static Long zremrangeByLex(String key, String min, String max) {
        return defaultExecutor.zremrangeByLex(key, min, max);
    }

    /**
     * 返回有序集 key 中成员 member 的排名，其中有序集成员按 score 升序排列
     * 排名以 0 为底， score 值最小的成员排名为 0
     * 返回 member 的排名，元素不存在则返回null
     */
    public static <T> Long zrank(String key, T member) {
        return defaultExecutor.zrank(key, member);
    }

    /**
     * 返回有序集 key 中成员 member 的排名，其中有序集成员按 score 降序排列
     * 排名以 0 为底， score 值最小的成员排名为 0
     * 返回 member 的排名，元素不存在则返回null
     */
    public static <T> Long zrevrank(String key, T member) {
        return defaultExecutor.zrevrank(key, member);
    }

    /**
     * 返回有序集 key 中元素的数量
     */
    public static Long zcard(String key) {
        return defaultExecutor.zcard(key);
    }

    /**
     * 返回有序集 key 中，成员 member 的 score 值
     */
    public static <T> Double zscore(String key, T member) {
        return defaultExecutor.zscore(key, member);
    }

    /**
     * 返回有序集 key 中， score 值在 min 和 max 之间(默认包括 score 值等于 min 或 max )的成员的数量
     */
    public static Long zcount(String key, double min, double max) {
        return defaultExecutor.zcount(key, min, max);
    }

    /**
     * 返回有序集 key 中， score 值在 min 和 max 之间(默认包括 score 值等于 min 或 max )的成员的数量
     */
    public static Long zcount(String key, String min, String max) {
        return defaultExecutor.zcount(key, min, max);
    }

    /**
     * 返回该集合中， 成员介于 min 和 max 范围内的元素数量
     */
    public static Long zlexcount(String key, String min, String max) {
        return defaultExecutor.zlexcount(key, min, max);
    }


    /********** hyperloglog相关操作 ************/

    /**
     * 将任意数量的元素添加到指定的 HyperLogLog 里面，返回1表示添加成功
     */
    public static <T> Long pfadd(String key, T... elements) {
        return defaultExecutor.pfadd(key, elements);
    }

    /**
     * 将任意数量的元素添加到指定的 HyperLogLog 里面，返回1表示添加成功
     */
    public static <T> Long pfadd(String key, List<T> elements) {
        return defaultExecutor.pfadd(key, elements);
    }

    /**
     * 返回给定 HyperLogLog 包含的唯一元素的近似数量
     */
    public static Long pfcount(String key) {
        return defaultExecutor.pfcount(key);
    }


    /********** bitmap相关操作 ************/
    /**
     * 对 key 所储存的字符串值，设置或清除指定偏移量上的位(bit)，返回指定偏移量原来储存的位
     * @param offset 偏移量
     * @param value 该偏移量原来储存的值，false/true，对应0/1
     */
    public static boolean setbit(String key, long offset, boolean value) {
        return defaultExecutor.setbit(key, offset, value);
    }

    /**
     * 对 key 所储存的字符串值，设置或清除指定偏移量上的位(bit)，返回指定偏移量原来储存的位
     * @param offset 偏移量
     * @param value 该偏移量原来储存的值，0/1
     */
    public static boolean setbit(String key, long offset, String value) {
        return defaultExecutor.setbit(key, offset, value);
    }

    /**
     * 获取位图 key 指定偏移量上的位(bit)，返回字符串值指定偏移量上的位(bit)
     * 当 offset 比字符串值的长度大，或者 key 不存在时，返回 false
     * @param offset 偏移量
     */
    public static boolean getbit(String key, long offset) {
        return defaultExecutor.getbit(key, offset);
    }

    /**
     * 获取位图中所有值为 1 的位的个数
     */
    public static Long bitcount(String key) {
        return defaultExecutor.bitcount(key);
    }

    /**
     * 获取位图中所有值为 1 的位的个数
     * @param start 和end一样表示起始结束位，-1 表示最后一个字节， -2表示倒数第二个字节
     */
    public static Long bitcount(String key, long start, long end) {
        return defaultExecutor.bitcount(key, start, end);
    }

    /**
     * 返回位图中第一个值为 value 的二进制位的位置，返回位置整数，没有查到会返回-1
     */
    public static Long bitpos(String key, boolean value) {
        return defaultExecutor.bitpos(key, value);
    }

    /**
     * 返回位图中第一个值为 value 的二进制位的位置，返回位置整数，没有查到会返回-1
     * start 参数和 end 参数指定要检测的范围
     */
    public static Long bitpos(String key, boolean value, long start, long end) {
        return defaultExecutor.bitpos(key, value, start, end);
    }

    /**
     * 对一个或多个保存二进制位的字符串 key 进行位元操作，并将结果保存到 destkey 上
     * 返回保存到 destkey 的字符串的长度（和输入 key 中最长的字符串长度相等）
     * @param op 操作类型，如下四种
     * AND-求逻辑并，将多个Key的值转化为二进制取 交集，再转化为原数据类型
     * OR-求逻辑或，将多个Key的值转化为二进制取 并集，再转化为原数据类型
     * XOR-求逻辑异或，将多个Key的值转化为二进制取 并集，再执行逻辑非操作，得到相反值，再转化为原数据类型
     * NOT-求逻辑非，只能针对一个Key操作，将Key的值转化为二进制，再将所有比特位的值 反转，0变1，1变0
     * @param destKey 保存的结果Key
     * @param srcKeys 被操作的Key集合
     */
    public static Long bitop(BitOP op, String destKey, String... srcKeys) {
        return defaultExecutor.bitop(op, destKey, srcKeys);
    }

    /**
     * 对一个或多个保存二进制位的字符串 key 进行位元操作，并将结果保存到 destkey 上
     * 返回保存到 destkey 的字符串的长度（和输入 key 中最长的字符串长度相等）
     * @param op 操作类型，如下四种
     * AND-求逻辑并，将多个Key的值转化为二进制取 交集，再转化为原数据类型
     * OR-求逻辑或，将多个Key的值转化为二进制取 并集，再转化为原数据类型
     * XOR-求逻辑异或，将多个Key的值转化为二进制取 并集，再执行逻辑非操作，得到相反值，再转化为原数据类型
     * NOT-求逻辑非，只能针对一个Key操作，将Key的值转化为二进制，再将所有比特位的值 反转，0变1，1变0
     * @param destKey 保存的结果Key
     * @param srcKeys 被操作的Key集合
     */
    public static Long bitop(BitOP op, String destKey, List<String> srcKeys) {
        return defaultExecutor.bitop(op, destKey, srcKeys);
    }

    /**
     * 对多个位范围进行子操作，返回子操作集合的结果列表
     * @param arguments 子操作集合，支持的子命令如下：
     * GET <type> <offset> —— 返回指定的二进制位范围
     * SET <type> <offset> <value> —— 对指定的二进制位范围进行设置，并返回它的旧值
     * INCRBY <type> <offset> <increment> —— 对指定的二进制位范围执行加法操作，并返回它的旧值，传入负值表示减法操作。
     * 操作示例：BITFIELD mykey INCRBY i8 100 1 GET u4 0
     * 该命令实现的作用：对位于偏移量 100 的 8 位长有符号整数执行加法操作， 并获取位于偏移量 0 上的 4 位长无符号整数
     */
    public static List<Long> bitfield(String key, String... arguments) {
        return defaultExecutor.bitfield(key, arguments);
    }

    /**
     * 对多个位范围进行子操作，返回子操作集合的结果列表
     * @param arguments 子操作集合，支持的子命令如下：
     * GET <type> <offset> —— 返回指定的二进制位范围
     * SET <type> <offset> <value> —— 对指定的二进制位范围进行设置，并返回它的旧值
     * INCRBY <type> <offset> <increment> —— 对指定的二进制位范围执行加法操作，并返回它的旧值，传入负值表示减法操作。
     * 操作示例：BITFIELD mykey INCRBY i8 100 1 GET u4 0
     * 该命令实现的作用：对位于偏移量 100 的 8 位长有符号整数执行加法操作， 并获取位于偏移量 0 上的 4 位长无符号整数
     */
    public static List<Long> bitfield(String key, List<String> arguments) {
        return defaultExecutor.bitfield(key, arguments);
    }

    /********** geo相关操作，3.2.0以上版本可用 ************/

    /**
     * 将给定的空间元素（经度、纬度、名字）添加到指定的键里面，返回新添加到键里面的空间元素数量， 不包括那些已经存在但是被更新的元素
     * @param key 键
     * @param longitude 经度
     * @param latitude 纬度
     * @param member 名字
     */
    public static <T> Long geoadd(String key, double longitude, double latitude, T member) {
        return defaultExecutor.geoadd(key, longitude, latitude, member);
    }

    /**
     * 将给定的空间元素（经度、纬度、名字）添加到指定的键里面，返回新添加到键里面的空间元素数量， 不包括那些已经存在但是被更新的元素
     * @param key 键
     * @param memberCoordinateMap 经度、纬度、名字的Map
     */
    public static Long geoadd(String key, Map<String, GeoCoordinate> memberCoordinateMap) {
        return defaultExecutor.geoadd(key, memberCoordinateMap);
    }

    /**
     * 返回两个给定位置之间的距离
     * 如果两个位置之间的其中一个不存在， 那么命令返回空值
     * 距离单位为米
     */
    public static <T> Double geodist(String key, T member1, T member2) {
        return defaultExecutor.geodist(key, member1, member2);
    }

    /**
     * 返回两个给定位置之间的距离
     * 如果两个位置之间的其中一个不存在， 那么命令返回空值
     * 距离单位：M-米 KM-千米 MI-英里 FT-英尺
     */
    public static <T> Double geodist(String key, T member1, T member2, GeoUnit unit) {
        return defaultExecutor.geodist(key, member1, member2, unit);
    }

    /**
     * 返回一个或多个位置元素的 Geohash 值
     */
    public static <T> List<String> geohash(String key, T... members) {
        return defaultExecutor.geohash(key, members);
    }

    /**
     * 返回一个或多个位置元素的 Geohash 值
     */
    public static <T> List<String> geohash(String key, List<T> members) {
        return defaultExecutor.geohash(key, members);
    }

    /**
     * 从键里面返回所有给定位置元素的位置（经度和纬度）
     */
    public static <T> List<GeoCoordinate> geopos(String key, T... members) {
        return defaultExecutor.geopos(key, members);
    }

    /**
     * 从键里面返回所有给定位置元素的位置（经度和纬度）
     */
    public static <T> List<GeoCoordinate> geopos(String key, List<T> members) {
        return defaultExecutor.geopos(key, members);
    }

    /**
     * 以给定的经纬度为中心， 返回键包含的位置元素当中， 与中心的距离不超过给定最大距离的所有位置元素
     * @param key 键
     * @param longitude 中心点经度
     * @param latitude 中心点纬度
     * @param radius 半径
     * @param unit 半径的单位：M-米 KM-千米 MI-英里 FT-英尺
     */
    public static List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit) {
        return defaultExecutor.georadius(key, longitude, latitude, radius, unit);
    }

    /**
     * 以给定的经纬度为中心， 返回键包含的位置元素当中， 与中心的距离不超过给定最大距离的所有位置元素
     * @param key 键
     * @param longitude 中心点经度
     * @param latitude 中心点纬度
     * @param radius 半径
     * @param unit 半径的单位：M-米 KM-千米 MI-英里 FT-英尺
     * @param param 用于指定排序方式以及limit数量
     */
    public static List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
        return defaultExecutor.georadius(key, longitude, latitude, radius, unit, param);
    }

    /**
     * 以给定的中心点名称， 返回键包含的位置元素当中， 与中心的距离不超过给定最大距离的所有位置元素
     * @param key 键
     * @param member 中心点名称
     * @param radius 半径
     * @param unit 半径的单位：M-米 KM-千米 MI-英里 FT-英尺
     */
    public static <T> List<GeoRadiusResponse> georadiusByMember(String key, T member, double radius, GeoUnit unit) {
        return defaultExecutor.georadiusByMember(key, member, radius, unit);
    }

    /**
     * 以给定的中心点名称， 返回键包含的位置元素当中， 与中心的距离不超过给定最大距离的所有位置元素
     * @param key 键
     * @param member 中心点名称
     * @param radius 半径
     * @param unit 半径的单位：M-米 KM-千米 MI-英里 FT-英尺
     * @param param 用于指定排序方式以及limit数量
     */
    public static <T> List<GeoRadiusResponse> georadiusByMember(String key, T member, double radius, GeoUnit unit, GeoRadiusParam param) {
        return defaultExecutor.georadiusByMember(key, member, radius, unit, param);
    }


    /********** 布隆过滤器相关操作 ************/
    /**
     * 添加指定值到BloomFilter中，返回True表示添加成功，返回False表示filter中已经存在该值
     * @param value 值
     */
    public static <T> boolean bloomadd(String key, T value) {
        return defaultExecutor.bloomadd(key, value);
    }

    /**
     * 判断指定值在BloomFilter中是否已经存在，返回True表示存在，返回false表示不存在
     * @param value 值
     */
    public static <T> boolean bloomcons(String key, T value) {
        return defaultExecutor.bloomcons(key, value);
    }


    /********** 分布式锁相关操作 ************/

    /**
     * 获取分布式锁，返回true表示获取成功
     * @param lockKey Key
     * @param requestId requestId
     * @param expireTime 过期时间，单位为毫秒
     */
    public static boolean getDistributedLock(String lockKey, String requestId, int expireTime) {
        return defaultExecutor.getDistributedLock(lockKey, requestId, expireTime);
    }

    /**
     * 释放分布式锁，返回true表示释放成功
     * @param lockKey Key
     * @param requestId requestId
     */
    public static boolean releaseDistributedLock(String lockKey, String requestId) {
        return defaultExecutor.releaseDistributedLock(lockKey, requestId);
    }

    /********** Lua脚本相关操作 ************/

    /**
     * 执行Lua脚本
     * @param script 脚本内容
     * @param keyCount 参数数量
     * @param params 参数值列表
     * @return 脚本设定的返回值
     */
    public static <P, T> T eval(String script, int keyCount, List<P> params, Class<T> type) {
        return defaultExecutor.eval(script, keyCount, params, type);
    }

    /**
     * 执行Lua脚本
     * @param script 脚本内容
     * @param keyCount 参数数量
     * @param params 参数值列表
     * @return 脚本设定的返回值
     */
    public static <P, T> T eval(String script, int keyCount, Class<T> type, P... params) {
        return defaultExecutor.eval(script, keyCount, type, params);
    }

    /**
     * 执行Lua脚本
     * @param script 脚本内容
     * @param keys 参数名列表
     * @param args 参数值列表
     * @return 脚本设定的返回值
     */
    public static <P, T, R> R eval(String script, List<P> keys, List<T> args, Class<R> type) {
        return defaultExecutor.eval(script, keys, args, type);
    }

}
