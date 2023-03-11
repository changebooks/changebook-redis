package io.github.changebooks.redis;

import org.springframework.data.redis.cache.CacheKeyPrefix;
import org.springframework.util.StringUtils;

/**
 * 拼接缓存名前缀、缓存名和缓存键的函数接口
 *
 * @author changebooks@qq.com
 */
public interface KeyPrefix {
    /**
     * 默认的缓存名和缓存键的拼接符
     */
    String SEPARATOR = ":";

    /**
     * 指定是否拼接缓存名前缀
     * 拼接前缀，如，"cacheNamePrefix::cacheName:"
     * 不拼前缀，如，"cacheName:"
     *
     * @param useCacheNamePrefix 拼接缓存名前缀？
     * @param cacheNamePrefix    缓存名前缀
     * @return {@link CacheKeyPrefix} 函数接口
     */
    static CacheKeyPrefix of(boolean useCacheNamePrefix, String cacheNamePrefix) {
        if (useCacheNamePrefix) {
            return prefixed(cacheNamePrefix);
        } else {
            return simple();
        }
    }

    /**
     * 有缓存名前缀
     * 缓存名前缀 + 缓存名 + 缓存名和缓存键的拼接符
     * 如，"cacheNamePrefix::cacheName:"
     *
     * @param cacheNamePrefix 缓存名前缀
     * @return {@link CacheKeyPrefix} 函数接口
     */
    static CacheKeyPrefix prefixed(String cacheNamePrefix) {
        if (StringUtils.hasText(cacheNamePrefix)) {
            return name -> cacheNamePrefix + name + SEPARATOR;
        } else {
            return simple();
        }
    }

    /**
     * 无缓存名前缀
     * 缓存名 + 缓存名和缓存键的拼接符
     * 如，"cacheName:"
     *
     * @return {@link CacheKeyPrefix} 函数接口
     */
    static CacheKeyPrefix simple() {
        return name -> name + SEPARATOR;
    }

}
