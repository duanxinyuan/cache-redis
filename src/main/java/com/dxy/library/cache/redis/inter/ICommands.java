package com.dxy.library.cache.redis.inter;

import redis.clients.jedis.*;
import redis.clients.jedis.params.GeoRadiusParam;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.params.ZAddParams;
import redis.clients.jedis.params.ZIncrByParams;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis命令
 * @author duanxinyuan
 * 2018/8/8 17:45
 */
public interface ICommands {

    /**
     * 查找所有符合给定模式 pattern 的 key，返回符合给定模式的 key 列表
     * @param pattern key规则，支持正则
     */
    Set<String> keys(String pattern);

    /**
     * 迭代查找所有符合给定模式 pattern 的 key，返回 count 个符合给定模式的 key 列表
     * @param cursor 光标，从0开始，scan命令会返回一个新的游标，如果新的游标不是0，表示遍历还没有结束，继续遍历需要使用新的游标作为参数，继续输入获得后面的结果
     * @param params pattern key规则，以及迭代的 key 的个数
     */
    ScanResult<String> scan(String cursor, ScanParams params);

    /**
     * 返回 key 所储存的值的类型
     */
    String type(String key);

    /**
     * 以秒为单位，返回给定 key 的剩余生存时间(TTL, time to live)
     * 当 key 不存在时，返回 -2
     * 当 key 存在但没有设置剩余生存时间时，返回 -1
     * 否则，以秒为单位，返回 key 的剩余生存时间
     */
    Long ttl(String key);

    /**
     * 以毫秒为单位，返回给定 key 的剩余生存时间(TTL, time to live)
     * 当 key 不存在时，返回 -2
     * 当 key 存在但没有设置剩余生存时间时，返回 -1
     * 否则，以毫秒为单位，返回 key 的剩余生存时间
     */
    Long pttl(String key);

    /**
     * 设置键过期时间，返回1表示设置成功
     */
    Long expire(String key, int seconds);

    /**
     * 以 UNIX 时间戳(unix timestamp)格式设置 key 的过期时间，返回1表示设置成功
     */
    Long expireAt(String key, long unixTime);

    /**
     * 清除设置的过期时间，将Key设置为永久有效，返回1表示设置成功
     */
    Long persist(String key);

    /**
     * key是否存在，返回true表示成功
     */
    boolean exists(String key);

    /**
     * 删除key，返回删除的 key 的数量
     */
    Long del(String key);

    /**
     * 批量删除key，返回删除的 key 的数量
     */
    Long del(String... keys);

    /**
     * 批量删除key，返回删除的 key 的数量
     */
    Long del(List<String> keys);

    /**
     * 异步删除key，返回删除的 key 的数量
     */
    Long unlink(String key);

    /**
     * 将 oldkey 重命名为 newkey，返回OK表示成功
     * @param oldkey 旧的Key名称
     * @param newkey 新的Key名称
     */
    String rename(String oldkey, String newkey);

    /**
     * 将 oldkey 重命名为 newkey（newkey 不存在才设置，原子方法），返回1表示成功
     * @param oldkey 旧的Key名称
     * @param newkey 新的Key名称
     */
    Long renamenx(String oldkey, String newkey);

    /********** string相关操作 ************/

    /**
     * 为指定的 key 设置值，返回OK表示成功
     */
    <T> String set(String key, T value);

    /**
     * 为指定的 key 设置值，返回OK表示成功
     * @param key 键
     * @param value 值
     * @param setParams set命令的其他参数
     */
    <T> String set(String key, T value, SetParams setParams);

    /**
     * 为指定的 key 设置值（不存在才设置，原子方法），返回1表示成功
     */
    <T> Long setnx(String key, T value);

    /**
     * 为指定的 key 设置值及其过期时间（单位为秒），返回OK表示成功
     */
    <T> String setex(String key, int seconds, T value);

    /**
     * 为指定的 key 设置值及其过期时间（单位为秒），返回OK表示成功
     */
    <T> String setex(String key, long time, TimeUnit timeUnit, T value);

    /**
     * 为指定的 key 设置值及其过期时间（单位为毫秒），返回OK表示成功
     */
    <T> String psetex(String key, long milliseconds, T value);

    /**
     * 为批量的 key 设置值，返回OK表示成功
     */
    <T> String mset(Map<String, T> map);

    /**
     * 返回指定的 key 值
     */
    String get(String key);

    /**
     * 返回指定的 key 值
     */
    <T> T get(String key, Class<T> type);

    /**
     * 批量返回的 key 值
     */
    List<String> mget(String... keys);

    /**
     * 批量返回的 key 值
     */
    List<String> mget(List<String> keys);

    /**
     * 批量返回的 key 值
     */
    <T> List<T> mget(List<String> keys, Class<T> type);

    /**
     * 将存储的数字key加1，返回key增量后的值
     */
    Long incr(String key);

