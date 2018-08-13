package com.dxy.library.cache.cache.redis;

import com.google.gson.reflect.TypeToken;
import redis.clients.jedis.BitOP;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Redis缓存器
 * @author duanxinyuan
 * 2018/8/8 17:45
 */
public interface IRedis {

    /**
     * 设置键值对，返回OK表示成功
     */
    <T> String set(String key, T value);

    /**
     * 设置键值对和过期时间，返回OK表示成功
     */
    <T> String set(String key, T value, int seconds);

    /**
     * 设置键值对（不存在才设置，原子方法），返回1表示成功
     */
    <T> Long setnx(String key, T value);

    /**
     * 设置键值对和过期时间（不存在才设置，原子方法），返回1表示成功
     */
    <T> Long setnx(String key, T value, int seconds);

    /**
     * 获取值
     */
    String get(String key);

    /**
     * 获取值
     */
    <T> T get(String key, Class<T> c);

    /**
     * 获取值
     */
    <T> T get(String key, TypeToken<T> typeToken);

    /**
     * 对数值增加指定值，返回修改后的数值
     */
    Long incr(String key, Integer value);

    /**
     * 对数值增加指定值，并设置过期时间，返回修改后的数值
     */
    Long incr(String key, Integer value, int seconds);

    /**
     * 对数值减少指定的值，返回修改后的数值
     */
    Long decr(String key, Integer value);

    /**
     * 对数值减少指定值，并设置过期时间，返回修改后的数值
     */
    Long decr(String key, Integer value, int seconds);

    /**
     * 设置键过期时间，返回1表示设置成功
     */
    Long expire(String key, int seconds);

    /**
     * 清除设置的过期时间，将Key设置为永久有效，返回1表示设置成功
     */
    Long persist(String key);

    /**
     * key是否存在，返回true表示成功
     */
    boolean exist(String key);

    /**
     * 删除key，返回1表示设置成功
     */
    Long del(String key);

    /**
     * 批量删除key
     */
    void del(String... keys);

    /********** 一下为list相关操作 ************/

    /**
     * 添加元素到list的头部，value可重复，返回list的长度
     */
    <T> Long lpush(String key, T value);

    /**
     * 添加元素到list的头部，value可重复，返回list的长度
     */
    <T> Long lpush(String key, T value, int seconds);

    /**
     * 添加元素到list的头部，value可重复，返回list的长度
     */
    <T> Long lpush(String key, List<T> values);

    /**
     * 添加元素到list的头部，value可重复，返回list的长度
     */
    <T> Long lpush(String key, List<T> values, int seconds);

    /**
     * 添加元素到list的尾部，value可重复，返回list的长度
     */
    <T> Long rpush(String key, T value);

    /**
     * 添加元素到list的尾部，value可重复，返回list的长度
     */
    <T> Long rpush(String key, T value, int seconds);

    /**
     * 添加元素到list的尾部，value可重复，返回list的长度
     */
    <T> Long rpush(String key, List<T> values);

    /**
     * 添加元素到list的尾部，value可重复，返回list的长度
     */
    <T> Long rpush(String key, List<T> values, int seconds);

    /**
     * 获取整个list
     */
    List<String> lrange(String key);

    /**
     * 获取整个list
     */
    <T> List<T> lrange(String key, Class<T> c);

    /**
     * 获取list中n个元素，起始下标为0，结束下标为end
     */
    List<String> lrange(String key, long end);

    /**
     * 获取list中n个元素，起始下标为0，结束下标为end
     */
    <T> List<T> lrange(String key, long end, Class<T> c);

    /**
     * 获取list中n个元素，起始下标为start，结束下标为end
     */
    List<String> lrange(String key, long start, long end);

    /**
     * 获取list中n个元素，起始下标为start，结束下标为end
     */
    <T> List<T> lrange(String key, long start, long end, Class<T> c);

    /**
     * 获取list中n个元素，起始下标为pageNo*pageSize，结束下标为(pageNo+1)*pageSize
     */
    List<String> lrangePage(String key, int pageNo, int pageSize);

    /**
     * 获取list中n个元素，起始下标为pageNo*pageSize，结束下标为(pageNo+1)*pageSize
     */
    <T> List<T> lrangePage(String key, int pageNo, int pageSize, Class<T> c);

    /**
     * 获取list中第index个元素
     */
    String lindex(String key, int index);

    /**
     * 获取list中第index个元素
     */
    <T> T lindex(String key, int index, Class<T> c);

    /**
     * 获取list长度
     */
    Long llen(String key);

    /**
     * 清空整个List
     */
    void lclear(String key);

    /**
     * 移除列表元素，返回移除的元素数量
     * @param value 匹配的元素
     * @return Long
     */
    Long lrem(String key, String value);

