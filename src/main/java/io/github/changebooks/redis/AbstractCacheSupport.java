package io.github.changebooks.redis;

import org.springframework.util.StringUtils;

/**
 * 通过默认的方法，或重写的子方法，创建实例
 *
 * @author changebooks@qq.com
 */
public abstract class AbstractCacheSupport {
    /**
     * 拼接缓存名前缀？
     */
    private final boolean useCacheNamePrefix;

    /**
     * 缓存名前缀
     */
    private final String cacheNamePrefix;

    public AbstractCacheSupport(boolean useCacheNamePrefix, String cacheNamePrefix) {
        this.useCacheNamePrefix = useCacheNamePrefix;
        this.cacheNamePrefix = cacheNamePrefix(cacheNamePrefix);
    }

    /**
     * 拼接缓存名前缀和缓存名
     *
     * @param cacheName 格式化后的缓存名
     * @return 拼接缓存名前缀？ 缓存名前缀 + 缓存名，否则无前缀的缓存名
     */
    public String prefixedCacheName(String cacheName) {
        if (isUseCacheNamePrefix()) {
            String cacheNamePrefix = getCacheNamePrefix();
            if (StringUtils.hasLength(cacheNamePrefix)) {
                return cacheNamePrefix + cacheName;
            } else {
                return cacheName;
            }
        } else {
            return cacheName;
        }
    }

    /**
     * 格式化缓存名前缀
     *
     * @param cacheNamePrefix 未格式化的前缀
     * @return 格式化后的前缀
     */
    public String cacheNamePrefix(String cacheNamePrefix) {
        return StringUtils.trimAllWhitespace(cacheNamePrefix);
    }

    /**
     * 格式化缓存名
     *
     * @param cacheName 未格式化的缓存名
     * @return 格式化后的缓存名
     */
    public String cacheName(String cacheName) {
        return StringUtils.trimAllWhitespace(cacheName);
    }

    public boolean isUseCacheNamePrefix() {
        return useCacheNamePrefix;
    }

    public String getCacheNamePrefix() {
        return cacheNamePrefix;
    }

}
