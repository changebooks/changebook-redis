package io.github.changebooks.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 通过默认的方法，或重写的子方法，创建实例
 * <pre>
 * {@link CacheLock}
 * {@link RateLimiter}
 * {@link TokenBucket}
 * </pre>
 *
 * @author changebooks@qq.com
 */
public class CacheDistributedSupport extends AbstractCacheSupport {
    /**
     * 执行模板
     */
    private final StringRedisTemplate stringRedisTemplate;

    public CacheDistributedSupport(StringRedisTemplate stringRedisTemplate,
                                   @NonNull CachePrefixNameTtl cachePrefixNameTtl) {
        super(cachePrefixNameTtl.isUseCacheNamePrefix(), cachePrefixNameTtl.getCacheNamePrefix());
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public CacheDistributedSupport(StringRedisTemplate stringRedisTemplate) {
        super(true, null);
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public CacheDistributedSupport(StringRedisTemplate stringRedisTemplate,
                                   boolean useCacheNamePrefix,
                                   String cacheNamePrefix) {
        super(useCacheNamePrefix, cacheNamePrefix);
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 分布式锁
     *
     * @param cacheName 缓存名
     * @param token     解锁和续期的令牌
     * @return {@link CacheLock} 实例
     */
    public CacheLock cacheLock(String cacheName, String token) {
        String cleanedCacheName = cacheName(cacheName);
        Assert.hasText(cleanedCacheName, "cleanedCacheName can't be empty");

        StringRedisTemplate stringRedisTemplate = getStringRedisTemplate();
        String prefixedCacheName = prefixedCacheName(cleanedCacheName);
        String cleanedToken = token(token);

        return CacheLock.create(stringRedisTemplate, prefixedCacheName, cleanedToken);
    }

    /**
     * 分布式限流（固定时间窗口）
     *
     * @param cacheName 缓存名
     * @param seconds   总秒数（x秒内）
     * @param permits   总许可数（许可n次）
     * @return {@link RateLimiter} 实例
     */
    public RateLimiter rateLimiter(String cacheName, int seconds, int permits) {
        String cleanedCacheName = cacheName(cacheName);
        Assert.hasText(cleanedCacheName, "cleanedCacheName can't be empty");

        StringRedisTemplate stringRedisTemplate = getStringRedisTemplate();
        String prefixedCacheName = prefixedCacheName(cleanedCacheName);

        return RateLimiter.create(stringRedisTemplate, prefixedCacheName, seconds, permits);
    }

    /**
     * 分布式限流（令牌桶）
     *
     * @param cacheName        缓存名
     * @param maxPermits       最大令牌数（桶容量）
     * @param permitsPerSecond 每秒放入令牌数
     * @return {@link TokenBucket} 实例
     */
    public TokenBucket tokenBucket(String cacheName, int maxPermits, int permitsPerSecond) {
        String cleanedCacheName = cacheName(cacheName);
        Assert.hasText(cleanedCacheName, "cleanedCacheName can't be empty");

        StringRedisTemplate stringRedisTemplate = getStringRedisTemplate();
        String prefixedCacheName = prefixedCacheName(cleanedCacheName);

        return TokenBucket.create(stringRedisTemplate, prefixedCacheName, maxPermits, permitsPerSecond);
    }

    /**
     * 格式化令牌
     *
     * @param token 未格式化的令牌
     * @return 格式化后的令牌
     */
    public String token(String token) {
        return StringUtils.trimAllWhitespace(token);
    }

    public StringRedisTemplate getStringRedisTemplate() {
        return stringRedisTemplate;
    }

}
