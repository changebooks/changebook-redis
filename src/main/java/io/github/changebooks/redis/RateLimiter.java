package io.github.changebooks.redis;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.List;

/**
 * 分布式限流（固定时间窗口）
 * x秒内，许可n次
 *
 * @author changebooks@qq.com
 */
public final class RateLimiter {
    /**
     * 脚本路径
     */
    private static final String SCRIPT_PATH = "rate-limiter.lua";

    /**
     * 脚本命令
     */
    private static final DefaultRedisScript<Boolean> SCRIPT = new DefaultRedisScript<>();

    static {
        ClassPathResource pathResource = new ClassPathResource(SCRIPT_PATH);
        ResourceScriptSource scriptSource = new ResourceScriptSource(pathResource);

        SCRIPT.setResultType(Boolean.class);
        SCRIPT.setScriptSource(scriptSource);
    }

    /**
     * 名称
     */
    private final String name;

    /**
     * 总秒数（x秒内）
     */
    private int seconds;

    /**
     * 总许可数（许可n次）
     */
    private int permits;

    /**
     * keys = [name]
     */
    private final List<String> keys;

    /**
     * args = ["seconds", "permits"]
     */
    private final Object[] args = {"0", "0"};

    /**
     * {@link StringRedisTemplate}
     */
    private final StringRedisTemplate template;

    /**
     * 创建 {@link RateLimiter} 实例
     *
     * @param template {@link StringRedisTemplate} 实例
     * @param name     名称
     * @param seconds  总秒数（x秒内）
     * @param permits  总许可数（许可n次）
     * @return {@link RateLimiter} 实例
     */
    public static RateLimiter create(StringRedisTemplate template, String name, int seconds, int permits) {
        return new RateLimiter(template, name).
                setSeconds(seconds).
                setPermits(permits);
    }

    private RateLimiter(StringRedisTemplate template, String name) {
        Assert.notNull(template, "template can't be null");
        Assert.hasText(name, "name can't be empty");

        this.template = template;
        this.name = name;
        this.keys = Collections.singletonList(name);
    }

    /**
     * 获取许可
     *
     * @return 得到许可？
     */
    public Boolean acquire() {
        return template.execute(SCRIPT, keys, args);
    }

    public String getName() {
        return name;
    }

    public int getSeconds() {
        return seconds;
    }

    public RateLimiter setSeconds(int seconds) {
        Assert.isTrue(seconds > 0, "seconds must be greater than 0");

        this.seconds = seconds;
        this.args[0] = String.valueOf(seconds);
        return this;
    }

    public int getPermits() {
        return permits;
    }

    public RateLimiter setPermits(int permits) {
        Assert.isTrue(permits > 0, "permits must be greater than 0");

        this.permits = permits;
        this.args[1] = String.valueOf(permits);
        return this;
    }

    public List<String> getKeys() {
        return keys;
    }

    public Object[] getArgs() {
        return args;
    }

    public StringRedisTemplate getTemplate() {
        return template;
    }

}
