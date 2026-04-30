package guard.ics.backend.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5));
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withCacheConfiguration("alertStats", RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofSeconds(30)))
                .withCacheConfiguration("deviceList", RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofSeconds(15)))
                .withCacheConfiguration("trafficStats", RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofSeconds(30)))
                .build();
    }
}
