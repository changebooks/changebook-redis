package io.github.changebooks.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 通过默认的方法，或重写的子方法，创建 {@link CachePrefixNameTtl} 实例
 *
 * @author changebooks@qq.com
 */
public class CachePrefixNameTtlSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachePrefixNameTtlSupport.class);

    /**
     * 默认的缓存名和缓存过期时间的拼接符
     * 拼接缓存名和过期时间，如，token&P1D, city&PT6H
     */
    private static final String SEPARATOR = "&";

    /**
     * 格式化缓存名前缀、缓存名、缓存过期时间
     *
     * @param cacheProperties {@link CacheProperties} 实例
     * @return {@link CachePrefixNameTtl} 实例
     */
    public CachePrefixNameTtl cachePrefixNameTtl(CacheProperties cacheProperties) {
        if (cacheProperties == null) {
            return null;
        }

        boolean useCacheNamePrefix = true;
        String cacheNamePrefix = null;
        Duration ttl = null;

        CacheProperties.Redis redisProperties = cacheProperties.getRedis();
        if (redisProperties != null) {
            useCacheNamePrefix = redisProperties.isUseKeyPrefix();
            cacheNamePrefix = redisProperties.getKeyPrefix();
            ttl = redisProperties.getTimeToLive();
        }

        List<String> cacheNames = cacheProperties.getCacheNames();
        List<CacheNameTtl> cacheNameTtlList = cacheNameTtlList(cacheNames);

        CachePrefixNameTtl result = new CachePrefixNameTtl();

        result.setUseCacheNamePrefix(useCacheNamePrefix);
        result.setCacheNamePrefix(cacheNamePrefix);
        result.setTtl(ttl);
        result.setCacheNameTtlList(cacheNameTtlList);

        return result;
    }

    /**
     * 批量格式化缓存名和缓存过期时间
     *
     * @param cacheNameTtlList 缓存名拼接过期时间列表
     * @return {@link CacheNameTtl} 实例列表
     */
    public List<CacheNameTtl> cacheNameTtlList(List<String> cacheNameTtlList) {
        return Optional.ofNullable(cacheNameTtlList).
                orElse(Collections.emptyList()).
                stream().
                map(this::cacheNameTtl).
                filter(Objects::nonNull).
                collect(Collectors.toList());
    }

    /**
     * 格式化缓存名和缓存过期时间
     *
     * @param cacheNameTtl 缓存名拼接过期时间
     * @return {@link CacheNameTtl} 实例
     */
    public CacheNameTtl cacheNameTtl(String cacheNameTtl) {
        if (cacheNameTtl == null) {
            return null;
        }

        String cacheName;
        Duration ttl;

        String separator = separator();
        if (StringUtils.hasLength(separator) && cacheNameTtl.contains(separator)) {
            String[] splitR = cacheNameTtl.split(separator);
            if (splitR.length <= 0) {
                return null;
            }

            cacheName = splitR[0];
            if (splitR.length > 1) {
                String rawTtl = splitR[1];
                ttl = parseTtl(rawTtl);
            } else {
                ttl = null;
            }
        } else {
            cacheName = cacheNameTtl;
            ttl = null;
        }

        return new CacheNameTtl(cacheName, ttl);
    }

    /**
     * 时间格式转换，字符串 to 时间对象
     *
     * @param ttl 时间字符串
     * @return 时间对象，EmptyOrNull时返回Null，转换失败时抛出异常
     */
    public Duration parseTtl(String ttl) {
        String cleanedTtl = StringUtils.trimAllWhitespace(ttl);
        if (StringUtils.hasText(cleanedTtl)) {
            try {
                return Duration.parse(cleanedTtl);
            } catch (DateTimeParseException ex) {
                LOGGER.error("parseTtl failed, ttl: {}, cleanedTtl: {}, throwable: ", ttl, cleanedTtl, ex);
                throw ex;
            }
        } else {
            return null;
        }
    }

    /**
     * 自定义的缓存名和缓存过期时间的拼接符
     *
     * @return 自定义的拼接符，EmptyOrNull时无过期时间
     * @see #SEPARATOR
     */
    public String separator() {
        return SEPARATOR;
    }

}
