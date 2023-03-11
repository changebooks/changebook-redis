package io.github.changebooks.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

/**
 * 统一异常处理
 * 默认处理方式，错误日志
 *
 * @author changebooks@qq.com
 */
public class CacheExceptionHandler implements CacheErrorHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheExceptionHandler.class);

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        String cacheName = cache.getName();
        LOGGER.error("handleCacheGetError, cacheName: {}, key: {}, throwable: ",
                cacheName, key, exception);
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        String cacheName = cache.getName();
        LOGGER.error("handleCachePutError, cacheName: {}, key: {}, value: {}, throwable: ",
                cacheName, key, value, exception);
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        String cacheName = cache.getName();
        LOGGER.error("handleCacheEvictError, cacheName: {}, key: {}, throwable: ",
                cacheName, key, exception);
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        String cacheName = cache.getName();
        LOGGER.error("handleCacheClearError, cacheName: {}, throwable: ",
                cacheName, exception);
    }

}
