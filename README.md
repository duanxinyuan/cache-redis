# cache-redis
Redis缓存库，支持caffeine、guava、redis、redis-sentinel、redis-sharding、redis-cluster，支持两级缓存配置，支持布隆过滤器，支持Redis分布式锁

## Maven依赖：
```xml
<dependency>
    <groupId>com.github.duanxinyuan</groupId>
    <artifactId>library-cache-redis</artifactId>
    <version>1.0.0</version>
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
    RedisCache.set
    RedisCache.get
    RedisCache.del
    RedisCache.exists
    RedisCache.expire
    RedisCache.persist
    
    //key value
    RedisCache.setnx
    
    //list
    RedisCache.lpush
    RedisCache.rpush
    RedisCache.lindex
    RedisCache.lrangePage
    RedisCache.lrem
    
    //set
    RedisCache.sadd
    RedisCache.sismember
    RedisCache.smembers
    
    //hash
    RedisCache.hset
    RedisCache.hmset
    RedisCache.hget
    RedisCache.hgetAll
    
    //hyperloglog
    RedisCache.pfadd
    RedisCache.pfcount
  
    //bitmap
    RedisCache.setbit
    RedisCache.getbit
    RedisCache.bitcount
    RedisCache.bitop
    RedisCache.bitfield
    RedisCache.bitpos
  
    //bloomfilter
    RedisCache.bloomadd
    RedisCache.bloomcons

    //distributed lock
    RedisCache.getDistributedLock
    RedisCache.releaseDistributedLock