    /**
     * 对数值增加指定值，返回key增量后的值
     */
    Long incrBy(String key, long increment);

    /**
     * 为 key 中所储存的值加上指定的浮点数增量值，执行命令之后 key 的值
     */
    Double incrByFloat(String key, double increment);

    /**
     * 将 key 中储存的数字值减一，返回执行命令之后 key 的值
     */
    Long decr(String key);

    /**
     * 将 key 所储存的值减去指定的减量值，返回减去指定减量值之后key 的值
     */
    Long decrBy(String key, long decrement);

    /**
     * 为指定的 key 追加值，返回追加指定值之后 key 中字符串的长度
     */
    Long append(String key, String value);

    /**
     * 返回键 key 储存的字符串值的长度
     */
    Long strlen(String key);


    /********** hash相关操作 ************/

    /**
     * 添加键值对到map，返回1表示成功
     */
    <P, T> Long hset(String key, P field, T value);

    /**
     * 添加键值对到map，返回OK表示成功
     */
    <P, T> String hmset(String key, Map<P, T> hash);

    /**
     * 只有在字段 field 不存在时，设置哈希表字段的值，返回1表示field是散列中的新字段并且value已设置
     */
    <P, T> Long hsetnx(String key, P field, T value);

    /**
     * 返回哈希表中给定域的值
     */
    <P> String hget(String key, P field);

    /**
     * 返回哈希表中给定域的值
     */
    <P, T> T hget(String key, P field, Class<T> type);

    /**
     * 返回哈希表 key 中一个或多个给定域的值，返回list
     */
    <P> List<String> hmget(String key, P... fields);

    /**
     * 返回哈希表 key 中一个或多个给定域的值，返回list
     */
    <P> List<String> hmget(String key, List<P> fields);

    /**
     * 返回哈希表 key 中一个或多个给定域的值，返回list
     */
    <P, T> List<T> hmget(String key, List<P> fields, Class<T> type);

    /**
     * 为哈希表 key 中的域 field 的值加上增量 increment
     */
    <P> Long hincrBy(String key, P field, long value);

    /**
     * 为哈希表 key 中的域 field 加上浮点数增量，返回执行加法操作之后 field 域的值
     */
    <P> Double hincrByFloat(String key, P field, double value);

    /**
     * 返回哈希表 key 中的所有域
     */
    Set<String> hkeys(String key);

    /**
     * 返回哈希表 key 中所有域的值
     */
    List<String> hvals(String key);

    /**
     * 返回哈希表 key 中所有域的值
     */
    <T> List<T> hvals(String key, Class<T> type);

    /**
     * 返回哈希表 key 中，所有的域和值
     */
    Map<String, String> hgetAll(String key);

    /**
     * 返回哈希表 key 中，所有的域和值
     */
    <T> Map<String, T> hgetAll(String key, Class<T> type);

    /**
     * 从哈希表 key 中迭代查找所有符合给定模式 pattern 的域和值
     * @param cursor 游标名
     */
    ScanResult<Map.Entry<String, String>> hscan(String key, String cursor);

    /**
     * 从哈希表 key 中迭代查找 count 个符合给定模式 pattern 的域和值
     * @param cursor 游标名
     * @param params pattern key规则，以及迭代的 key 的个数
     */
    ScanResult<Map.Entry<String, String>> hscan(String key, String cursor, ScanParams params);

    /**
     * 检查给定域 field 是否存在于哈希表 hash 当中，返回1表示存在
     */
    <P> boolean hexists(String key, P field);

    /**
     * 删除哈希表 key 中的一个或多个指定域，返回被成功移除的域的数量
     */
    <P> Long hdel(String key, P... fields);

    /**
     * 删除哈希表 key 中的一个或多个指定域，返回被成功移除的域的数量
     */
    <P> Long hdel(String key, List<P> fields);

    /**
     * 返回哈希表 key 中， 与给定域 field 相关联的值的字符串长度
     * @return 字符串长度
     */
    <P> Long hstrlen(String key, P field);

    /********** list相关操作 ************/

    /**
     * 将一个或多个值 value 插入到列表 key 的表头，返回命令执行后列表的长度
     */
    <T> Long lpush(String key, T... values);

    /**
     * 将一个或多个值 value 插入到列表 key 的表头，返回命令执行后列表的长度
     */
    <T> Long lpush(String key, List<T> values);

    /**
     * 将一个或多个值 value 插入到列表 key 的表尾(最右边)，返回命令执行后列表的长度
     */
    <T> Long rpush(String key, T... values);

    /**
     * 将一个或多个值 value 插入到列表 key 的表尾(最右边)，返回命令执行后列表的长度
     */
    <T> Long rpush(String key, List<T> values);

    /**
     * 将值 value 插入到列表 key 的表头，当且仅当 key 存在并且是一个列表，返回命令执行后列表的长度
     */
    <T> Long lpushx(String key, T... values);

