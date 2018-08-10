package com.dxy.library.cache.cache.memory;

/**
 * 内存缓存器
 * 不保存中间状态的数据，只保存和Redis同步的数据，只允许增删查，不允许修改
 * @author duanxinyuan
 * 2018/8/9 15:21
 */
public interface IMemory {

    <T> void set(String key, T value);

    <T> T get(String key);

    boolean exist(String key);

    void del(String key);

    void del(String... keys);
}
