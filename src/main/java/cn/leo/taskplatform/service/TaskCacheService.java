package cn.leo.taskplatform.service;

import cn.leo.taskplatform.redis.RedisKeyConstants;
import cn.leo.taskplatform.vo.task.TaskDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaskCacheService {

    private static final Duration TASK_STATUS_TTL = Duration.ofHours(24);
    private static final Duration TASK_DETAIL_TTL = Duration.ofMinutes(30);

    private final RedisTemplate<String, Object> redisTemplate;

    public void cacheTaskStatus(String taskId,
                                String status,
                                String currentStepCode,
                                Integer progressPercent,
                                String errorCode,
                                LocalDateTime updatedAt) {
        Map<String, Object> statusSnapshot = new LinkedHashMap<>();
        statusSnapshot.put("taskId", taskId);
        statusSnapshot.put("status", status);
        statusSnapshot.put("currentStepCode", currentStepCode);
        statusSnapshot.put("progressPercent", progressPercent);
        statusSnapshot.put("errorCode", errorCode);
        statusSnapshot.put("updatedAt", updatedAt);
        redisTemplate.opsForValue().set(RedisKeyConstants.TASK_STATUS + taskId, statusSnapshot, TASK_STATUS_TTL);
    }

    public void cacheTaskDetail(TaskDetailVO taskDetail) {
        redisTemplate.opsForValue().set(RedisKeyConstants.TASK_DETAIL + taskDetail.getTaskId(), taskDetail, TASK_DETAIL_TTL);
    }

    public TaskDetailVO getTaskDetail(String taskId) {
        Object value = redisTemplate.opsForValue().get(RedisKeyConstants.TASK_DETAIL + taskId);
        if (value instanceof TaskDetailVO taskDetailVO) {
            return taskDetailVO;
        }
        return null;
    }

    public void evictTaskDetail(String taskId) {
        redisTemplate.delete(RedisKeyConstants.TASK_DETAIL + taskId);
    }

    public void evictTaskStatus(String taskId) {
        redisTemplate.delete(RedisKeyConstants.TASK_STATUS + taskId);
    }
}
