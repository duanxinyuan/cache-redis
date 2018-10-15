import com.dxy.library.cache.Cache;
import com.dxy.library.json.gson.GsonUtil;
import org.junit.Test;

/**
 * @author duanxinyuan
 * 2018/8/9 20:04
 */
public class CacheTest {

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
        System.out.println("set: " + Cache.set("text_dxy", "123456"));
        System.out.println("get: " + Cache.get("text_dxy"));
        System.out.println("set: " + Cache.set("text_dxy", "789"));
        System.out.println("get: " + Cache.get("text_dxy"));

        System.out.println("\n");
        System.out.println("expire: " + Cache.expire("text_dxy", 60));
        System.out.println("persist: " + Cache.persist("text_dxy"));
        System.out.println("exist: " + Cache.exist("text_dxy"));
        System.out.println("del: " + Cache.del("text_dxy"));
    }

    @Test
    public void testNx() {
        System.out.println("setnx: " + Cache.setnx("text_dxy_nx", "123456"));
        System.out.println("get: " + Cache.get("text_dxy_nx"));
        System.out.println("setnx: " + Cache.setnx("text_dxy_nx", "789"));
        System.out.println("get: " + Cache.get("text_dxy_nx"));
        Cache.del("text_dxy_nx");
    }

    @Test
    public void testNumber() {
        System.out.println("incr: " + Cache.incr("text_dxy_number", 10));
        System.out.println("get: " + Cache.get("text_dxy_number"));
        System.out.println("decr: " + Cache.decr("text_dxy_number", 5));
        System.out.println("get: " + Cache.get("text_dxy_number"));
        Cache.del("text_dxy_number");
    }

    @Test
    public void testList() {
        System.out.println("lpush: " + Cache.lpush("text_dxy_list", "1"));
        System.out.println("rpush: " + Cache.rpush("text_dxy_list", "rpush"));
        System.out.println("lpush: " + Cache.lpush("text_dxy_list", "2"));
        System.out.println("lpush: " + Cache.lpush("text_dxy_list", "3"));
        System.out.println("lpush: " + Cache.lpush("text_dxy_list", "4"));
        System.out.println("lpush: " + Cache.lpush("text_dxy_list", "5"));
        System.out.println("lrange: " + GsonUtil.to(Cache.lrange("text_dxy_list", 1, 2)));
        System.out.println("lrange: " + GsonUtil.to(Cache.lrange("text_dxy_list", 10)));
        System.out.println("lrangePage: " + GsonUtil.to(Cache.lrangePage("text_dxy_list", 0, 15)));
        System.out.println("lindex: " + Cache.lindex("text_dxy_list", 1));
        System.out.println("lpop: " + Cache.lpop("text_dxy_list"));
        System.out.println("rpop: " + Cache.rpop("text_dxy_list"));
        System.out.println("ltrim: " + Cache.ltrim("text_dxy_list", 1, 4));
        System.out.println("llen: " + Cache.llen("text_dxy_list"));
        System.out.println("lrange: " + GsonUtil.to(Cache.lrange("text_dxy_list")));
        System.out.println("lrem: " + Cache.lrem("text_dxy_list", "3"));
        System.out.println("lrem: " + Cache.lrem("text_dxy_list", "2"));
        System.out.println("lrange: " + GsonUtil.to(Cache.lrange("text_dxy_list")));
        Cache.del("text_dxy_list");
    }

    @Test
    public void testSet() {
        System.out.println("sadd: " + Cache.sadd("text_dxy_set", "123456"));
        System.out.println("sismember: " + Cache.sismember("text_dxy_set", "123456"));
        System.out.println("smembers: " + GsonUtil.to(Cache.smembers("text_dxy_set")));
        Cache.del("text_dxy_set");
    }

    @Test
    public void testMap() {
        System.out.println("hset: " + Cache.hset("text_dxy_hset", "k1", "v1"));
        System.out.println("hget: " + Cache.hget("text_dxy_hset", "k1"));
        System.out.println("hset: " + Cache.hset("text_dxy_hset", "k2", "v2"));
        System.out.println("hmset: " + Cache.hmset("text_dxy_hset", "k3", "v3", "k4", "v4"));
        System.out.println("hgetAll: " + GsonUtil.to(Cache.hgetAll("text_dxy_hset")));
        Cache.del("text_dxy_hset");
    }

    @Test
    public void testPf() {
        System.out.println("pfadd: " + Cache.pfadd("text_dxy_pf", "pf1"));
        System.out.println("pfcount: " + Cache.pfcount("text_dxy_pf"));
        System.out.println("pfadd: " + Cache.pfadd("text_dxy_pf", "pf1"));
        System.out.println("pfcount: " + GsonUtil.to(Cache.pfcount("text_dxy_pf")));
        Cache.del("text_dxy_pf");
    }

    @Test
    public void testBit() {
        System.out.println("setbit: " + Cache.setbit("text_dxy_bit", 10000, true));
        System.out.println("getbit: " + Cache.getbit("text_dxy_bit", 10000));
        System.out.println("bitcount: " + Cache.bitcount("text_dxy_bit"));
        System.out.println("bitpos: " + GsonUtil.to(Cache.bitpos("text_dxy_bit", true)));
        Cache.del("text_dxy_bit");
    }

    @Test
    public void testBloom() {
        //测试错误率
        int failCount = 0;
        for (int i = 0; i < 10000000; i++) {
            if (!Cache.bloomadd("text_dxy_bloom", String.valueOf(i))) {
                failCount++;
            }
        }
        System.out.println("failCount: " + failCount);
        Cache.del("text_dxy_bloom");

//        System.out.println(Clock.systemUTC().millis());
//        System.out.println("bloomadd: " + Cache.bloomadd("text_dxy_bloom", "alskdahsdoaishydoiauysod"));
//        System.out.println(Clock.systemUTC().millis());
//        System.out.println("bloomcons: " + Cache.bloomcons("text_dxy_bloom", "alskdahsdoaishydoiauysod"));
//        System.out.println(Clock.systemUTC().millis());
//        System.out.println("bloomcons: " + Cache.bloomcons("text_dxy_bloom", "abc"));
//        Cache.del("text_dxy_bloom");
    }
}
