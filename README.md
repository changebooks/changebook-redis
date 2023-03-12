# changebook-redis
### 分布式锁、分布式限流、注解

### pom.xml
```
<dependency>
    <groupId>io.github.changebooks</groupId>
    <artifactId>changebook-redis</artifactId>
    <version>1.1.3</version>
</dependency>
```

### application.yml
```
spring:
  application:
    name: sample
  redis:
    host: 127.0.0.1
    port: 6379
    database: 0
    password:
    timeout: 3s
    lettuce:
      pool:
        max-active: 200
        max-idle: 80
        min-idle: 0
        max-wait: -1
  cache:
    cache-names: 缓存名1&缓存过期时间1, 缓存名2&缓存过期时间2（如，token&P1D, city&PT6H）
    redis:
      use-key-prefix: true
      key-prefix: 缓存名前缀（建议，"${spring.application.name}::"）
      time-to-live: 缓存过期时间（如，PT1M）
```

### 实现 CachingConfigurerSupport
```
@Configuration
@EnableCaching
@EnableConfigurationProperties({CacheProperties.class})
public class CachingConfigurerSupportImpl extends CachingConfigurerSupport {

    @Resource
    private RedisConnectionFactory redisConnectionFactory;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheProperties cacheProperties;

    @Bean
    public CacheLock cacheLock(CacheDistributedSupport cacheDistributedSupport) {
        return cacheDistributedSupport.cacheLock("缓存名", "解锁和续期的令牌");
    }

    @Bean
    public RateLimiter rateLimiter(CacheDistributedSupport cacheDistributedSupport) {
        return cacheDistributedSupport.rateLimiter("缓存名", 总秒数（x秒内）, 总许可数（许可n次）);
    }

    @Bean
    public TokenBucket tokenBucket(CacheDistributedSupport cacheDistributedSupport) {
        return cacheDistributedSupport.tokenBucket("缓存名", 最大令牌数（桶容量）, 每秒放入令牌数);
    }

    @Bean
    public CacheManager cacheManager(CacheManagerSupport cacheManagerSupport) {
        return cacheManagerSupport.cacheManager();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheExceptionHandler();
    }

    @Bean
    public CacheManagerSupport cacheManagerSupport(CachePrefixNameTtl cachePrefixNameTtl) {
        if (cachePrefixNameTtl == null) {
            return new CacheManagerSupport(redisConnectionFactory);
        } else {
            return new CacheManagerSupport(redisConnectionFactory, cachePrefixNameTtl);
        }
    }

    @Bean
    public CacheDistributedSupport cacheDistributedSupport(CachePrefixNameTtl cachePrefixNameTtl) {
        if (cachePrefixNameTtl == null) {
            return new CacheDistributedSupport(stringRedisTemplate);
        } else {
            return new CacheDistributedSupport(stringRedisTemplate, cachePrefixNameTtl);
        }
    }

    @Bean
    public CachePrefixNameTtl cachePrefixNameTtl(CachePrefixNameTtlSupport cachePrefixNameTtlSupport) {
        return cachePrefixNameTtlSupport.cachePrefixNameTtl(cacheProperties);
    }

    @Bean
    public CachePrefixNameTtlSupport cachePrefixNameTtlSupport() {
        return new CachePrefixNameTtlSupport();
    }

}
```

### 注解
```
// 配置注解
@CacheConfig(cacheNames = "缓存名")

// 存取缓存
@Cacheable(key = "#id")

// 删除缓存
@CacheEvict(key = "#record.id")
```

### 分布式锁
```
// 加锁
boolean 加锁成功？ = cacheLock.lock(过期时间, 时间单位);

// 解锁
boolean 解锁成功？ = cacheLock.unlock();

// 续期
boolean 续期成功？ = cacheLock.renewal(过期时间, 时间单位);

// 定时续期
cacheLock.scheduleRenewal(延迟时间, 过期时间, 时间单位);
```

### 分布式限流，固定时间窗口，x秒内，许可n次
```
// 获取许可
Boolean 得到许可？ = rateLimiter.acquire();
```

### 分布式限流，令牌桶，每秒放入n个令牌
```
// 取出令牌
Long 实取令牌数 = tokenBucket.acquire(待取令牌数);
```
