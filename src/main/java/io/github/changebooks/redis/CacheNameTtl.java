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

/**
 * 缓存名和缓存过期时间
 *
 * @author changebooks@qq.com
 */
public final class CacheNameTtl implements Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheNameTtl.class);

    /**
     * 解析json
     */
    private static final ObjectMapper JSON_PARSER = new ObjectMapper();

    /**
     * 缓存名
     */
    private String cacheName;

    /**
     * 过期时间
     */
    @JsonSerialize(using = DurationSerializer.class)
    @JsonDeserialize(using = DurationDeserializer.class)
    private Duration ttl;

    public CacheNameTtl() {
    }

    public CacheNameTtl(String cacheName, Duration ttl) {
        this.cacheName = cacheName;
        this.ttl = ttl;
    }

    @Override
    public String toString() {
        try {
            return JSON_PARSER.writeValueAsString(this);
        } catch (JsonProcessingException ex) {
            LOGGER.error("parseJson failed, cacheName: {}, ttl: {}, throwable: ", cacheName, ttl, ex);
            return null;
        }
    }

    public String getCacheName() {
        return cacheName;
    }

    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }

}