    /**
     * 将值 value 插入到列表 key 的表头，当且仅当 key 存在并且是一个列表，返回命令执行后列表的长度
     */
    <T> Long lpushx(String key, List<T> values);

    /**
     * 将值 value 插入到列表 key 的表尾，当且仅当 key 存在并且是一个列表，返回命令执行后列表的长度
     */
    <T> Long rpushx(String key, T... values);

    /**
     * 将值 value 插入到列表 key 的表尾，当且仅当 key 存在并且是一个列表，返回命令执行后列表的长度
     */
    <T> Long rpushx(String key, List<T> values);

    /**
     * 将列表 key 下标为 index 的元素的值设置为 value，操作成功返回 ok
     */
    <T> String lset(String key, long index, T value);

    /**
     * 将值 value 插入到列表 key 当中，位于值 pivot 之前或之后，返回返回插入操作完成之后，列表的长度
     */
    <T> Long linsert(String key, ListPosition where, String pivot, T value);

    /**
     * 从 source 队列的右边读取一个值，放到 destination 队列的左边，并返回取到的值
     */
    String rpoplpush(final String srckey, final String dstkey);

    /**
     * 从 source 队列的右边读取一个值，放到 destination 队列的左边，并返回取到的值
     * 等待 timeout 秒，如果超时，返回 null
     */
    String brpoplpush(final String source, final String destination, final int timeout);

    /**
     * 返回列表 key 中指定区间内的元素，起始下标为start，结束下标为end
     * 下标参数 start 和 stop 都以 0 为底，以 0 表示有序集第一个成员，以 -1 表示最后一个成员
     */
    List<String> lrange(String key, long start, long end);

    /**
     * 返回列表 key 中指定区间内的元素，起始下标为start，结束下标为end
     * 下标参数 start 和 stop 都以 0 为底，以 0 表示有序集第一个成员，以 -1 表示最后一个成员
     */
    <T> List<T> lrange(String key, long start, long end, Class<T> type);

    /**
     * 返回列表 key 中指定区间内的元素，起始下标为pageNo*pageSize，结束下标为(pageNo+1)*pageSize
     */
    List<String> lrangePage(String key, int pageNo, int pageSize);

    /**
     * 返回列表 key 中指定区间内的元素，起始下标为pageNo*pageSize，结束下标为(pageNo+1)*pageSize
     */
    <T> List<T> lrangePage(String key, int pageNo, int pageSize, Class<T> type);

    /**
     * 返回列表 key 中的所有元素
     */
    List<String> lrangeAll(String key);

    /**
     * 返回列表 key 中的所有元素
     */
    <T> List<T> lrangeAll(String key, Class<T> type);

    /**
     * 返回列表 key 中，下标为 index 的元素
     */
    String lindex(String key, int index);

    /**
     * 返回列表 key 中，下标为 index 的元素
     */
    <T> T lindex(String key, int index, Class<T> type);

    /**
     * 返回列表 key 的长度
     */
    Long llen(String key);

    /**
     * 移除并返回列表 key 的头元素，返回列表的头元素。 当 key 不存在时，返回 null
     */
    String lpop(String key);

    /**
     * 移除并返回列表 key 的头元素，返回列表的头元素。 当 key 不存在时，返回 null
     */
    <T> T lpop(String key, Class<T> type);

    /**
     * 移除并返回列表 key 的尾元素，返回列表的尾元素。 当 key 不存在时，返回 null
     */
    String rpop(String key);

    /**
     * 移除并返回列表 key 的尾元素，返回列表的尾元素。 当 key 不存在时，返回 null
     */
    <T> T rpop(String key, Class<T> type);

    /**
     * 移除并返回列表 key 的头元素，返回列表的头元素。 如果列表没有元素会阻塞列表直到等待超时或发现可弹出元素为止
     */
    List<String> blpop(int timeout, String key);

    /**
     * 移除并返回列表 key 的尾元素，返回列表的尾元素。 如果列表没有元素会阻塞列表直到等待超时或发现可弹出元素为止
     * @param timeout 超时时间，单位为秒
     */
    List<String> brpop(int timeout, String key);

    /**
     * 根据参数 count 的值，移除列表中与参数 value 相等的元素
     * @param count 表示动作或者查找方向 ，
     * count=0，移除所有匹配的元素
     * count<0，从表尾开始向表头搜索，移除与 value 相等的元素，数量为 count 的绝对值
     * count>0，从表头开始向表尾搜索，移除与 value 相等的元素，数量为 count
     * @param value 匹配的元素
     */
    <T> Long lrem(String key, long count, T value);

    /**
     * 对一个列表进行修剪(trim)，让列表只保留指定区间内的元素，不在指定区间之内的元素都将被删除。返回OK表示修剪成功
     * @param key 键
     * @param start 可以为负数（-1是列表的最后一个元素，-2是列表倒数第二的元素。）
     * 如果start大于end，则返回一个空的列表，即列表被清空
     * @param end 可以为负数（-1是列表的最后一个元素，-2是列表倒数第二的元素。）可以超出索引，不影响结果
     */
    String ltrim(String key, long start, long end);


