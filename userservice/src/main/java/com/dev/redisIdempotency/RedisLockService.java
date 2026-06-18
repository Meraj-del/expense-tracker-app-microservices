package com.dev.redisIdempotency;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisLockService {

    private final StringRedisTemplate redisTemplate;

    public RedisLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean acquireLock(String key, long timeoutMillis) {
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, "locked", Duration.ofMillis(timeoutMillis));
        return Boolean.TRUE.equals(success);
    }

    public void releaseLock(String key) {
        redisTemplate.delete(key);
    }
}
