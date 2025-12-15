package com.sky.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Slf4j
public class RedisConfiguration {

    @Bean//后续可以自动注入到其他类中使用
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory) {//RedisConnectionFactory注入连接工厂对象
        log.info("开始创建redis对象");
        RedisTemplate redisTemplate = new RedisTemplate();
        //设置Redis连接工厂对象
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        //设置Redis key的序列化器
        redisTemplate.setKeySerializer(new StringRedisSerializer());

        return redisTemplate;//我们自己写这个配置类，就是为了得到我们想要的序列器的redistemplate
    }
}
