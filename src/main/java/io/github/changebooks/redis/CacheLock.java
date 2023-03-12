package io.github.changebooks.redis;

import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.util.Assert;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁
 *
 * @author changebooks@qq.com
 */
public final class CacheLock {
    /**
     * 默认的令牌拼接符
     * 拼接令牌，如，"客户端id-线程id"
     */
    private static final String SEPARATOR = "-";

    /**
     * 解锁命令
     */
    public static final byte[] UNLOCK_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end".getBytes();

    /**
     * 续期命令
     */
    public static final byte[] RENEWAL_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('pexpire', KEYS[1], ARGV[2]) else return 0 end".getBytes();

    /**
     * 锁名
     */
    private final byte[] name;

    /**
     * 解锁和续期的令牌
     */
    private String token;

    /**
     * {@link StringRedisTemplate}
     */
    private final StringRedisTemplate template;

    /**
     * 创建 {@link CacheLock} 实例
     *
     * @param template {@link StringRedisTemplate} 实例
     * @param name     锁名
     * @param token    解锁和续期的令牌，如，客户端id
     * @return {@link CacheLock} 实例
     */
    public static CacheLock create(StringRedisTemplate template, String name, String token) {
        return new CacheLock(template, name).
                setToken(token);
    }

    private CacheLock(StringRedisTemplate template, String name) {
        Assert.notNull(template, "template can't be null");
        Assert.hasText(name, "name can't be empty");

        this.template = template;
        this.name = name.getBytes();
    }

    /**
     * 加锁
     *
     * @param expirationTime 过期时间
     * @param timeUnit       过期时间的单位
     * @return 加锁成功？
     */
    public Boolean lock(long expirationTime, TimeUnit timeUnit) {
        Assert.isTrue(expirationTime > 0, "expirationTime must be greater than 0");

        Expiration expiredAt = Expiration.from(expirationTime, timeUnit);
        return template.execute((RedisCallback<Boolean>) conn -> conn.set(
                name,
                token(),
                expiredAt,
                RedisStringCommands.SetOption.SET_IF_ABSENT));
    }

    /**
     * 解锁
     *
     * @return 解锁成功？
     */
    public Boolean unlock() {
        return template.execute((RedisCallback<Boolean>) conn -> conn.eval(
                UNLOCK_SCRIPT,
                ReturnType.BOOLEAN,
                1,
                name,
                token()));
    }

    /**
     * 续期
     *
     * @param expirationTime 过期时间
     * @param timeUnit       过期时间的单位
     * @return 续期成功？
     */
    public Boolean renewal(long expirationTime, TimeUnit timeUnit) {
        Assert.isTrue(expirationTime > 0, "expirationTime must be greater than 0");

        long expirationTimeMs = Expiration.from(expirationTime, timeUnit).getExpirationTimeInMilliseconds();
        String expiredAt = Long.toString(expirationTimeMs);

        return template.execute((RedisCallback<Boolean>) conn -> conn.eval(
                RENEWAL_SCRIPT,
                ReturnType.BOOLEAN,
                1,
                name,
                token(),
                expiredAt.getBytes()));
    }

    /**
     * 定时续期
     * 关注时间轮配置
     *
     * @param delayTime      延迟时间
     * @param expirationTime 过期时间
     * @param timeUnit       时间单位
     * @see TimeoutScheduler
     */
    public void scheduleRenewal(long delayTime, long expirationTime, TimeUnit timeUnit) {
        Assert.isTrue(delayTime > 0, "delayTime must be greater than 0");
        Assert.isTrue(expirationTime > 0, "expirationTime must be greater than 0");

        TimeoutScheduler.newTimeout(timeout -> {
            Boolean r = renewal(expirationTime, timeUnit);
            if (r != null && r) {
                scheduleRenewal(delayTime, expirationTime, timeUnit);
            }
        }, delayTime, timeUnit);
    }

    /**
     * 格式化令牌，解锁和续期的令牌
     *
     * @return 格式化后的令牌，如，"客户端id-线程id"
     */
    public byte[] token() {
        long threadId = Thread.currentThread().getId();
        String token = getToken() + SEPARATOR + threadId;
        return token.getBytes();
    }

    public byte[] getName() {
        return name;
    }

    public String getToken() {
        return token;
    }

    public CacheLock setToken(String token) {
        Assert.hasText(token, "token can't be empty");

        this.token = token;
        return this;
    }

    public StringRedisTemplate getTemplate() {
        return template;
    }

}
