package cn.leo.taskplatform.service;

import cn.leo.taskplatform.redis.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TaskIdempotencyService {

    private static final Duration IDEMPOTENT_TTL = Duration.ofHours(24);
    private static final String PENDING = "PENDING";

    private final StringRedisTemplate stringRedisTemplate;

    public boolean tryAcquire(String tenantId, String bizKey) {
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue()
                .setIfAbsent(buildKey(tenantId, bizKey), PENDING, IDEMPOTENT_TTL));
    }

    public String get(String tenantId, String bizKey) {
        return stringRedisTemplate.opsForValue().get(buildKey(tenantId, bizKey));
    }

    public void markSuccess(String tenantId, String bizKey, String taskId) {
        stringRedisTemplate.opsForValue().set(buildKey(tenantId, bizKey), taskId, IDEMPOTENT_TTL);
    }

    public void release(String tenantId, String bizKey) {
        stringRedisTemplate.delete(buildKey(tenantId, bizKey));
    }

    public boolean isPending(String taskId) {
        return PENDING.equals(taskId);
    }

    private String buildKey(String tenantId, String bizKey) {
        return RedisKeyConstants.TASK_IDEMPOTENT + tenantId + ":" + bizKey;
    }
}
