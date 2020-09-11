import com.dxy.library.cache.redis.RedisCache;
import com.dxy.library.cache.redis.inter.RedisConsumer;
import com.dxy.library.cache.redis.inter.RedisFunction;
import com.dxy.library.json.jackson.JacksonUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.math.NumberUtils;
import org.junit.Assert;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author duanxinyuan
 * 2018/8/9 20:04
 */
public class RedisCacheTest {
    public int timeout = 60;

    @Test
    public void testAll() {
        testNameString();
        testString();
        testNx();
        testNumber();
        testList();
        testSet();
        testMap();
        testPf();
        testBit();
        testBloom();
    }


    @Test
    public void testPipeline() {
        String name = "abc";
        RedisCache.single().execute((RedisFunction<Jedis, Void>) jedis -> {
            Pipeline pipelined = jedis.pipelined();
            System.out.println("set: " + pipelined.set("test:dxy", "123456"));
            System.out.println("set: " + pipelined.set("test:dxy1", "789"));
            List<Object> objects = pipelined.syncAndReturnAll();
            System.out.println(objects);
            pipelined.close();
            return null;
        });

        RedisCache.single().executeVoid(jedis -> {
            Pipeline pipelined = jedis.pipelined();
            System.out.println("set: " + pipelined.set("test:dxy", "123456"));
            System.out.println("set: " + pipelined.set("test:dxy1", "789"));
            List<Object> objects = pipelined.syncAndReturnAll();
            System.out.println(objects);
            pipelined.close();
        });
        RedisCache.executeVoid((RedisConsumer<Jedis>) jedis -> {
            Pipeline pipelined = jedis.pipelined();
            System.out.println("set: " + pipelined.set("test:dxy", "123456"));
            System.out.println("set: " + pipelined.set("test:dxy1", "789"));
            List<Object> objects = pipelined.syncAndReturnAll();
            System.out.println(objects);
            pipelined.close();
        });
    }

    @Test
    public void testMsetAndMget() {
        String name = "abc";
        HashMap<String, String> map = Maps.newHashMap();
        map.put("test:dxy1", "1");
        map.put("test:dxy2", "2");
        map.put("test:dxy3", "3");
        map.put("test:dxy4", "4");

        ArrayList<String> keys = Lists.newArrayList(map.keySet());

        ArrayList<String> values = Lists.newArrayList(map.values());

        String mset = RedisCache.name(name).mset(map);
        Assert.assertEquals("OK", mset);
        System.out.println("mset: " + mset);
        List<String> mget = RedisCache.name(name).mget(keys);
        System.out.println("mget: " + mget);
        values.forEach(s -> Assert.assertTrue(mget.contains(s)));
        List<Integer> mget1 = RedisCache.name(name).mget(keys, Integer.class);
        System.out.println("mget: " + mget1);
        values.forEach(s -> Assert.assertTrue(mget1.contains(NumberUtils.toInt(s))));

        RedisCache.name(name).del(keys);
    }


    @Test
    public void testNameString() {
        String name = "abc";
        String key = "test:dxy";
        String setex = RedisCache.name(name).setex(key, timeout, "123456");
        Assert.assertEquals("OK", setex);
        System.out.println("setex: " + setex);
        String get = RedisCache.name(name).get(key);
        Assert.assertEquals("123456", get);
        System.out.println("get: " + get);
        String type = RedisCache.name(name).type(key);
        Assert.assertEquals("string", type);
        System.out.println("type: " + type);
        String set = RedisCache.name(name).set(key, "789");
        Assert.assertEquals("OK", set);
        System.out.println("set: " + set);
        String get1 = RedisCache.name(name).get(key);
        Assert.assertEquals("789", get1);
        System.out.println("get: " + get1);

        System.out.println();
        Long expire = RedisCache.name(name).expire(key, 60);
        Assert.assertEquals(1, expire.longValue());
        System.out.println("expire: " + expire);
        Long persist = RedisCache.name(name).persist(key);
        Assert.assertEquals(1, persist.longValue());
        System.out.println("persist: " + persist);
        boolean exists = RedisCache.name(name).exists(key);
        Assert.assertTrue(exists);
        System.out.println("exists: " + exists);
        Long del = RedisCache.name(name).del(key);
        Assert.assertEquals(1, del.longValue());
        System.out.println("del: " + del);
    }

