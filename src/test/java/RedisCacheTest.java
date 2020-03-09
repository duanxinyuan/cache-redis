import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.dxy.library.cache.redis.RedisCache;
import com.dxy.library.cache.redis.inter.CommandsSupplier;
import com.dxy.library.cache.redis.inter.RedisFunction;
import com.dxy.library.json.jackson.JacksonUtil;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
        RedisCache.single((RedisFunction<Jedis, Void>) jedis -> {
            Pipeline pipelined = jedis.pipelined();
            System.out.println("set: " + pipelined.set("test:dxy", "123456"));
            System.out.println("set: " + pipelined.set("test:dxy1", "789"));
            pipelined.sync();
            pipelined.close();
            return null;
        });

        RedisCache.execute(new CommandsSupplier<Void>() {
            @Override
            public Void single(Jedis jedis) throws IOException {
                Pipeline pipelined = jedis.pipelined();
                System.out.println("set: " + pipelined.set("test:dxy", "123456"));
                System.out.println("set: " + pipelined.set("test:dxy1", "789"));
                pipelined.sync();
                pipelined.close();
                return null;
            }
        });

    }


    @Test
    public void testMsetAndMget() {
        String name = "mobeye";
        HashMap<String, String> map = Maps.newHashMap();
        map.put("test:dxy1", "1");
        map.put("test:dxy2", "2");
        map.put("test:dxy3", "3");
        map.put("test:dxy4", "4");

        ArrayList<String> keys = Lists.newArrayList(map.keySet());

        System.out.println("mset: " + RedisCache.name(name).mset(map));
        System.out.println("msetex: " + RedisCache.name(name).msetex(60, map));
        System.out.println("mget: " + RedisCache.name(name).mget(keys));
        System.out.println("mget: " + RedisCache.name(name).mget(keys, Integer.class));

//        System.out.println("del: " + RedisCache.name(name).del(keys));
    }


    @Test
    public void testNameString() {
        String name = "mobeye";
        System.out.println("set: " + RedisCache.name(name).setex("test:dxy", timeout, "123456"));
        System.out.println("get: " + RedisCache.name(name).get("test:dxy"));
        System.out.println("type: " + RedisCache.name(name).type("test:dxy"));
        System.out.println("set: " + RedisCache.name(name).set("test:dxy", "789"));
        System.out.println("get: " + RedisCache.name(name).get("test:dxy"));

        System.out.println("\n");
        System.out.println("expire: " + RedisCache.name(name).expire("test:dxy", 60));
        System.out.println("persist: " + RedisCache.name(name).persist("test:dxy"));
        System.out.println("exists: " + RedisCache.name(name).exists("test:dxy"));
        System.out.println("del: " + RedisCache.name(name).del("test:dxy"));
    }


    @Test
    public void testString() {
        System.out.println("set: " + RedisCache.set("test:dxy", "123456"));
        System.out.println("get: " + RedisCache.get("test:dxy"));
        System.out.println("set: " + RedisCache.set("test:dxy", "789"));
        System.out.println("get: " + RedisCache.get("test:dxy"));

        System.out.println("\n");
        System.out.println("expire: " + RedisCache.expire("test:dxy", 60));
        System.out.println("persist: " + RedisCache.persist("test:dxy"));
        System.out.println("exists: " + RedisCache.exists("test:dxy"));
        System.out.println("del: " + RedisCache.del("test:dxy"));
    }

    @Test
    public void testNx() {
        System.out.println("setnx: " + RedisCache.setnx("test:dxy_nx", "123456"));
        System.out.println("get: " + RedisCache.get("test:dxy_nx"));
        System.out.println("setnx: " + RedisCache.setnx("test:dxy_nx", "789"));
        System.out.println("get: " + RedisCache.get("test:dxy_nx"));
        System.out.println("del: " + RedisCache.del("test:dxy_nx"));
    }

    @Test
    public void testNumber() {
        System.out.println("incrBy: " + RedisCache.incrBy("test:dxy_number", 10));
        System.out.println("get: " + RedisCache.get("test:dxy_number"));
        System.out.println("decr: " + RedisCache.decrBy("test:dxy_number", 5));
        System.out.println("get: " + RedisCache.get("test:dxy_number"));
        System.out.println("del: " + RedisCache.del("test:dxy_number"));
    }

    @Test
    public void testList() {
        System.out.println("lpush: " + RedisCache.lpush("test:dxy_list", "1"));
        System.out.println("rpush: " + RedisCache.rpush("test:dxy_list", "rpush"));
        System.out.println("lpush: " + RedisCache.lpush("test:dxy_list", "2"));
        System.out.println("lpush: " + RedisCache.lpush("test:dxy_list", "3"));
        System.out.println("lpush: " + RedisCache.lpush("test:dxy_list", "4"));
        System.out.println("lpush: " + RedisCache.lpush("test:dxy_list", "5"));
        System.out.println("lrange: " + JacksonUtil.to(RedisCache.lrange("test:dxy_list", 1, 2)));
        System.out.println("lrange: " + JacksonUtil.to(RedisCache.lrange("test:dxy_list", 0, 10)));
        System.out.println("lrangePage: " + JacksonUtil.to(RedisCache.lrangePage("test:dxy_list", 0, 15)));
        System.out.println("lindex: " + RedisCache.lindex("test:dxy_list", 1));
        System.out.println("lpop: " + RedisCache.lpop("test:dxy_list"));
        System.out.println("rpop: " + RedisCache.rpop("test:dxy_list"));
        System.out.println("ltrim: " + RedisCache.ltrim("test:dxy_list", 1, 4));
        System.out.println("llen: " + RedisCache.llen("test:dxy_list"));
        System.out.println("lrange: " + JacksonUtil.to(RedisCache.lrangeAll("test:dxy_list")));
        System.out.println("lrem: " + RedisCache.lrem("test:dxy_list", 0, "3"));
        System.out.println("lrem: " + RedisCache.lrem("test:dxy_list", 0, "2"));
        System.out.println("lrange: " + JacksonUtil.to(RedisCache.lrangeAll("test:dxy_list")));
        System.out.println("del: " + RedisCache.del("test:dxy_list"));
    }

    @Test
    public void testSet() {
        System.out.println("sadd: " + RedisCache.sadd("test:dxy_set", "123456"));
        System.out.println("sismember: " + RedisCache.sismember("test:dxy_set", "123456"));
        System.out.println("smembers: " + JacksonUtil.to(RedisCache.smembers("test:dxy_set")));
        System.out.println("del: " + RedisCache.del("test:dxy_set"));
    }

    @Test
    public void testMap() {
        System.out.println("hset: " + RedisCache.hset("test:dxy_hset", "k1", "v1"));
        System.out.println("hget: " + RedisCache.hget("test:dxy_hset", "k1"));
        Map<String, String> hash = Maps.newHashMap();
        hash.put("k3", "v3");
        hash.put("k4", "v4");
        System.out.println("hmset: " + RedisCache.hmset("test:dxy_hset", hash));
        System.out.println("hgetAll: " + JacksonUtil.to(RedisCache.hgetAll("test:dxy_hset")));
        System.out.println("del: " + RedisCache.del("test:dxy_hset"));
    }

    @Test
    public void testPf() {
        System.out.println("pfadd: " + RedisCache.pfadd("test:dxy_pf", "pf1"));
        System.out.println("pfcount: " + RedisCache.pfcount("test:dxy_pf"));
        System.out.println("pfadd: " + RedisCache.pfadd("test:dxy_pf", "pf1"));
        System.out.println("pfcount: " + JacksonUtil.to(RedisCache.pfcount("test:dxy_pf")));
        System.out.println("del: " + RedisCache.del("test:dxy_pf"));
    }

    @Test
    public void testBit() {
        System.out.println("setbit: " + RedisCache.setbit("test:dxy_bit", 10000, true));
        System.out.println("getbit: " + RedisCache.getbit("test:dxy_bit", 10000));
        System.out.println("bitcount: " + RedisCache.bitcount("test:dxy_bit"));
        System.out.println("bitpos: " + JacksonUtil.to(RedisCache.bitpos("test:dxy_bit", true)));
        System.out.println("del: " + RedisCache.del("test:dxy_bit"));
    }

    @Test
    public void testBloom() {
        //测试错误率
        int failCount = 0;
        for (int i = 0; i < 1_0000; i++) {
            if (!RedisCache.bloomadd("test:dxy_bloom", String.valueOf(i))) {
                failCount++;
            }
        }
        System.out.println("failCount: " + failCount);
        System.out.println("del: " + RedisCache.del("test:dxy_bloom"));

//        System.out.println(Clock.systemUTC().millis());
//        System.out.println("bloomadd: " + Cache.bloomadd("test:dxy_bloom", "alskdahsdoaishydoiauysod"));
//        System.out.println(Clock.systemUTC().millis());
//        System.out.println("bloomcons: " + Cache.bloomcons("test:dxy_bloom", "alskdahsdoaishydoiauysod"));
//        System.out.println(Clock.systemUTC().millis());
//        System.out.println("bloomcons: " + Cache.bloomcons("test:dxy_bloom", "abc"));
//        Cache.del("test:dxy_bloom");
    }
}
