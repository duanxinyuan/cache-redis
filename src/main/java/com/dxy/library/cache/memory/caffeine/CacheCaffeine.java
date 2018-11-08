package com.dxy.library.cache.memory.caffeine;

import com.dxy.library.cache.memory.IMemory;
import com.dxy.library.util.common.config.ConfigUtils;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;

/**
 * 基于caffeine的内存缓存器
 * @author duanxinyuan
 * 2018/8/8 19:48
 */
@Slf4j
public class CacheCaffeine implements IMemory {

    private LoadingCache<String, Optional<Object>> cache;

    public CacheCaffeine() {
        cache = Caffeine.newBuilder()
                .initialCapacity(NumberUtils.toInt(ConfigUtils.getConfig("cache.memory.key.capacity.initial"), 1000))
                .maximumSize(NumberUtils.toInt(ConfigUtils.getConfig("cache.memory.key.capacity.max"), 5_0000))
                .expireAfterWrite(NumberUtils.toInt(ConfigUtils.getConfig("cache.memory.expire.seconds.after.write"), 300), TimeUnit.SECONDS)
                .expireAfterAccess(NumberUtils.toInt(ConfigUtils.getConfig("cache.memory.expire.seconds.after.access"), 300), TimeUnit.SECONDS)
                .refreshAfterWrite(NumberUtils.toInt(ConfigUtils.getConfig("cache.memory.refresh.seconds.after.write"), 300), TimeUnit.SECONDS)
                .recordStats()
                .build(new CacheLoader<String, Optional<Object>>() {
                    @Nullable
                    @Override
                    public Optional<Object> load(@Nonnull String key) {
                        return Optional.fromNullable(get(key));
                    }
                });
    }

    @Override
    public <T> void set(String key, T value) {
        if (null == key) {
            return;
        }
        if (StringUtils.isEmpty(key) || value == null) {
            return;
        }
        cache.put(key, Optional.fromNullable(value));
    }

    @Override
    public <T> T get(String key) {
        if (null == key) {
            return null;
        }
        Optional<Object> optional = cache.getIfPresent(key);
        if (optional == null) {
            return null;
        }
        return (T) optional.orNull();
    }

    @Override
    public boolean exist(String key) {
        return get(key) != null;
    }

    @Override
    public void del(String key) {
        if (key == null) {
            return;
        }
        cache.invalidate(key);
    }

    @Override
    public void del(String... keys) {
        if (keys == null) {
            return;
        }
        cache.invalidateAll(Lists.newArrayList(keys));
    }

}
