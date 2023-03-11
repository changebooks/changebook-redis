package io.github.changebooks.redis;

import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.CacheKeyPrefix;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 通过默认的方法，或重写的子方法，创建实例
 * <pre>
 * {@link CacheManager}
 * {@link RedisCacheWriter}
 * {@link RedisCacheConfiguration}
 * </pre>
 *
 * @author changebooks@qq.com
 */
public class CacheManagerSupport extends AbstractCacheSupport {
    /**
     * 连接工厂
     */
    private final RedisConnectionFactory redisConnectionFactory;

    /**
     * 默认的缓存过期时间
     */
    private final Duration ttl;

    /**
     * 缓存名和过期时间列表
     */
    private final List<CacheNameTtl> cacheNameTtlList;

    public CacheManagerSupport(RedisConnectionFactory redisConnectionFactory,
                               @NonNull CachePrefixNameTtl cachePrefixNameTtl) {
        super(cachePrefixNameTtl.isUseCacheNamePrefix(), cachePrefixNameTtl.getCacheNamePrefix());

        this.redisConnectionFactory = redisConnectionFactory;
        this.ttl = cachePrefixNameTtl.getTtl();
        this.cacheNameTtlList = cacheNameTtlList(cachePrefixNameTtl.getCacheNameTtlList());
    }

    public CacheManagerSupport(RedisConnectionFactory redisConnectionFactory) {
        super(true, null);

        this.redisConnectionFactory = redisConnectionFactory;
        this.ttl = null;
        this.cacheNameTtlList = null;
    }

    public CacheManagerSupport(RedisConnectionFactory redisConnectionFactory,
                               boolean useCacheNamePrefix,
                               String cacheNamePrefix,
                               Duration ttl,
                               List<CacheNameTtl> cacheNameTtlList) {
        super(useCacheNamePrefix, cacheNamePrefix);

        this.redisConnectionFactory = redisConnectionFactory;
        this.ttl = ttl;
        this.cacheNameTtlList = cacheNameTtlList(cacheNameTtlList);
    }

    /**
     * 创建 {@link CacheManager} 实例
     *
     * @return {@link CacheManager} 实例
     */
    public CacheManager cacheManager() {
        return cacheManager(
                redisCacheWriter(),
                redisCacheConfiguration(),
                redisCacheConfigurationMap());
    }

    /**
     * 创建 {@link CacheManager} 实例
     *
     * @param redisCacheWriter               {@link RedisCacheWriter} 实例
     * @param defaultRedisCacheConfiguration {@link RedisCacheConfiguration} 实例
     * @param redisCacheConfigurationMap     [ 缓存名 : {@link RedisCacheConfiguration} 实例 ]
     * @return {@link CacheManager} 实例
     */
    public CacheManager cacheManager(RedisCacheWriter redisCacheWriter,
                                     RedisCacheConfiguration defaultRedisCacheConfiguration,
                                     Map<String, RedisCacheConfiguration> redisCacheConfigurationMap) {
        RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager.builder(redisCacheWriter).
                cacheDefaults(defaultRedisCacheConfiguration);

        if (CollectionUtils.isEmpty(redisCacheConfigurationMap)) {
            return builder.build();
        }

        Set<String> cacheNameSet = redisCacheConfigurationMap.keySet();
        return builder.
                initialCacheNames(cacheNameSet).
                withInitialCacheConfigurations(redisCacheConfigurationMap).
                build();
    }

