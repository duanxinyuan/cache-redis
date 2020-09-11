package com.dxy.library.cache.redis.properties;

import com.dxy.library.util.config.ConfigUtils;
import com.dxy.library.util.config.dto.Config;
import com.google.common.collect.Lists;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.List;

/**
 * Redis配置
 * @author duanxinyuan
 * 2019/4/15 17:12
 */
@Data
public class RedisProperties {

    //Redis缓存类型，single/sentinel/sharded/cluster，必须配置
    private String type;

    //最大连接数，建议配置，默认为100
    private int maxTotal = 100;

    //最大空闲连接数，建议配置，默认为50
    private int maxIdle = 50;

    //获取连接时的最大等待毫秒数，可不配置，默认为5000
    private long maxWaitMillis = 5000;

    //Redis节点信息列表，多个使用逗号隔开，必须配置
    private List<String> nodes;

    //Redis密码，没有密码不需要配置
    private String password;

    //Redis database，只有单机和哨兵模式支持，可不配置，默认为0
    private int database = 0;

    //连接超时毫秒数 和 读取数据超时毫秒数，可不配置，默认2000
    private int timeoutMillis = 2000;

    public RedisProperties() {
        this(Config.DEFAULT_NAME);
    }

    public RedisProperties(String name) {
        Config<String> cacheTypeConfig = ConfigUtils.getConfig("cache.redis.type", name);
        if (cacheTypeConfig != null) {
            this.type = cacheTypeConfig.getValue();
        }
        Config<String> maxTotalConfig = ConfigUtils.getConfig("cache.redis.connection.max.total", name);
        if (maxTotalConfig != null) {
            this.maxTotal = NumberUtils.toInt(maxTotalConfig.getValue());
        }
        Config<String> maxIdleConfig = ConfigUtils.getConfig("cache.redis.connection.max.idle", name);
        if (maxIdleConfig != null) {
            this.maxIdle = NumberUtils.toInt(maxIdleConfig.getValue());
        }
        Config<String> maxWaitMillisConfig = ConfigUtils.getConfig("cache.redis.max.wait.millis", name);
        if (maxWaitMillisConfig != null) {
            this.maxWaitMillis = NumberUtils.toInt(maxWaitMillisConfig.getValue());
        }
        Config<String> nodesConfig = ConfigUtils.getConfig("cache.redis.nodes", name);
        if (nodesConfig != null) {
            this.nodes = Lists.newArrayList(nodesConfig.getValue().split(","));
        }
        Config<String> passwordConfig = ConfigUtils.getConfig("cache.redis.password", name);
        if (passwordConfig != null && StringUtils.isNotBlank(passwordConfig.getValue()) && !"null".equals(passwordConfig.getValue())) {
            this.password = passwordConfig.getValue();
        }
        Config<String> databaseConfig = ConfigUtils.getConfig("cache.redis.database", name);
        if (databaseConfig != null) {
            this.database = NumberUtils.toInt(databaseConfig.getValue());
        }
        Config<String> timeoutMillisConfig = ConfigUtils.getConfig("cache.redis.timeout.millis", name);
        if (timeoutMillisConfig != null) {
            this.timeoutMillis = NumberUtils.toInt(timeoutMillisConfig.getValue());
        }
    }

}
