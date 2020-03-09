# cache-redis

* Redis缓存库，支持绝大部分Redis数据结构（string/list/hash/set/sortedSet/bitmap/hyperloglog/geohash/bloom）
* 支持single/sentinel/sharded/cluster四种集群方式
* 支持布隆过滤器
* 支持分布式锁
* 支持HyperLogLog、BitMap、GeoHash等特殊的数据结构
* 支持lua脚本

## Maven依赖：

```xml
<dependency>
    <groupId>com.mob.library</groupId>
    <artifactId>cache-redis</artifactId>
</dependency>
```

## 配置信息：

```text
#Redis缓存类型，single/sentinel/sharded/cluster，必须配置
cache.redis.type=cluster
#最大连接数，建议配置，默认为100
cache.redis.connection.max.total=100
#最大空闲连接数，建议配置，默认为50
cache.redis.connection.max.idle=50
#获取连接时的最大等待毫秒数，可不配置，默认为5000
cache.redis.max.wait.millis=5000
#Redis节点信息列表，多个使用逗号隔开，必须配置
cache.redis.nodes=10.21.131.12:6380,10.21.131.12:6381,10.21.131.12:6382,10.21.131.12:6383,10.21.131.12:6384,10.21.131.12:6385
#Redis密码，没有密码不需要配置
cache.redis.password=9vBaiNzlVMSAJMa
#Redis database，只有单机和哨兵模式支持，可不配置，默认为0
cache.redis.database=0
#连接超时毫秒数 和 读取数据超时毫秒数，可不配置，默认2000
cache.redis.timeout.millis=2000
#
#多个连接池配置如下：
#Redis缓存类型，single/sentinel/sharded/cluster，必须配置
cache.redis.type.mobeye=single
#最大连接数，建议配置，默认为100
cache.redis.connection.max.total.mobeye=100
#最大空闲连接数，建议配置，默认为50
cache.redis.connection.max.idle.mobeye=50
#获取连接时的最大等待毫秒数，可不配置，默认为5000
cache.redis.max.wait.millis.mobeye=5000
#Redis节点信息列表，多个使用逗号隔开，必须配置
cache.redis.nodes.mobeye=10.21.131.12:6379
#Redis密码，没有密码不需要配置
cache.redis.password.mobeye=9vBaiNzlVMSAJMa
#Redis database，只有单机和哨兵模式支持，可不配置，默认为0
cache.redis.database.mobeye=0
#连接超时毫秒数 和 读取数据超时毫秒数，可不配置，默认2000
cache.redis.timeout.millis.mobeye=2000
```

## 简洁版配置：

```text
cache.redis.type=cluster
cache.redis.connection.max.total=100
cache.redis.connection.max.idle=50
cache.redis.nodes=10.21.131.12:6380,10.21.131.12:6381,10.21.131.12:6382,10.21.131.12:6383,10.21.131.12:6384,10.21.131.12:6385
cache.redis.password=9vBaiNzlVMSAJMa
```

## 使用示例：
    
    //自定义操作
    RedisCache.execute
    RedisCache.single
    RedisCache.sentinel
    RedisCache.sharded
    RedisCache.cluster
     
    //common
    RedisCache.type
    RedisCache.expire
    RedisCache.expireAt
    RedisCache.persist
    RedisCache.exists
    RedisCache.del
    RedisCache.unlink
    RedisCache.rename
    RedisCache.renamenx

    //string
    RedisCache.set
    RedisCache.mset
    RedisCache.get
    RedisCache.mget
    RedisCache.setnx
    RedisCache.setex
    RedisCache.psetex
    RedisCache.msetex
    RedisCache.incr
    RedisCache.incrBy
    RedisCache.incrByFloat
    RedisCache.decr
    RedisCache.decrBy
    RedisCache.append
    RedisCache.strlen
    
    //hash
    RedisCache.hset
    RedisCache.hmset
    RedisCache.hsetnx
    RedisCache.hget
    RedisCache.hmget
    RedisCache.hgetAll
    RedisCache.hincrBy
    RedisCache.hincrByFloat
    RedisCache.hkeys
    RedisCache.hvals
    RedisCache.hexists
    RedisCache.hdel
    RedisCache.hstrlen

    //list
    RedisCache.lpush
    RedisCache.rpush
    RedisCache.lpushx
    RedisCache.rpushx
    RedisCache.lset
    RedisCache.linsert
    RedisCache.lrange
    RedisCache.lrangePage
    RedisCache.lrangeAll
    RedisCache.lindex
    RedisCache.llen
    RedisCache.lpop
    RedisCache.rpop
    RedisCache.blpop
    RedisCache.brpop
    RedisCache.lrem
    RedisCache.ltrim
    
    //set
    RedisCache.sadd
    RedisCache.smembers
    RedisCache.srem
    RedisCache.spop
    RedisCache.scard
    RedisCache.sismember
    RedisCache.srandmember
     
    //sorted set
    RedisCache.zadd
    RedisCache.zrange
    RedisCache.zrevrange
    RedisCache.zrem
    RedisCache.zincrby
    RedisCache.zrangeByScore
    RedisCache.zrevrangeByScore
    RedisCache.zrangeByLex
    RedisCache.zremrangeByRank
    RedisCache.zremrangeByScore
    RedisCache.zremrangeByLex
    RedisCache.zrank
    RedisCache.zrevrank
    RedisCache.zcard
    RedisCache.zscore
    RedisCache.zcount
    RedisCache.zlexcount
      
    //hyperloglog
    RedisCache.pfadd
    RedisCache.pfcount
  
    //bitmap
    RedisCache.setbit
    RedisCache.getbit
    RedisCache.bitcount
    RedisCache.bitpos
    RedisCache.bitop
    RedisCache.bitfield
  
    //Geo
    RedisCache.geoadd
    RedisCache.geodist
    RedisCache.geohash
    RedisCache.geopos
    RedisCache.georadius
    RedisCache.georadiusByMember
    
  
    //bloom filter
    RedisCache.bloomadd
    RedisCache.bloomcons

    //distributed lock
    RedisCache.getDistributedLock
    RedisCache.releaseDistributedLock
    
    //script
    RedisCache.eval