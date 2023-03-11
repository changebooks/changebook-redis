-- 分布式限流（令牌桶）
-- 每秒放入n个令牌

-- 名称
local name = KEYS[1]

-- 最大令牌数（桶容量）
local maxPermits = tonumber(ARGV[1])

-- 每秒放入令牌数
local permitsPerSecond = tonumber(ARGV[2])

-- 当前时间，单位：毫秒
local nowTime = tonumber(ARGV[3])

-- 待取令牌数
local acquirePermits = tonumber(ARGV[4])

-- 上次取令牌时间，单位：毫秒
local lastTime = 0

-- 上次取令牌后，剩余可取令牌数
local lastAvailablePermits = 0

-- 上次取令牌时间:上次取令牌后的剩余可取令牌数
local lastTimeAndAvailablePermits = tostring(redis.call('get', name))
if lastTimeAndAvailablePermits ~= 'false' then
    local index = string.find(lastTimeAndAvailablePermits, ':')
    if index ~= nil and index > 0 then
        lastTime = tonumber(string.sub(lastTimeAndAvailablePermits, 0, index - 1))
        if lastTime == nil or lastTime < 0 then
            lastTime = 0
        end

        lastAvailablePermits = tonumber(string.sub(lastTimeAndAvailablePermits, index + 1))
        if lastAvailablePermits == nil or lastAvailablePermits < 0 then
            lastAvailablePermits = 0
        end
    end
end

if lastTime <= 0 or lastTime > nowTime then
    -- 首次取令牌
    -- 或
    -- 时钟回拨，Clock Backward
    lastTime = nowTime
end

-- 从上次取令牌到现在，经历时长，单位：毫秒
local elapsedTime = nowTime - lastTime

-- 从上次取令牌到现在，放入令牌数
local putPermits = math.floor(permitsPerSecond * elapsedTime / 1000)

-- 本次可取令牌数
local availablePermits = lastAvailablePermits + putPermits
if availablePermits > maxPermits then
    -- 满桶
    availablePermits = maxPermits
end

if availablePermits < 0 then
    -- 取前是空桶
    availablePermits = 0
end

-- 实取令牌数 = MIN(待取令牌数, 可取令牌数)
if acquirePermits > availablePermits then
    acquirePermits = availablePermits
end

-- 本次取令牌后，剩余可取令牌数
availablePermits = availablePermits - acquirePermits
if availablePermits < 0 then
    -- 取后成空桶
    availablePermits = 0
end

local nowTimeAndAvailablePermits = table.concat({ nowTime, ':', availablePermits })
redis.call('set', name, nowTimeAndAvailablePermits)

-- 实取令牌数
return acquirePermits
