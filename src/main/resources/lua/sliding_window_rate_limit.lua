local key = KEYS[1]
local now = tonumber(ARGV[1])
local window_seconds = tonumber(ARGV[2])
local max_requests = tonumber(ARGV[3])
local window_start = now - (window_seconds * 1000)
local counter_key = key .. ':counter'

redis.call('ZREMRANGEBYSCORE', key, 0, window_start)

local current = redis.call('ZCARD', key)
if current >= max_requests then
    redis.call('EXPIRE', key, window_seconds)
    redis.call('EXPIRE', counter_key, window_seconds)
    return 0
end

local member_seq = redis.call('INCR', counter_key)
local member = tostring(now) .. '-' .. tostring(member_seq)
redis.call('ZADD', key, now, member)
redis.call('EXPIRE', key, window_seconds)
redis.call('EXPIRE', counter_key, window_seconds)
return 1
