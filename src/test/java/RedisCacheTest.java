import com.dxy.library.cache.RedisCache;
import com.dxy.library.json.gson.GsonUtil;
import org.junit.Test;

/**
 * @author duanxinyuan
 * 2018/8/9 20:04
 */
public class RedisCacheTest {
    public int timeout = 60;

    @Test
    public void testAll() {
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
    public void testString() {
        System.out.println("set: " + RedisCache.set("text_dxy", "123456", timeout));
        System.out.println("get: " + RedisCache.get("text_dxy"));
        System.out.println("set: " + RedisCache.set("text_dxy", "789"));
        System.out.println("get: " + RedisCache.get("text_dxy"));

        System.out.println("\n");
        System.out.println("expire: " + RedisCache.expire("text_dxy", 60));
        System.out.println("persist: " + RedisCache.persist("text_dxy"));
        System.out.println("exist: " + RedisCache.exist("text_dxy"));
        System.out.println("del: " + RedisCache.del("text_dxy"));
    }

    @Test
    public void testNx() {
        System.out.println("setnx: " + RedisCache.setnx("text_dxy_nx", "123456",timeout));
        System.out.println("get: " + RedisCache.get("text_dxy_nx"));
        System.out.println("setnx: " + RedisCache.setnx("text_dxy_nx", "789"));
        System.out.println("get: " + RedisCache.get("text_dxy_nx"));
        RedisCache.del("text_dxy_nx");
    }

    @Test
    public void testNumber() {
        System.out.println("incr: " + RedisCache.incr("text_dxy_number", 10,timeout));
        System.out.println("get: " + RedisCache.get("text_dxy_number"));
        System.out.println("decr: " + RedisCache.decr("text_dxy_number", 5,timeout));
        System.out.println("get: " + RedisCache.get("text_dxy_number"));
        RedisCache.del("text_dxy_number");
    }

    @Test
    public void testList() {
        System.out.println("lpush: " + RedisCache.lpush("text_dxy_list", "1",timeout));
        System.out.println("rpush: " + RedisCache.rpush("text_dxy_list", "rpush",timeout));
        System.out.println("lpush: " + RedisCache.lpush("text_dxy_list", "2",timeout));
        System.out.println("lpush: " + RedisCache.lpush("text_dxy_list", "3",timeout));
        System.out.println("lpush: " + RedisCache.lpush("text_dxy_list", "4",timeout));
        System.out.println("lpush: " + RedisCache.lpush("text_dxy_list", "5",timeout));
        System.out.println("lrange: " + GsonUtil.to(RedisCache.lrange("text_dxy_list", 1, 2)));
        System.out.println("lrange: " + GsonUtil.to(RedisCache.lrange("text_dxy_list", 10)));
        System.out.println("lrangePage: " + GsonUtil.to(RedisCache.lrangePage("text_dxy_list", 0, 15)));
        System.out.println("lindex: " + RedisCache.lindex("text_dxy_list", 1));
        System.out.println("lpop: " + RedisCache.lpop("text_dxy_list"));
        System.out.println("rpop: " + RedisCache.rpop("text_dxy_list"));
        System.out.println("ltrim: " + RedisCache.ltrim("text_dxy_list", 1, 4));
        System.out.println("llen: " + RedisCache.llen("text_dxy_list"));
        System.out.println("lrange: " + GsonUtil.to(RedisCache.lrange("text_dxy_list")));
        System.out.println("lrem: " + RedisCache.lrem("text_dxy_list", "3"));
        System.out.println("lrem: " + RedisCache.lrem("text_dxy_list", "2"));
        System.out.println("lrange: " + GsonUtil.to(RedisCache.lrange("text_dxy_list")));
        RedisCache.del("text_dxy_list");
    }

    @Test
    public void testSet() {
        System.out.println("sadd: " + RedisCache.sadd("text_dxy_set", "123456"));
        System.out.println("sismember: " + RedisCache.sismember("text_dxy_set", "123456"));
        System.out.println("smembers: " + GsonUtil.to(RedisCache.smembers("text_dxy_set")));
        RedisCache.del("text_dxy_set");
    }

    @Test
    public void testMap() {
        System.out.println("hset: " + RedisCache.hset("text_dxy_hset", "k1", "v1",timeout));
        System.out.println("hget: " + RedisCache.hget("text_dxy_hset", "k1"));
        System.out.println("hset: " + RedisCache.hset("text_dxy_hset", "k2", "v2",timeout));
        System.out.println("hmset: " + RedisCache.hmset("text_dxy_hset", "k3", "v3", "k4", "v4"));
        System.out.println("hgetAll: " + GsonUtil.to(RedisCache.hgetAll("text_dxy_hset")));
        RedisCache.del("text_dxy_hset");
    }

    @Test
    public void testPf() {
        System.out.println("pfadd: " + RedisCache.pfadd("text_dxy_pf", "pf1",timeout));
        System.out.println("pfcount: " + RedisCache.pfcount("text_dxy_pf"));
        System.out.println("pfadd: " + RedisCache.pfadd("text_dxy_pf", "pf1",timeout));
        System.out.println("pfcount: " + GsonUtil.to(RedisCache.pfcount("text_dxy_pf")));
        RedisCache.del("text_dxy_pf");
    }

    @Test
    public void testBit() {
        System.out.println("setbit: " + RedisCache.setbit("text_dxy_bit", 10000, true));
        System.out.println("getbit: " + RedisCache.getbit("text_dxy_bit", 10000));
        System.out.println("bitcount: " + RedisCache.bitcount("text_dxy_bit"));
        System.out.println("bitpos: " + GsonUtil.to(RedisCache.bitpos("text_dxy_bit", true)));
        RedisCache.del("text_dxy_bit");
    }

    @Test
    public void testBloom() {
        //测试错误率
        int failCount = 0;
        for (int i = 0; i < 10000000; i++) {
            if (!RedisCache.bloomadd("text_dxy_bloom", String.valueOf(i))) {
                failCount++;
            }
        }
        System.out.println("failCount: " + failCount);
        RedisCache.del("text_dxy_bloom");

//        System.out.println(Clock.systemUTC().millis());
//        System.out.println("bloomadd: " + Cache.bloomadd("text_dxy_bloom", "alskdahsdoaishydoiauysod"));
//        System.out.println(Clock.systemUTC().millis());
//        System.out.println("bloomcons: " + Cache.bloomcons("text_dxy_bloom", "alskdahsdoaishydoiauysod"));
//        System.out.println(Clock.systemUTC().millis());
//        System.out.println("bloomcons: " + Cache.bloomcons("text_dxy_bloom", "abc"));
//        Cache.del("text_dxy_bloom");
    }
}