    /********** set相关操作 ************/

    /**
     * 将一个或多个 member 元素加入到集合 key 当中，返回被添加到集合中的新元素的数量
     */
    <T> Long sadd(String key, T... values);

    /**
     * 将一个或多个 member 元素加入到集合 key 当中，返回被添加到集合中的新元素的数量
     */
    <T> Long sadd(String key, List<T> values);

    /**
     * 返回集合 key 中的所有成员
     */
    Set<String> smembers(String key);

    /**
     * 返回集合 key 中的所有成员
     */
    <T> Set<T> smembers(String key, Class<T> type);

    /**
     * 从集合 key 中迭代查找所有符合给定模式 pattern 的元素
     * @param cursor 游标名
     */
    ScanResult<String> sscan(String key, String cursor);

    /**
     * 从集合 key 中迭代查找所有符合给定模式 pattern 的元素
     * @param cursor 游标名
     */
    <T> ScanResult<T> sscan(String key, String cursor, Class<T> type);

    /**
     * 从集合 key 中迭代查找 count 个符合给定模式 pattern 的元素
     * @param cursor 游标名
     * @param params pattern key规则，以及迭代的 key 的个数
     */
    ScanResult<String> sscan(String key, String cursor, ScanParams params);

    /**
     * 从集合 key 中迭代查找 count 个符合给定模式 pattern 的元素
     * @param cursor 游标名
     * @param params pattern key规则，以及迭代的 key 的个数
     */
    <T> ScanResult<T> sscan(String key, String cursor, ScanParams params, Class<T> type);

    /**
     * 移除集合 key 中的一个或多个 member 元素，返回被成功移除的元素的数量
     */
    <T> Long srem(String key, T... values);

    /**
     * 移除集合 key 中的一个或多个 member 元素，返回被成功移除的元素的数量
     */
    <T> Long srem(String key, List<T> values);

    /**
     * 移除并返回集合 key 中的一个随机元素，返回被移除的随机元素
     */
    String spop(String key);

    /**
     * 移除并返回集合 key 中的一个随机元素，返回被移除的随机元素
     */
    <T> T spop(String key, Class<T> type);

    /**
     * 返回集合 key 中元素的数量
     */
    Long scard(String key);

    /**
     * 判断 member 元素是否是集合 key 的成员，如果 member 元素是集合的成员，返回 1
     */
    <T> boolean sismember(String key, T value);

    /**
     * 返回集合 key 中的一个随机元素
     */
    String srandmember(String key);

    /**
     * 返回集合 key 中的 count 个随机元素
     * count>0，且小于集合基数，那么命令返回一个包含 count 个元素的数组，数组中的元素各不相同。如果 count 大于等于集合基数，那么返回整个集合
     * count<0，那么命令返回一个数组，数组中的元素可能会重复出现多次，而数组的长度为 count 的绝对值
     */
    List<String> srandmember(String key, int count);

    /**
     * 返回集合 key 中的 count 个随机元素
     * count>0，且小于集合基数，那么命令返回一个包含 count 个元素的数组，数组中的元素各不相同。如果 count 大于等于集合基数，那么返回整个集合
     * count<0，那么命令返回一个数组，数组中的元素可能会重复出现多次，而数组的长度为 count 的绝对值
     */
    <T> List<T> srandmember(String key, int count, Class<T> type);

    /********** sortedset相关操作 ************/

    /**
     * 将一个 member 元素及其 score 值加入到有序集 key 当中，返回被成功添加的新成员的数量，不包括那些被更新的、已经存在的成员
     * @param score 排序参数，可以是整数值或双精度浮点数
     */
    <T> Long zadd(String key, double score, T member);

    /**
     * 将一个 member 元素及其 score 值加入到有序集 key 当中，返回被成功添加的新成员的数量，不包括那些被更新的、已经存在的成员
     * @param score 排序参数，可以是整数值或双精度浮点数
     */
    <T> Long zadd(String key, double score, T member, ZAddParams params);

    /**
     * 将多个 member 元素及其 score 值加入到有序集 key 当中，返回被成功添加的新成员的数量，不包括那些被更新的、已经存在的成员
     */
    Long zadd(String key, Map<String, Double> scoreMembers);

    /**
     * 将多个 member 元素及其 score 值加入到有序集 key 当中，返回被成功添加的新成员的数量，不包括那些被更新的、已经存在的成员
     */
    Long zadd(String key, Map<String, Double> scoreMembers, ZAddParams params);

    /**
     * 返回有序集 key 中指定区间内的成员，其中成员的位置按 score 升序排序
     * 起始下标为start，结束下标为end
     * 下标参数 start 和 stop 都以 0 为底，以 0 表示有序集第一个成员，以 -1 表示最后一个成员
     * 返回指定区间内，带有 score 值(可选)的有序集成员的列表
     */
    Set<String> zrange(String key, long start, long stop);

