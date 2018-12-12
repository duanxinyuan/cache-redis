# Cache
缓存库，支持caffeine、guava、redis、redis-sentinel、redis-sharding、redis-cluster，支持两级缓存配置，支持布隆过滤器，支持Redis分布式锁

## Maven依赖：
```xml
<dependency>
    <groupId>com.github.duanxinyuan</groupId>
    <artifactId>library-cache</artifactId>
    <version>1.5.0</version>
</dependency>
```

## 只需如下配置
```text
#是否使用内存
cache.memory.enable=true
#内存类型，guava/caffeine
cache.memory.type=caffeine
#Redis缓存类型，single/sentinel/shard/cluster
cache.redis.type=cluster
#
#Memory，如cache.memory.enable为true，必须配置
cache.memory.key.capacity.initial=100
cache.memory.key.capacity.max=50000
cache.memory.expire.seconds.after.write=300
cache.memory.expire.seconds.after.access=300
cache.memory.refresh.seconds.after.write=300
#
#Redis，必须配置
cache.redis.connection.max.total=100
cache.redis.connection.max.idle=50
cache.redis.max.wait.millis=5000
cache.redis.nodes=127.0.0.1:6379,127.0.0.1:6379
#没有密码不需要配置
cache.redis.password=465a4sda1
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