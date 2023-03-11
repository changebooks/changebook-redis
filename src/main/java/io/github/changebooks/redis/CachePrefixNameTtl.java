package io.github.changebooks.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.DurationDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.DurationSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.time.Duration;
import java.util.List;

/**
 * 缓存名前缀、缓存名、缓存过期时间
 *
 * @author changebooks@qq.com
 */
public final class CachePrefixNameTtl implements Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachePrefixNameTtl.class);

    /**
     * 解析json
     */
    private static final ObjectMapper JSON_PARSER = new ObjectMapper();

    /**
     * 拼接缓存名前缀？
     */
    private boolean useCacheNamePrefix;

    /**
     * 缓存名前缀
     */
    private String cacheNamePrefix;

    /**
     * 过期时间
     */
    @JsonSerialize(using = DurationSerializer.class)
    @JsonDeserialize(using = DurationDeserializer.class)
    private Duration ttl;

    /**
     * 缓存名和过期时间
     */
    private List<CacheNameTtl> cacheNameTtlList;

    @Override
    public String toString() {
        try {
            return JSON_PARSER.writeValueAsString(this);
        } catch (JsonProcessingException ex) {
            LOGGER.error("parseJson failed, useCacheNamePrefix: {}, cacheNamePrefix: {}, ttl: {}, throwable: ",
                    useCacheNamePrefix, cacheNamePrefix, ttl, ex);
            return null;
        }
    }

    public boolean isUseCacheNamePrefix() {
        return useCacheNamePrefix;
    }

    public void setUseCacheNamePrefix(boolean useCacheNamePrefix) {
        this.useCacheNamePrefix = useCacheNamePrefix;
    }

    public String getCacheNamePrefix() {
        return cacheNamePrefix;
    }

    public void setCacheNamePrefix(String cacheNamePrefix) {
        this.cacheNamePrefix = cacheNamePrefix;
    }

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }

    public List<CacheNameTtl> getCacheNameTtlList() {
        return cacheNameTtlList;
    }

    public void setCacheNameTtlList(List<CacheNameTtl> cacheNameTtlList) {
        this.cacheNameTtlList = cacheNameTtlList;
    }

}