    /**
     * 返回有序集 key 中指定区间内的成员，其中成员的位置按 score 升序排序
     * 起始下标为start，结束下标为end
     * 下标参数 start 和 stop 都以 0 为底，以 0 表示有序集第一个成员，以 -1 表示最后一个成员
     * 返回指定区间内，带有 score 值(可选)的有序集成员的列表
     */
    <T> Set<T> zrange(String key, long start, long stop, Class<T> type);

    /**
     * 返回有序集 key 中指定区间内的成员，其中成员的位置按 score 降序排列
     * 起始下标为start，结束下标为end
     * 下标参数 start 和 stop 都以 0 为底，以 0 表示有序集第一个成员，以 -1 表示最后一个成员
     * 返回指定区间内，带有 score 值(可选)的有序集成员的列表
     */
    Set<String> zrevrange(String key, long start, long stop);

    /**
     * 返回有序集 key 中指定区间内的成员，其中成员的位置按 score 降序排列
     * 起始下标为start，结束下标为end
     * 下标参数 start 和 stop 都以 0 为底，以 0 表示有序集第一个成员，以 -1 表示最后一个成员
     * 返回指定区间内，带有 score 值(可选)的有序集成员的列表
     */
    <T> Set<T> zrevrange(String key, long start, long stop, Class<T> type);

    /**
     * 移除有序集 key 中的一个或多个成员，返回被成功移除的成员的数量
     */
    <T> Long zrem(String key, T... members);

    /**
     * 移除有序集 key 中的一个或多个成员，返回被成功移除的成员的数量
     */
    <T> Long zrem(String key, List<T> members);

    /**
     * 为有序集 key 的成员 member 的 score 值加上增量 increment，返回member 成员的新 score 值
     */
    <T> Double zincrby(String key, double increment, T member);

    /**
     * 为有序集 key 的成员 member 的 score 值加上增量 increment，返回member 成员的新 score 值
     */
    <T> Double zincrby(String key, double increment, T member, ZIncrByParams params);

    /**
     * 返回有序集 key 中， score 值介于 max 和 min 之间(默认包括等于 max 或 min )的所有的成员
     * 有序集成员按 score 值升序排列
     */
    Set<String> zrangeByScore(String key, double min, double max);

    /**
     * 返回有序集 key 中， score 值介于 max 和 min 之间(默认包括等于 max 或 min )的所有的成员
     * 有序集成员按 score 值升序排列
     */
    <T> Set<T> zrangeByScore(String key, double min, double max, Class<T> type);

    /**
     * 返回有序集 key 中， score 值介于 max 和 min 之间(默认包括等于 max 或 min )的所有的成员
     * 有序集成员按 score 值升序排列
     */
    Set<String> zrangeByScore(String key, String min, String max);

    /**
     * 返回有序集 key 中， score 值介于 max 和 min 之间(默认包括等于 max 或 min )的所有的成员
     * 有序集成员按 score 值升序排列
     */
    <T> Set<T> zrangeByScore(String key, String min, String max, Class<T> type);

    /**
     * 返回有序集 key 中， score 值介于 max 和 min 之间(默认包括等于 max 或 min )的所有的成员及其分数
     * 有序集成员按 score 值升序排列
     */
    Set<Tuple> zrangeByScoreWithScores(String key, double min, double max);

    /**
     * 返回有序集 key 中， score 值介于 max 和 min 之间(默认包括等于 max 或 min )的所有的成员及其分数
     * 有序集成员按 score 值升序排列
     * 指定返回下标和数量
     */
    Set<Tuple> zrangeByScoreWithScores(String key, double min, double max, int offset, int count);

    /**
     * 返回有序集 key 中， score 值介于 max 和 min 之间(默认包括等于 max 或 min )的所有的成员
     * 有序集成员按 score 值降序排列
     */
    Set<String> zrevrangeByScore(String key, double max, double min);

    /**
     * 返回有序集 key 中， score 值介于 max 和 min 之间(默认包括等于 max 或 min )的所有的成员
     * 有序集成员按 score 值降序排列
     */
    <T> Set<T> zrevrangeByScore(String key, double max, double min, Class<T> type);

    /**
     * 返回有序集 key 中， score 值介于 max 和 min 之间(默认包括等于 max 或 min )的所有的成员
     * 有序集成员按 score 值降序排列
     */
    Set<String> zrevrangeByScore(String key, String max, String min);

    /**
     * 返回有序集 key 中， score 值介于 max 和 min 之间(默认包括等于 max 或 min )的所有的成员
     * 有序集成员按 score 值降序排列
     */
    <T> Set<T> zrevrangeByScore(String key, String max, String min, Class<T> type);

