package com.pharmaflow.gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing token blacklist using Redis with in-memory fallback.
 * Tokens are blacklisted on logout or when compromised.
 *
 * Falls back to in-memory cache if Redis is unavailable (development mode).
 * WARNING: In-memory blacklist is NOT distributed - only use for development!
 */
@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);
    private static final String BLACKLIST_PREFIX = "blacklist:token:";

    private final RedisTemplate<String, String> redisTemplate;
    private final Map<String, Long> inMemoryBlacklist = new ConcurrentHashMap<>();
    private boolean redisAvailable = true;

    public TokenBlacklistService(@Autowired(required = false) RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        if (redisTemplate == null) {
            log.warn("⚠️  Redis not configured - using in-memory blacklist (NOT FOR PRODUCTION!)");
            redisAvailable = false;
        } else {
            log.info("✅ Redis configured for token blacklisting");
        }
    }

    public void blacklistToken(String token, long expirationMillis) {
        if (redisAvailable && redisTemplate != null) {
            try {
                String key = BLACKLIST_PREFIX + token;
                redisTemplate.opsForValue().set(key, "blacklisted", expirationMillis, TimeUnit.MILLISECONDS);
                log.debug("Token blacklisted in Redis");
            } catch (Exception e) {
                log.error("Redis error, falling back to in-memory: {}", e.getMessage());
                redisAvailable = false;
                blacklistInMemory(token, expirationMillis);
            }
        } else {
            blacklistInMemory(token, expirationMillis);
        }
    }

    public boolean isTokenBlacklisted(String token) {
        if (redisAvailable && redisTemplate != null) {
            try {
                String key = BLACKLIST_PREFIX + token;
                return Boolean.TRUE.equals(redisTemplate.hasKey(key));
            } catch (Exception e) {
                log.error("Redis error, falling back to in-memory: {}", e.getMessage());
                redisAvailable = false;
                return isTokenBlacklistedInMemory(token);
            }
        } else {
            return isTokenBlacklistedInMemory(token);
        }
    }

    private void blacklistInMemory(String token, long expirationMillis) {
        long expiryTime = System.currentTimeMillis() + expirationMillis;
        inMemoryBlacklist.put(token, expiryTime);
        cleanupExpiredTokens();
    }

    private boolean isTokenBlacklistedInMemory(String token) {
        Long expiryTime = inMemoryBlacklist.get(token);
        if (expiryTime == null) {
            return false;
        }
        if (System.currentTimeMillis() > expiryTime) {
            inMemoryBlacklist.remove(token);
            return false;
        }
        return true;
    }

    private void cleanupExpiredTokens() {
        long now = System.currentTimeMillis();
        inMemoryBlacklist.entrySet().removeIf(entry -> entry.getValue() < now);
    }
}



