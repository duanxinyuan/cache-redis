package com.dxy.library.cache.redis.util;

import com.google.common.collect.Lists;
import com.dxy.library.json.jackson.JacksonUtil;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author duanxinyuan
 * 2019/4/15 17:46
 */
public class Serializer {

    public static <T> String serialize(T value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        } else {
            return JacksonUtil.to(value);
        }
    }

    public static <T> String[] serialize(T... values) {
        if (values == null) {
            return null;
        }
        if (values.length > 0 && values[0] instanceof String) {
            return (String[]) values;
        }
        String[] strings = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            strings[i] = JacksonUtil.to(values[i]);
        }
        return strings;
    }

    public static <T> List<String> serialize(List<T> values) {
        if (values == null) {
            return null;
        }
        if (!values.isEmpty() && values.get(0) instanceof String) {
            return (List<String>) values;
        }
        List<String> strings = Lists.newArrayList();
        for (T value : values) {
            strings.add(JacksonUtil.to(value));
        }
        return strings;
    }

    public static <T> List<String> serialize(Map<String, T> map) {
        if (map == null) {
            return null;
        }
        List<String> strings = Lists.newArrayList();
        map.forEach((k, v) -> {
            strings.add(k);
            strings.add(serialize(v));
        });
        return strings;
    }

    public static <T> List<T> deserialize(List<String> values, Class<T> type) {
        if (values == null) {
            return null;
        }
        return values.stream().map(s -> deserialize(s, type)).collect(Collectors.toList());
    }

    public static <T> Set<T> deserialize(Set<String> values, Class<T> type) {
        if (values == null) {
            return null;
        }
        return values.stream().map(s -> deserialize(s, type)).collect(Collectors.toSet());
    }

    public static <T> T deserialize(Object value, Class<T> type) {
        if (value == null) {
            return null;
        }
        if (value.getClass() == type) {
            return (T) value;
        } else {
            if (value instanceof String) {
                return JacksonUtil.from((String) value, type);
            } else {
                return JacksonUtil.from(JacksonUtil.to(value), type);
            }
        }
    }

    public static <T> T deserialize(String value, Class<T> type) {
        if (value == null) {
            return null;
        }
        if (type == String.class) {
            return (T) value;
        } else {
            return JacksonUtil.from(value, type);
        }
    }

}
