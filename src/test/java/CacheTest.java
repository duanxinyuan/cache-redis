import com.dxy.library.cache.cache.Cache;
import com.dxy.library.json.GsonUtil;
import org.junit.Test;

/**
 * @author duanxinyuan
 * 2018/8/9 20:04
 */
public class CacheTest {

    @Test
    public void test() {
        Cache cache = Cache.getInstance();
        System.out.println("set: " + cache.set("text_dxy", "123456"));
        System.out.println("get: " + cache.get("text_dxy"));
        System.out.println("set: " + cache.set("text_dxy", "789"));
        System.out.println("get: " + cache.get("text_dxy"));

        System.out.println("\n");
        System.out.println("expire: " + cache.expire("text_dxy", 60));
        System.out.println("persist: " + cache.persist("text_dxy"));
        System.out.println("exist: " + cache.exist("text_dxy"));
        System.out.println("del: " + cache.del("text_dxy"));

        System.out.println("\n");
        System.out.println("setnx: " + cache.setnx("text_dxy_nx", "123456"));
        System.out.println("get: " + cache.get("text_dxy_nx"));
        System.out.println("setnx: " + cache.setnx("text_dxy_nx", "789"));
        System.out.println("get: " + cache.get("text_dxy_nx"));
        cache.del("text_dxy_nx");

        System.out.println("\n");
        System.out.println("incr: " + cache.incr("text_dxy_number", 10));
        System.out.println("get: " + cache.get("text_dxy_number"));
        System.out.println("decr: " + cache.decr("text_dxy_number", 5));
        System.out.println("get: " + cache.get("text_dxy_number"));
        cache.del("text_dxy_number");

        System.out.println("\n");
        System.out.println("lpush: " + cache.lpush("text_dxy_list", "1"));
        System.out.println("rpush: " + cache.rpush("text_dxy_list", "rpush"));
        System.out.println("lpush: " + cache.lpush("text_dxy_list", "2"));
        System.out.println("lpush: " + cache.lpush("text_dxy_list", "3"));
        System.out.println("lpush: " + cache.lpush("text_dxy_list", "4"));
        System.out.println("lpush: " + cache.lpush("text_dxy_list", "5"));
        System.out.println("lrange: " + GsonUtil.to(cache.lrange("text_dxy_list", 1, 2)));
        System.out.println("lrange: " + GsonUtil.to(cache.lrange("text_dxy_list", 10)));
        System.out.println("lrangePage: " + GsonUtil.to(cache.lrangePage("text_dxy_list", 0, 15)));
        System.out.println("lindex: " + cache.lindex("text_dxy_list", 1));
        System.out.println("lpop: " + cache.lpop("text_dxy_list"));
        System.out.println("rpop: " + cache.rpop("text_dxy_list"));
        System.out.println("ltrim: " + cache.ltrim("text_dxy_list", 1, 4));
        System.out.println("llen: " + cache.llen("text_dxy_list"));
        System.out.println("lrange: " + GsonUtil.to(cache.lrange("text_dxy_list")));
        System.out.println("lrem: " + cache.lrem("text_dxy_list", "3"));
        System.out.println("lrem: " + cache.lrem("text_dxy_list", "2"));
        System.out.println("lrange: " + GsonUtil.to(cache.lrange("text_dxy_list")));
        cache.del("text_dxy_list");

        System.out.println("\n");
        System.out.println("sadd: " + cache.sadd("text_dxy_set", "123456"));
        System.out.println("sismember: " + cache.sismember("text_dxy_set", "123456"));
        System.out.println("smembers: " + GsonUtil.to(cache.smembers("text_dxy_set")));
        cache.del("text_dxy_set");

        System.out.println("\n");
        System.out.println("hset: " + cache.hset("text_dxy_hset", "k1", "v1"));
        System.out.println("hget: " + cache.hget("text_dxy_hset", "k1"));
        System.out.println("hset: " + cache.hset("text_dxy_hset", "k2", "v2"));
        System.out.println("hmset: " + cache.hmset("text_dxy_hset", "k3", "v3", "k4", "v4"));
        System.out.println("hgetAll: " + GsonUtil.to(cache.hgetAll("text_dxy_hset")));
        cache.del("text_dxy_hset");

        System.out.println("\n");
        System.out.println("pfadd: " + cache.pfadd("text_dxy_pf", "pf1"));
        System.out.println("pfcount: " + cache.pfcount("text_dxy_pf"));
        System.out.println("pfadd: " + cache.pfadd("text_dxy_pf", "pf1"));
        System.out.println("pfcount: " + GsonUtil.to(cache.pfcount("text_dxy_pf")));
        cache.del("text_dxy_pf");

        System.out.println("\n");
        System.out.println("setbit: " + cache.setbit("text_dxy_bit", 10000, true));
        System.out.println("getbit: " + cache.getbit("text_dxy_bit", 10000));
        System.out.println("bitcount: " + cache.bitcount("text_dxy_bit"));
        System.out.println("bitpos: " + GsonUtil.to(cache.bitpos("text_dxy_bit", true)));
        cache.del("text_dxy_bit");
    }
}
