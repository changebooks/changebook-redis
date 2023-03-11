package io.github.changebooks.redis;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.List;

/**
 * 分布式限流（令牌桶）
 * 每秒放入n个令牌
 *
 * @author changebooks@qq.com
 */
public final class TokenBucket {
    /**
     * 脚本路径
     */
    private static final String SCRIPT_PATH = "token-bucket.lua";

    /**
     * 脚本命令
     */
    private static final DefaultRedisScript<Long> SCRIPT = new DefaultRedisScript<>();

    static {
        ClassPathResource pathResource = new ClassPathResource(SCRIPT_PATH);
        ResourceScriptSource scriptSource = new ResourceScriptSource(pathResource);

        SCRIPT.setResultType(Long.class);
        SCRIPT.setScriptSource(scriptSource);
    }

    /**
     * 名称
     */
    private final String name;

    /**
     * 最大令牌数（桶容量）
     */
    private int maxPermits;

    /**
     * 每秒放入令牌数
     */
    private int permitsPerSecond;

    /**
     * keys = [name]
     */
    private final List<String> keys;

    /**
     * "maxPermits"
     */
    private String arg1 = "0";

    /**
     * "permitsPerSecond"
     */
    private String arg2 = "0";

    /**
     * {@link StringRedisTemplate}
     */
    private final StringRedisTemplate template;

    /**
     * 创建 {@link TokenBucket} 实例
     *
     * @param template         {@link StringRedisTemplate} 实例
     * @param name             名称
     * @param maxPermits       最大令牌数（桶容量）
     * @param permitsPerSecond 每秒放入令牌数
     * @return {@link TokenBucket} 实例
     */
    public static TokenBucket create(StringRedisTemplate template, String name, int maxPermits, int permitsPerSecond) {
        return new TokenBucket(template, name).
                setMaxPermits(maxPermits).
                setPermitsPerSecond(permitsPerSecond);
    }

    private TokenBucket(StringRedisTemplate template, String name) {
        Assert.notNull(template, "template can't be null");
        Assert.hasText(name, "name can't be empty");

        this.template = template;
        this.name = name;
        this.keys = Collections.singletonList(name);
    }

    /**
     * 取出令牌
     * 剩余令牌数 = 可取令牌数 - 实取令牌数
     *
     * @param permits 待取令牌数
     * @return 实取令牌数 = MIN(待取令牌数, 可取令牌数)
     */
    public Long acquire(int permits) {
        Assert.isTrue(permits > 0, "permits must be greater than 0");

        if (permits > maxPermits) {
            permits = maxPermits;
        }

        String arg3 = String.valueOf(System.currentTimeMillis());
        String arg4 = String.valueOf(permits);

        // 最大令牌数（桶容量）、每秒放入令牌数、当前时间（毫秒）、待取令牌数
        return template.execute(SCRIPT, keys, arg1, arg2, arg3, arg4);
    }

    public String getName() {
        return name;
    }

    public int getMaxPermits() {
        return maxPermits;
    }

    public TokenBucket setMaxPermits(int maxPermits) {
        Assert.isTrue(maxPermits > 0, "maxPermits must be greater than 0");

        this.maxPermits = maxPermits;
        this.arg1 = String.valueOf(maxPermits);
        return this;
    }

    public int getPermitsPerSecond() {
        return permitsPerSecond;
    }

    public TokenBucket setPermitsPerSecond(int permitsPerSecond) {
        Assert.isTrue(permitsPerSecond > 0, "permitsPerSecond must be greater than 0");

        this.permitsPerSecond = permitsPerSecond;
        this.arg2 = String.valueOf(permitsPerSecond);
        return this;
    }

    public List<String> getKeys() {
        return keys;
    }

    public String getArg1() {
        return arg1;
    }

    public String getArg2() {
        return arg2;
    }

    public StringRedisTemplate getTemplate() {
        return template;
    }

}
