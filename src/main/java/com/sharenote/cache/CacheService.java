package com.sharenote.cache;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;

import com.github.benmanes.caffeine.cache.Cache;

public class CacheService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final Cache<String, Object> localCache;

    // Spring automatically injects your configured redisTemplate bean here
    public CacheService(RedisTemplate<String, Object> redisTemplate,Cache<String, Object> localCache) {
        this.redisTemplate = redisTemplate;
        this.localCache = localCache;
    }

    // ==========================================
    // 1. STRING OPERATIONS (Standard Key-Value)
    // ==========================================

    /**
     * Save a value to Redis with an expiration time (Time-To-Live).
     */
    public void setWithTtl(String key, Object value, long timeoutInMinutes) {
        // .opsForValue() targets Redis String commands (SET, GET, etc.)
        redisTemplate.opsForValue().set(key, value, timeoutInMinutes, TimeUnit.MINUTES);
    }

    /**
     * Retrieve a value from Redis.
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * Delete a key from Redis.
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    // ==========================================
    // 2. HASH OPERATIONS (Objects with Fields)
    // ==========================================

    /**
     * Store a specific field inside a Redis Hash.
     * Useful for grouping related data under one master key (e.g., user profiles).
     */
    public void putInHash(String redisKey, String hashField, Object value) {
        // .opsForHash() targets Redis Hash commands (HSET, HGET, etc.)
        redisTemplate.opsForHash().put(redisKey, hashField, value);
    }

    /**
     * Retrieve a single field's value from a Redis Hash.
     */
    public Object getFromHash(String redisKey, String hashField) {
        return redisTemplate.opsForHash().get(redisKey, hashField);
    }

    public void putUserRoleInSet(Set<Long> roleIds) {

        

        CompletableFuture<Void> redisTask = CompletableFuture.runAsync(()-> {
            redisTemplate.opsForSet(CacheConfig.USER_ROLE_CACHE_KEY, roleIds);
        });

        CompletableFuture<Void> localCacheTask = CompletableFuture.runAsync(()-> {
            localCache.put(CacheConfig.USER_ROLE_CACHE_KEY, roleIds);
        });

        CompletableFuture.allOf(redisTask,localCacheTask);
    }
}
