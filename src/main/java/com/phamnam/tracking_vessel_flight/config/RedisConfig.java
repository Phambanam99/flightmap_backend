package com.phamnam.tracking_vessel_flight.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

    @Value("${app.redis.cache.current-positions.ttl:300}")
    private long currentPositionsTtl;

    @Value("${app.redis.cache.aircraft-data.ttl:600}")
    private long aircraftDataTtl;

    @Value("${app.redis.cache.vessel-data.ttl:600}")
    private long vesselDataTtl;

    @Value("${app.redis.cache.user-sessions.ttl:1800}")
    private long userSessionsTtl;

    @Value("${app.redis.cache.api-responses.ttl:120}")
    private long apiResponsesTtl;

    @Value("${app.redis.cache.alerts.ttl:3600}")
    private long alertsTtl;

    @Value("${app.redis.cache.statistics.ttl:900}")
    private long statisticsTtl;

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Configure serializers
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = createJsonSerializer();

        // Key serialization
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);

        // Value serialization
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                                .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        // Custom cache configurations for different cache names
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Current positions cache (5 minutes)
        cacheConfigurations.put("currentPositions", defaultCacheConfig
                .entryTtl(Duration.ofSeconds(currentPositionsTtl)));

        // Aircraft data cache (10 minutes)
        cacheConfigurations.put("aircraftData", defaultCacheConfig
                .entryTtl(Duration.ofSeconds(aircraftDataTtl)));

        // Vessel data cache (10 minutes)
        cacheConfigurations.put("vesselData", defaultCacheConfig
                .entryTtl(Duration.ofSeconds(vesselDataTtl)));

        // User sessions cache (30 minutes)
        cacheConfigurations.put("userSessions", defaultCacheConfig
                .entryTtl(Duration.ofSeconds(userSessionsTtl)));

        // API responses cache (2 minutes)
        cacheConfigurations.put("apiResponses", defaultCacheConfig
                .entryTtl(Duration.ofSeconds(apiResponsesTtl)));

        // Alerts cache (1 hour)
        cacheConfigurations.put("alerts", defaultCacheConfig
                .entryTtl(Duration.ofSeconds(alertsTtl)));

        // Statistics cache (15 minutes)
        cacheConfigurations.put("statistics", defaultCacheConfig
                .entryTtl(Duration.ofSeconds(statisticsTtl)));

        // Real-time positions cache (30 seconds)
        cacheConfigurations.put("realtimePositions", defaultCacheConfig
                .entryTtl(Duration.ofSeconds(30)));

        // Route cache (1 hour)
        cacheConfigurations.put("routes", defaultCacheConfig
                .entryTtl(Duration.ofHours(1)));

        // Weather data cache (30 minutes)
        cacheConfigurations.put("weatherData", defaultCacheConfig
                .entryTtl(Duration.ofMinutes(30)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    private Jackson2JsonRedisSerializer<Object> createJsonSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        mapper.registerModule(new JavaTimeModule());

        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(mapper, Object.class);
        return serializer;
    }

    // Specialized Redis Templates for different data types
    @Bean
    public RedisTemplate<String, String> stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setDefaultSerializer(new StringRedisSerializer());
        return template;
    }

    @Bean("positionRedisTemplate")
    public RedisTemplate<String, Object> positionRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<Object> jsonSerializer = createJsonSerializer();

        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean("sessionRedisTemplate")
    public RedisTemplate<String, Object> sessionRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<Object> jsonSerializer = createJsonSerializer();

        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}