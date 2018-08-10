package com.dxy.library.cache.cache.redis;

import com.google.gson.reflect.TypeToken;

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