    @Test
    public void testString() {
        String key = "test:dxy";
        String setex = RedisCache.setex(key, timeout, "123456");
        Assert.assertEquals("OK", setex);
        System.out.println("setex: " + setex);
        String get = RedisCache.get(key);
        Assert.assertEquals("123456", get);
        System.out.println("get: " + get);
        String set = RedisCache.set(key, "789");
        Assert.assertEquals("OK", set);
        System.out.println("set: " + set);
        String get1 = RedisCache.get(key);
        Assert.assertEquals("789", get1);
        System.out.println("get: " + get1);

        System.out.println();
        String type = RedisCache.type(key);
        System.out.println(type);
        Assert.assertEquals(type, "string");

        System.out.println();
        Long expire = RedisCache.expire(key, 60);
        Assert.assertEquals(1, expire.longValue());
        System.out.println("expire: " + expire);

        System.out.println();
        Long ttl = RedisCache.ttl(key);
        Assert.assertNotNull(ttl);
        System.out.println(ttl);

        System.out.println();
        Long persist = RedisCache.persist(key);
        Assert.assertEquals(1, persist.longValue());
        System.out.println("persist: " + persist);

        System.out.println();
        boolean exists = RedisCache.exists(key);
        Assert.assertTrue(exists);
        System.out.println("exists: " + exists);

        System.out.println();
        Long del = RedisCache.del(key);
        Assert.assertEquals(1, del.longValue());
        System.out.println("del: " + del);
    }

    @Test
    public void testNx() {
        String key = "test:dxy_nx";
        Long setnx = RedisCache.setnx(key, "123456");
        Assert.assertEquals(1, setnx.longValue());
        System.out.println("setnx: " + setnx);
        String get = RedisCache.get(key);
        Assert.assertEquals("123456", get);
        System.out.println("get: " + get);
        System.out.println("del: " + RedisCache.del(key));
    }

    @Test
    public void testNumber() {
        String key = "test:dxy_number";
        RedisCache.del(key);
        Long incrBy = RedisCache.incrBy(key, 10);
        Assert.assertEquals(10, incrBy.longValue());
        System.out.println("incrBy: " + incrBy);
        String get = RedisCache.get(key);
        Assert.assertEquals("10", get);
        System.out.println("get: " + get);
        Long decrBy = RedisCache.decrBy(key, 5);
        Assert.assertEquals(5, decrBy.longValue());
        System.out.println("decr: " + decrBy);
        String get1 = RedisCache.get(key);
        Assert.assertEquals("5", get1);
        System.out.println("get: " + get1);
        System.out.println("del: " + RedisCache.del(key));
    }

    @Test
    public void testList() {
        String key = "test:dxy_list";
        RedisCache.del(key);
        Long lpush = RedisCache.lpush(key, "1");
        Assert.assertEquals(1, lpush.longValue());
        System.out.println("lpush: " + lpush);
        Long rpush = RedisCache.rpush(key, "rpush");
        Assert.assertEquals(2, rpush.longValue());
        System.out.println("rpush: " + rpush);
        System.out.println("lpush: " + RedisCache.lpush(key, "2"));
        System.out.println("lrange: " + RedisCache.lrange(key, 1, 2));
        System.out.println("lrange: " + RedisCache.lrange(key, 0, 10));
        System.out.println("lrangePage: " + RedisCache.lrangePage(key, 0, 15));
        String lindex = RedisCache.lindex(key, 1);
        Assert.assertEquals(lindex, "1");
        System.out.println("lindex: " + lindex);
        System.out.println("lpop: " + RedisCache.lpop(key));
        System.out.println("rpop: " + RedisCache.rpop(key));
        System.out.println("ltrim: " + RedisCache.ltrim(key, 1, 4));
        System.out.println("llen: " + RedisCache.llen(key));
        System.out.println("lrange: " + RedisCache.lrangeAll(key));
        System.out.println("lrem: " + RedisCache.lrem(key, 0, "3"));
        System.out.println("lrem: " + RedisCache.lrem(key, 0, "2"));
        System.out.println("lrange: " + RedisCache.lrangeAll(key));
        System.out.println("del: " + RedisCache.del(key));
    }

    @Test
    public void testSet() {
        String key = "test:dxy_set";
        Long sadd = RedisCache.sadd(key, "123456");
        Assert.assertEquals(1, sadd.longValue());
        System.out.println("sadd: " + sadd);
        boolean sismember = RedisCache.sismember(key, "123456");
        Assert.assertTrue(sismember);
        System.out.println("sismember: " + sismember);
        System.out.println("smembers: " + RedisCache.smembers(key));
        System.out.println("del: " + RedisCache.del(key));
    }