    /**
     * 移除列表元素，返回移除的元素数量
     * @param value 匹配的元素
     */
    <T> Long lrem(String key, T value);

    /**
     * 移除列表元素，返回移除的元素数量
     * @param count 标识，表示动作或者查找方向 ，当count=0时，移除所有匹配的元素
     * 当count为负数时，移除方向是从尾到头
     * 当count为正数时，移除方向是从头到尾
     * @param value 匹配的元素
     */
    Long lrem(String key, long count, String value);

    /**
     * 移除列表元素，返回移除的元素数量
     * @param key 键
     * @param count 标识，表示动作或者查找方向，当count=0时，移除所有匹配的元素
     * 当count为负数时，移除方向是从尾到头
     * 当count为正数时，移除方向是从头到尾
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

    /**
     * 移除并获取列表的第一个元素，当列表不存在或者为空时，返回null
     */
    String lpop(String key);

    /**
     * 移除并获取列表最后一个元素，当列表不存在或者为空时，返回null
     */
    String rpop(String key);


    /********** 一下为set相关操作 ************/

    /**
     * 添加元素到set的尾部，返回1表示成功
     */
    Long sadd(String key, String... values);

    /**
     * 向set中添加value，返回1表示成功
     */
    Long sadd(String key, int seconds, String... values);

    /**
     * 遍历获取set中是否存在某元素，返回true表示存在
     */
    boolean sismember(String key, String value);

    /**
     * 获取整个set
     */
    Set<String> smembers(String key);


    /********** 一下为map相关操作 ************/

    /**
     * 添加键值对到map，返回1表示成功
     */
    <T> Long hset(String key, String field, T value);

    /**
     * 添加键值对到map，返回OK表示成功
     */
    String hmset(String key, String... values);

    /**
     * 添加键值对到map，返回1表示成功
     */
    <T> Long hset(String key, String field, T value, int seconds);

    /**
     * 添加键值对到map，返回OK表示成功
     */
    String hmset(String key, int seconds, String... values);

    /**
     * 获取map中的某个键
     */
    String hget(String key, String field);

    /**
     * 增加map的某个数值
     */
    Long hincr(String key, String field, Integer value);

    /**
     * 减少map的某个数值
     */
    Long hdecr(String key, String field, Integer value);

    /**
     * 获取整个map
     */
    Map<String, String> hgetAll(String key);

    /********** 一下为hyperloglog相关操作 ************/

    /**
     * 使用hyperloglog算法添加值，值不能重复，返回1表示添加成功
     */
    Long pfadd(String key, String value);

    /**
     * 使用hyperloglog算法添加值，值不能重复，返回1表示添加成功
     */
    Long pfadd(String key, String value, int seconds);

    /**
     * 获取使用hyperloglog算法的元素数量
     */
    Long pfcount(String key);

    /********** 一下为Bitmap相关操作 ************/
    /**
     * 使用Bitmap算法设置值，返回修改之前该偏移量所在位置的值
     * @param offset 偏移量
     * @param value 该偏移量所在位置的值，false/true，对应0/1
     */
    boolean setbit(String key, long offset, boolean value);

    /**
     * 使用Bitmap算法设置值，返回修改之前该偏移量所在位置的值
     * @param offset 偏移量
     * @param value 该偏移量所在位置的值，存入bit中会转化为二进制
     */
    boolean setbit(String key, long offset, String value);

    /**
     * 获取Bitmap某个位置的值，返回true/false，对应1/0，
     * 当 offset 比字符串值的长度大，或者 key 不存在时，返回 false
     * @param offset 偏移量
     */
    boolean getbit(String key, long offset);

    /**
     * 获取Bitmap中所有值为 1 的位的个数
     */
    Long bitcount(String key);

    /**
     * 获取Bitmap中所有值为 1 的位的个数
     * @param start 和end一样表示起始结束位，-1 表示最后一个字节， -2表示倒数第二个字节
     */
    Long bitcount(String key, long start, long end);

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
     * 获取string的二进制中第一个0或1的位置
     * 如果指定了查询区间，无论查询0或是1，在没查询到的时候只会返回-1。
     * 在没有指定查询区间时，查询bit位为1的位置时，如果string中没有该位，则会返回-1，表示未查询到
     * @param value 值，false/true，对应0/1
     */
    Long bitpos(String key, boolean value);

    /**
     * 获取string的二进制中第一个0或1的位置
     * 如果指定了查询区间，无论查询0或是1，在没查询到的时候只会返回-1。
     * 在没有指定查询区间时，查询bit位为1的位置时，如果string中没有该位，则会返回-1，表示未查询到
     * @param value 值，false/true，对应0/1
     */
    Long bitpos(String key, boolean value, long start, long end);

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


    /********** 一下为分布式锁相关操作 ************/

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
}
