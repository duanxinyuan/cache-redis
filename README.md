# Cache
缓存库，支持caffeine、guava、redis、redis-cluster，支持两级缓存配置，支持布隆过滤器，支持Redis分布式锁

## Maven依赖：
```xml
<dependency>
    <groupId>com.github.duanxinyuan</groupId>
    <artifactId>library-cache</artifactId>
    <version>1.2.0</version>
</dependency>
```

## 使用示例：
    
    //key value
    Cache.set
    Cache.get
    Cache.del
    Cache.exists
    Cache.expire
    Cache.persist
    
    //key value
    Cache.setnx
    
    //list
    Cache.lpush
    Cache.rpush
    Cache.lindex
    Cache.lrangePage
    Cache.lrem
    
    //set
    Cache.sadd
    Cache.sismember
    Cache.smembers
    
    //hash
    Cache.hset
    Cache.hmset
    Cache.hget
    Cache.hgetAll
    
    //hyperloglog
    Cache.pfadd
    Cache.pfcount
  
    //bitmap
    Cache.setbit
    Cache.getbit
    Cache.bitcount
    Cache.bitop
    Cache.bitfield
    Cache.bitpos
  
    //bloomfilter
    Cache.bloomadd
    Cache.bloomcons

    //distributed lock
    Cache.getDistributedLock
    Cache.releaseDistributedLock