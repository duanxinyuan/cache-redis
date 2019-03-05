package com.dxy.library.cache.redis.util;

import com.dxy.library.json.gson.GsonUtil;
import redis.clients.util.MurmurHash;

import java.nio.charset.StandardCharsets;

/**
 * Bitmap的Hash工具类
 * @author duanxinyuan
 * 2018/8/13 16:47
 */
public class BitHashUtil {

    //Redis的Bitmap最大比特位为2的32次方，占用空间512M
    private static long MAX_BIT_COUNT = (long) Math.pow(2, 32);

    /**
     * 使用Redis的MurmurHash进行多次Hash获取bit的offset值
     * 2的32次方的Bitmap，8次Hash，错误率在万分之5以下，大约可以对4亿左右的32位字符串去重，对2亿左右的64位字符串去重
     * @param value Hash的值
     */
    public static <T> long[] getBitOffsets(T value) {
        return murmurHash(value, 8, MAX_BIT_COUNT);
    }


    /**
     * 使用Redis的MurmurHash进行多次Hash获取bit的offset值
     * @param value Hash的值
     * @param hashFunctionCount Hash次数
     * @param maxBitCount 最大比特位
     */
    public static <T> long[] murmurHash(T value, int hashFunctionCount, long maxBitCount) {
        long[] offsets = new long[hashFunctionCount];
        byte[] bytes;
        if (value instanceof String) {
            bytes = ((String) value).getBytes(StandardCharsets.UTF_8);
        } else {
            bytes = GsonUtil.to(value).getBytes(StandardCharsets.UTF_8);
        }
        int hash1 = MurmurHash.hash(bytes, 0);
        int hash2 = MurmurHash.hash(bytes, hash1);
        for (int i = 0; i < hashFunctionCount; ++i) {
            offsets[i] = Math.abs((hash1 + i * hash2) % maxBitCount);
        }
        return offsets;
    }

}