    /**
     * 返回给定的有序集合键 key 中， 值介于 min 和 max 之间的成员
     * 有序集成员按 score 值升序排列
     * 合法的 min 和 max 参数必须包含 ( 或者 [ ， 其中 ( 表示开区间（指定的值不会被包含在范围之内）， 而 [ 则表示闭区间（指定的值会被包含在范围之内）
     * 特殊值 + 和 - 在 min 参数以及 max 参数中具有特殊的意义， 其中 + 表示正无限， 而 - 表示负无限。
     * 因此， 向一个所有成员的分值都相同的有序集合发送命令 ZRANGEBYLEX <zset> - + ， 命令将返回有序集合中的所有元素
     */
    Set<String> zrangeByLex(String key, String min, String max);

    /**
     * 返回给定的有序集合键 key 中， 值介于 min 和 max 之间的成员
     * 有序集成员按 score 值升序排列
     * 合法的 min 和 max 参数必须包含 ( 或者 [ ， 其中 ( 表示开区间（指定的值不会被包含在范围之内）， 而 [ 则表示闭区间（指定的值会被包含在范围之内）
     * 特殊值 + 和 - 在 min 参数以及 max 参数中具有特殊的意义， 其中 + 表示正无限， 而 - 表示负无限。
     * 因此， 向一个所有成员的分值都相同的有序集合发送命令 ZRANGEBYLEX <zset> - + ， 命令将返回有序集合中的所有元素
     */
    <T> Set<T> zrangeByLex(String key, String min, String max, Class<T> type);

    /**
     * 返回给定的有序集合键 key 中， 值介于 min 和 max 之间的成员
     * 有序集成员按 score 值降序排列
     * 合法的 min 和 max 参数必须包含 ( 或者 [ ， 其中 ( 表示开区间（指定的值不会被包含在范围之内）， 而 [ 则表示闭区间（指定的值会被包含在范围之内）
     * 特殊值 + 和 - 在 min 参数以及 max 参数中具有特殊的意义， 其中 + 表示正无限， 而 - 表示负无限。
     * 因此， 向一个所有成员的分值都相同的有序集合发送命令 ZRANGEBYLEX <zset> - + ， 命令将返回有序集合中的所有元素
     */
    Set<String> zrevrangeByLex(String key, String max, String min);

    /**
     * 返回给定的有序集合键 key 中， 值介于 min 和 max 之间的成员
     * 有序集成员按 score 值降序排列
     * 合法的 min 和 max 参数必须包含 ( 或者 [ ， 其中 ( 表示开区间（指定的值不会被包含在范围之内）， 而 [ 则表示闭区间（指定的值会被包含在范围之内）
     * 特殊值 + 和 - 在 min 参数以及 max 参数中具有特殊的意义， 其中 + 表示正无限， 而 - 表示负无限。
     * 因此， 向一个所有成员的分值都相同的有序集合发送命令 ZRANGEBYLEX <zset> - + ， 命令将返回有序集合中的所有元素
     */
    <T> Set<T> zrevrangeByLex(String key, String max, String min, Class<T> type);

    /**
     * 从有序集 key 中迭代查找所有符合给定模式 pattern 的元素
     * @param cursor 游标名
     */
    ScanResult<Tuple> zscan(String key, String cursor);

    /**
     * 从有序集 key 中迭代查找 count 个符合给定模式 pattern 的元素
     * @param cursor 游标名
     * @param params pattern key规则，以及迭代的 key 的个数
     */
    ScanResult<Tuple> zscan(String key, String cursor, ScanParams params);

    /**
     * 移除有序集 key 中，指定排名(rank)区间内的所有成员，返回被移除成员的数量
     */
    Long zremrangeByRank(String key, long start, long stop);

    /**
     * 移除有序集 key 中，所有 score 值介于 min 和 max 之间(包括等于 min 或 max )的成员，返回被移除成员的数量
     */
    Long zremrangeByScore(String key, double min, double max);

    /**
     * 移除有序集 key 中，所有 score 值介于 min 和 max 之间(包括等于 min 或 max )的成员，返回被移除成员的数量
     */
    Long zremrangeByScore(String key, String min, String max);

    /**
     * 移除该集合中， 成员介于 min 和 max 范围内的所有元素，返回被移除成员的数量
     */
    Long zremrangeByLex(String key, String min, String max);

    /**
     * 返回有序集 key 中成员 member 的排名，其中有序集成员按 score 升序排列
     * 排名以 0 为底， score 值最小的成员排名为 0
     * 返回 member 的排名，元素不存在则返回null
     */
    <T> Long zrank(String key, T member);

    /**
     * 返回有序集 key 中成员 member 的排名，其中有序集成员按 score 降序排列
     * 排名以 0 为底， score 值最小的成员排名为 0
     * 返回 member 的排名，元素不存在则返回null
     */
    <T> Long zrevrank(String key, T member);

    /**
     * 返回有序集 key 中元素的数量
     */
    Long zcard(String key);