    @Test
    public void testMap() {
        String key = "test:dxy_hset";
        Long hset = RedisCache.hset(key, "k1", "v1");
        Assert.assertEquals(1, hset.longValue());
        System.out.println("hset: " + hset);
        String value = RedisCache.hget(key, "k1");
        Assert.assertEquals(value, "v1");
        System.out.println("hget: " + value);
        Map<String, String> hash = Maps.newHashMap();
        hash.put("k3", "v3");
        hash.put("k4", "v4");
        String hmset = RedisCache.hmset(key, hash);
        Assert.assertEquals(hmset, "OK");
        System.out.println("hmset: " + hmset);
        System.out.println("hgetAll: " + JacksonUtil.to(RedisCache.hgetAll(key)));
        System.out.println("del: " + RedisCache.del(key));
    }

    @Test
    public void testPf() {
        String key = "test:dxy_pf";
        RedisCache.del(key);
        Long pfadd = RedisCache.pfadd(key, "pf1");
        Assert.assertEquals(1, pfadd.longValue());
        System.out.println("pfadd: " + pfadd);
        Long pfcount = RedisCache.pfcount(key);
        Assert.assertEquals(1, pfcount.longValue());
        System.out.println("pfcount: " + pfcount);
        Long pfadd1 = RedisCache.pfadd(key, "pf1");
        Assert.assertEquals(0, pfadd1.longValue());
        System.out.println("pfadd: " + pfadd1);
        RedisCache.pfadd(key, "pf2");
        Long pfcount1 = RedisCache.pfcount(key);
        Assert.assertEquals(2, pfcount1.longValue());
        System.out.println("pfcount: " + pfcount1);
        RedisCache.pfadd(key, "pf3");
        Long pfcount2 = RedisCache.pfcount(key);
        Assert.assertEquals(3, pfcount2.longValue());
        System.out.println("pfcount: " + pfcount2);
        System.out.println("del: " + RedisCache.del(key));
    }

    @Test
    public void testBit() {
        String key = "test:dxy_bit";
        int offset = 10000;
        RedisCache.del(key);
        boolean setbit = RedisCache.setbit(key, offset, true);
        Assert.assertFalse(setbit);
        System.out.println("setbit: " + setbit);
        boolean getbit = RedisCache.getbit(key, offset);
        Assert.assertTrue(getbit);
        System.out.println("getbit: " + getbit);
        Long bitcount = RedisCache.bitcount(key);
        Assert.assertEquals(1, bitcount.longValue());
        System.out.println("bitcount: " + bitcount);
        Long bitpos = RedisCache.bitpos(key, true);
        Assert.assertEquals(offset, bitpos.longValue());
        System.out.println("bitpos: " + bitpos);
        System.out.println("del: " + RedisCache.del(key));
    }

    @Test
    public void testBloom() {
        String key = "test:dxy_bloom";
        long startTime = System.currentTimeMillis();
        RedisCache.bloomadd(key, "123");
        RedisCache.bloomadd(key, "456");
        RedisCache.bloomadd(key, "789");
        System.out.println("耗时" + (System.currentTimeMillis() - startTime) + "毫秒");
        boolean bloomcons = RedisCache.bloomcons(key, "456");
        Assert.assertTrue(bloomcons);
        System.out.println("bloomcons: " + bloomcons);
        System.out.println("bloomcons: " + RedisCache.bloomcons(key, "789"));
        System.out.println("del: " + RedisCache.del(key));
    }

    @Test
    public void testRpoplpush() {
        final String key = "test:redis_source";
        String target = "test:redis_target";
        RedisCache.single().executeVoid(j -> {
            j.lpush(key, "1", "2", "3");
            String value = j.rpoplpush(key, target);
            System.out.println(value);
            String remove = j.lpop(target);
            System.out.println(value);
            Assert.assertEquals(remove, value);
            List<String> remianer = j.lrange(key, 0, -1);
            System.out.println(remianer);
            j.del(key, target);
        });
    }

    @Test
    public void testBrpoplpush() throws InterruptedException {
        final String key = "test:redis_bsource";
        String target = "test:redis_btarget";

        String value = "123";
        String[] res = new String[1];
        Thread t = new Thread(() -> {
            RedisCache.single().executeVoid(j -> {
                res[0] = j.brpoplpush(key, target, 5);
                System.out.println("brpoplpush: " + res[0]);
                synchronized (key) {
                    key.notifyAll();
                }
            });
        });
        t.setDaemon(true);
        t.start();

        RedisCache.single().executeVoid(j -> {
            j.lpush(key, value);
        });

        if (res[0] == null) {
            synchronized (key) {
                if (res[0] == null) {
                    key.wait(5000);
                }
            }
        }

        Assert.assertEquals(res[0], value);
    }
}