    /**
     * 创建 {@link RedisCacheWriter} 实例
     *
     * @return {@link RedisCacheWriter} 实例
     */
    public RedisCacheWriter redisCacheWriter() {
        RedisConnectionFactory redisConnectionFactory = getRedisConnectionFactory();
        return RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory);
    }

    /**
     * 创建 {@link RedisCacheConfiguration} 实例
     *
     * @return {@link RedisCacheConfiguration} 实例
     * @see #redisCacheConfiguration(Duration)
     */
    public RedisCacheConfiguration redisCacheConfiguration() {
        Duration ttl = getTtl();
        return redisCacheConfiguration(ttl);
    }

    /**
     * 批量创建 {@link RedisCacheConfiguration} 实例
     *
     * @return [ 缓存名 : {@link RedisCacheConfiguration} 实例 ]
     * @see #redisCacheConfiguration(Duration)
     */
    public Map<String, RedisCacheConfiguration> redisCacheConfigurationMap() {
        List<CacheNameTtl> cacheNameTtlList = getCacheNameTtlList();
        if (cacheNameTtlList == null) {
            return null;
        }

        Duration defaultTtl = getTtl();
        Map<String, RedisCacheConfiguration> result = new HashMap<>(cacheNameTtlList.size());

        for (CacheNameTtl cacheNameTtl : cacheNameTtlList) {
            if (cacheNameTtl == null) {
                continue;
            }

            String cacheName = cacheNameTtl.getCacheName();
            Duration ttl = cacheNameTtl.getTtl();

            if (StringUtils.hasText(cacheName)) {
                Duration chosenTtl = Optional.ofNullable(ttl).orElse(defaultTtl);
                RedisCacheConfiguration configuration = redisCacheConfiguration(chosenTtl);
                result.put(cacheName, configuration);
            }
        }

        return result;
    }

    /**
     * 创建 {@link RedisCacheConfiguration} 实例
     * 设置1，缓存key的序列化适配器
     * 设置2，缓存value的序列化适配器
     * 设置3，拼接缓存名前缀、缓存名和缓存键的函数接口
     * 设置4，缓存过期时间
     *
     * @param ttl 缓存过期时间
     * @return {@link RedisCacheConfiguration} 实例
     */
    public RedisCacheConfiguration redisCacheConfiguration(Duration ttl) {
        RedisCacheConfiguration result = RedisCacheConfiguration.defaultCacheConfig();

        RedisSerializationContext.SerializationPair<String> keySerializationPair = keySerializationPair();
        if (keySerializationPair != null) {
            result = result.serializeKeysWith(keySerializationPair);
        }

        RedisSerializationContext.SerializationPair<?> valueSerializationPair = valueSerializationPair();
        if (valueSerializationPair != null) {
            result = result.serializeValuesWith(valueSerializationPair);
        }

        CacheKeyPrefix cacheKeyPrefix = cacheKeyPrefix();
        if (cacheKeyPrefix != null) {
            result = result.computePrefixWith(cacheKeyPrefix);
        }

        if (ttl != null) {
            result = result.entryTtl(ttl);
        }

        return result;
    }

    /**
     * 缓存key的序列化适配器
     *
     * @return {@link RedisSerializationContext.SerializationPair} 实例
     */
    public RedisSerializationContext.SerializationPair<String> keySerializationPair() {
        RedisSerializer<String> keySerializer = keySerializer();
        if (keySerializer == null) {
            return null;
        } else {
            return RedisSerializationContext.SerializationPair.fromSerializer(keySerializer);
        }
    }

    /**
     * 缓存value的序列化适配器
     *
     * @return {@link RedisSerializationContext.SerializationPair} 实例
     */
    public RedisSerializationContext.SerializationPair<?> valueSerializationPair() {
        RedisSerializer<?> valueSerializer = valueSerializer();
        if (valueSerializer == null) {
            return null;
        } else {
            return RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer);
        }
    }

    /**
     * 序列化缓存key
     * 默认处理方式，转字符串
     *
     * @return {@link RedisSerializer} 实例
     */
    public RedisSerializer<String> keySerializer() {
        return RedisSerializer.string();
    }

    /**
     * 序列化缓存value
     * 默认处理方式，转json
     *
     * @return {@link RedisSerializer} 实例
     */
    public RedisSerializer<?> valueSerializer() {
        return RedisSerializer.json();
    }

    /**
     * 拼接缓存名前缀、缓存名和缓存键的函数接口
     *
     * @return {@link CacheKeyPrefix} 函数接口
     */
    public CacheKeyPrefix cacheKeyPrefix() {
        boolean useCacheNamePrefix = isUseCacheNamePrefix();
        String cacheNamePrefix = getCacheNamePrefix();
        return KeyPrefix.of(useCacheNamePrefix, cacheNamePrefix);
    }

    /**
     * 批量格式化缓存名和过期时间
     *
     * @param cacheNameTtlList 未格式化的缓存名和过期时间列表
     * @return 格式化后的缓存名和过期时间列表
     */
    public List<CacheNameTtl> cacheNameTtlList(List<CacheNameTtl> cacheNameTtlList) {
        return Optional.ofNullable(cacheNameTtlList).
                orElse(Collections.emptyList()).
                stream().
                map(this::cacheNameTtl).
                filter(Objects::nonNull).
                collect(Collectors.toList());
    }

    /**
     * 格式化缓存名和过期时间
     *
     * @param cacheNameTtl 未格式化的缓存名和过期时间
     * @return 格式化后的缓存名和过期时间
     */
    public CacheNameTtl cacheNameTtl(CacheNameTtl cacheNameTtl) {
        if (cacheNameTtl == null) {
            return null;
        }

        String cacheName = cacheNameTtl.getCacheName();
        Duration ttl = cacheNameTtl.getTtl();

        String cleanedCacheName = cacheName(cacheName);
        if (StringUtils.hasText(cleanedCacheName)) {
            return new CacheNameTtl(cleanedCacheName, ttl);
        } else {
            return null;
        }
    }

    public RedisConnectionFactory getRedisConnectionFactory() {
        return redisConnectionFactory;
    }

    public Duration getTtl() {
        return ttl;
    }

    public List<CacheNameTtl> getCacheNameTtlList() {
        return cacheNameTtlList;
    }

}