    /**
     * 返回有序集 key 中，成员 member 的 score 值
     */
    <T> Double zscore(String key, T member);

    /**
     * 返回有序集 key 中， score 值在 min 和 max 之间(默认包括 score 值等于 min 或 max )的成员的数量
     */
    Long zcount(String key, double min, double max);

    /**
     * 返回有序集 key 中， score 值在 min 和 max 之间(默认包括 score 值等于 min 或 max )的成员的数量
     */
    Long zcount(String key, String min, String max);

    /**
     * 返回该集合中， 成员介于 min 和 max 范围内的元素数量
     */
    Long zlexcount(String key, String min, String max);

    /********** hyperloglog相关操作 ************/

    /**
     * 将任意数量的元素添加到指定的 HyperLogLog 里面，返回1表示添加成功
     */
    <T> Long pfadd(String key, T... elements);

    /**
     * 将任意数量的元素添加到指定的 HyperLogLog 里面，返回1表示添加成功
     */
    <T> Long pfadd(String key, List<T> elements);

    /**
     * 返回给定 HyperLogLog 包含的唯一元素的近似数量
     */
    Long pfcount(String key);

    /********** bitmap相关操作 ************/
    /**
     * 对 key 所储存的字符串值，设置或清除指定偏移量上的位(bit)，返回指定偏移量原来储存的位
     * @param offset 偏移量
     * @param value 该偏移量原来储存的值，false/true，对应0/1
     */
    boolean setbit(String key, long offset, boolean value);

    /**
     * 对 key 所储存的字符串值，设置或清除指定偏移量上的位(bit)，返回指定偏移量原来储存的位
     * @param offset 偏移量
     * @param value 该偏移量原来储存的值，0/1
     */
    boolean setbit(String key, long offset, String value);

    /**
     * 获取位图 key 指定偏移量上的位(bit)，返回字符串值指定偏移量上的位(bit)
     * 当 offset 比字符串值的长度大，或者 key 不存在时，返回 false
     * @param offset 偏移量
     */
    boolean getbit(String key, long offset);

    /**
     * 获取位图中所有值为 1 的位的个数
     */
    Long bitcount(String key);

    /**
     * 获取位图中所有值为 1 的位的个数
     * @param start 和end一样表示起始结束位，-1 表示最后一个字节， -2表示倒数第二个字节
     */
    Long bitcount(String key, long start, long end);

    /**
     * 返回位图中第一个值为 value 的二进制位的位置，返回位置整数，没有查到会返回-1
     */
    Long bitpos(String key, boolean value);

    /**
     * 返回位图中第一个值为 value 的二进制位的位置，返回位置整数，没有查到会返回-1
     * start 参数和 end 参数指定要检测的范围
     */
    Long bitpos(String key, boolean value, long start, long end);

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
    Long bitop(BitOP op, String destKey, String... srcKeys);

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
    Long bitop(BitOP op, String destKey, List<String> srcKeys);

    /**
     * 对多个位范围进行子操作，返回子操作集合的结果列表
     * @param arguments 子操作集合，支持的子命令如下：
     * GET <type> <offset> —— 返回指定的二进制位范围
     * SET <type> <offset> <value> —— 对指定的二进制位范围进行设置，并返回它的旧值
     * INCRBY <type> <offset> <increment> —— 对指定的二进制位范围执行加法操作，并返回它的旧值，传入负值表示减法操作。
     * 操作示例：BITFIELD mykey INCRBY i8 100 1 GET u4 0
     * 该命令实现的作用：对位于偏移量 100 的 8 位长有符号整数执行加法操作， 并获取位于偏移量 0 上的 4 位长无符号整数
     */
    List<Long> bitfield(String key, String... arguments);

    /**
     * 对多个位范围进行子操作，返回子操作集合的结果列表
     * @param arguments 子操作集合，支持的子命令如下：
     * GET <type> <offset> —— 返回指定的二进制位范围
     * SET <type> <offset> <value> —— 对指定的二进制位范围进行设置，并返回它的旧值
     * INCRBY <type> <offset> <increment> —— 对指定的二进制位范围执行加法操作，并返回它的旧值，传入负值表示减法操作。
     * 操作示例：BITFIELD mykey INCRBY i8 100 1 GET u4 0
     * 该命令实现的作用：对位于偏移量 100 的 8 位长有符号整数执行加法操作， 并获取位于偏移量 0 上的 4 位长无符号整数
     */
    List<Long> bitfield(String key, List<String> arguments);

    /********** geo相关操作，3.2.0以上版本可用 ************/

    /**
     * 将给定的空间元素（经度、纬度、名字）添加到指定的键里面，返回新添加到键里面的空间元素数量， 不包括那些已经存在但是被更新的元素
     * @param key 键
     * @param longitude 经度
     * @param latitude 纬度
     * @param member 名字
     */
    <T> Long geoadd(String key, double longitude, double latitude, T member);

