-- 分布式限流（固定时间窗口）
-- x秒内，许可n次

-- 名称
local name = KEYS[1]

-- 总秒数（x秒内）
local totalSeconds = tonumber(ARGV[1])

-- 总许可数（许可n次）
local totalPermits = tonumber(ARGV[2])

-- 已许可数
local usedPermits = tonumber(redis.call('get', name))
if usedPermits == nil then
    usedPermits = 0
end

if usedPermits >= totalPermits then
    -- x秒内，已许可n次
    -- 拒绝请求，等下x秒
    return false
end

-- 已许可数 + 1
usedPermits = redis.call('incr', name)

if usedPermits == 1 or redis.call('ttl', name) == -1 then
    -- x秒内，首次许可
    -- 或
    -- 未设过期时间
    redis.call('expire', name, totalSeconds)
end

if usedPermits > totalPermits then
    -- x秒内，已许可n次
    -- 拒绝请求，等下x秒
    return false
else
    -- 通过请求
    return true
end
