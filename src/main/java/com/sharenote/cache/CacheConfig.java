package com.sharenote.cache;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
public class CacheConfig {

    public static final String USER_ROLE_CACHE_KEY = "user:roles";
    public static final String USER_PERMISSION_CACHE_KEY = "user:permissions";

    @Bean
    public Cache<String, Object> localCache() {
        return Caffeine.newBuilder()
                // Limit memory footprint by capping total active users
                .maximumSize(100_000)
                // Automatically evict users who go idle for 15 minutes
                .expireAfterAccess(15, TimeUnit.MINUTES)
                // Record metrics to monitor your Hit/Miss ratios
                .recordStats()
                .build();
    }
}