    /**
     * 将给定的空间元素（经度、纬度、名字）添加到指定的键里面，返回新添加到键里面的空间元素数量， 不包括那些已经存在但是被更新的元素
     * @param key 键
     * @param memberCoordinateMap 经度、纬度、名字的Map
     */
    Long geoadd(String key, Map<String, GeoCoordinate> memberCoordinateMap);

    /**
     * 返回两个给定位置之间的距离
     * 如果两个位置之间的其中一个不存在， 那么命令返回空值
     * 距离单位为米
     */
    <T> Double geodist(String key, T member1, T member2);

    /**
     * 返回两个给定位置之间的距离
     * 如果两个位置之间的其中一个不存在， 那么命令返回空值
     * 距离单位：M-米 KM-千米 MI-英里 FT-英尺
     */
    <T> Double geodist(String key, T member1, T member2, GeoUnit unit);

    /**
     * 返回一个或多个位置元素的 Geohash 值
     */
    <T> List<String> geohash(String key, T... members);

    /**
     * 返回一个或多个位置元素的 Geohash 值
     */
    <T> List<String> geohash(String key, List<T> members);

    /**
     * 从键里面返回所有给定位置元素的位置（经度和纬度）
     */
    <T> List<GeoCoordinate> geopos(String key, T... members);

    /**
     * 从键里面返回所有给定位置元素的位置（经度和纬度）
     */
    <T> List<GeoCoordinate> geopos(String key, List<T> members);

    /**
     * 以给定的经纬度为中心， 返回键包含的位置元素当中， 与中心的距离不超过给定最大距离的所有位置元素
     * @param key 键
     * @param longitude 中心点经度
     * @param latitude 中心点纬度
     * @param radius 半径
     * @param unit 半径的单位：M-米 KM-千米 MI-英里 FT-英尺
     */
    List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit);

    /**
     * 以给定的经纬度为中心， 返回键包含的位置元素当中， 与中心的距离不超过给定最大距离的所有位置元素
     * @param key 键
     * @param longitude 中心点经度
     * @param latitude 中心点纬度
     * @param radius 半径
     * @param unit 半径的单位：M-米 KM-千米 MI-英里 FT-英尺
     * @param param 用于指定排序方式以及limit数量
     */
    List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param);

    /**
     * 以给定的中心点名称， 返回键包含的位置元素当中， 与中心的距离不超过给定最大距离的所有位置元素
     * @param key 键
     * @param member 中心点名称
     * @param radius 半径
     * @param unit 半径的单位：M-米 KM-千米 MI-英里 FT-英尺
     */
    <T> List<GeoRadiusResponse> georadiusByMember(String key, T member, double radius, GeoUnit unit);

    /**
     * 以给定的中心点名称， 返回键包含的位置元素当中， 与中心的距离不超过给定最大距离的所有位置元素
     * @param key 键
     * @param member 中心点名称
     * @param radius 半径
     * @param unit 半径的单位：M-米 KM-千米 MI-英里 FT-英尺
     * @param param 用于指定排序方式以及limit数量
     */
    <T> List<GeoRadiusResponse> georadiusByMember(String key, T member, double radius, GeoUnit unit, GeoRadiusParam param);

    /********** 布隆过滤器相关操作 ************/
    /**
     * 添加指定值到BloomFilter中，返回True表示添加成功，返回False表示filter中已经存在该值
     * @param value 值
     */
    <T> boolean bloomadd(String key, T value);

    /**
     * 判断指定值在BloomFilter中是否已经存在，返回True表示存在，返回false表示不存在
     * @param value 值
     */
    <T> boolean bloomcons(String key, T value);

    /********** 分布式锁相关操作 ************/

    /**
     * 获取分布式锁，返回true表示获取成功
     * @param lockKey Key
     * @param requestId requestId
     * @param expireTime 过期时间，单位为毫秒
     */
    boolean getDistributedLock(String lockKey, String requestId, int expireTime);

    /**
     * 释放分布式锁，返回true表示释放成功
     * @param lockKey Key
     * @param requestId requestId
     */
    boolean releaseDistributedLock(String lockKey, String requestId);

    /********** Lua脚本相关操作 ************/

    /**
     * 执行Lua脚本
     * @param script 脚本内容
     * @param keyCount 参数数量
     * @param params 参数值列表
     * @return 脚本设定的返回值
     */
    <P, T> T eval(String script, int keyCount, List<P> params, Class<T> type);

    /**
     * 执行Lua脚本
     * @param script 脚本内容
     * @param keyCount 参数数量
     * @param params 参数值列表
     * @return 脚本设定的返回值
     */
    <P, T> T eval(String script, int keyCount, Class<T> type, P... params);

    /**
     * 执行Lua脚本
     * @param script 脚本内容
     * @param keys 参数名列表
     * @param args 参数值列表
     * @return 脚本设定的返回值
     */
    <P, T, R> R eval(String script, List<P> keys, List<T> args, Class<R> type);
}
