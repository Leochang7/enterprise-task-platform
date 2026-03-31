package cn.leo.taskplatform.job;

import cn.leo.taskplatform.entity.TaskMainEntity;
import cn.leo.taskplatform.mapper.TaskMainMapper;
import cn.leo.taskplatform.mq.RocketMqTopics;
import cn.leo.taskplatform.mq.TaskDispatchMessage;
import cn.leo.taskplatform.mq.producer.RocketMqProducer;
import cn.leo.taskplatform.service.TaskCacheService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetryDispatchJob {

    private final TaskMainMapper taskMainMapper;
    private final RocketMqProducer rocketMqProducer;
    private final TaskCacheService taskCacheService;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${task.compensation.retry-scan-ms:30000}")
    public void retryDispatch() {
        LocalDateTime now = LocalDateTime.now();
        List<TaskMainEntity> retryTasks = taskMainMapper.selectList(new LambdaQueryWrapper<TaskMainEntity>()
                .eq(TaskMainEntity::getStatus, "RETRYING")
                .le(TaskMainEntity::getNextRetryAt, now)
                .eq(TaskMainEntity::getIsDeleted, 0)
                .orderByAsc(TaskMainEntity::getNextRetryAt)
                .last("LIMIT 100"));

        for (TaskMainEntity task : retryTasks) {
            try {
                rocketMqProducer.sendTaskDispatchMessage(resolveTopic(task.getPriority()), buildMessage(task));
                task.setStatus("QUEUED");
                task.setNextRetryAt(null);
                task.setUpdatedBy("retry-job");
                taskMainMapper.updateById(task);
                taskCacheService.cacheTaskStatus(task.getTaskId(), task.getStatus(), task.getCurrentStepCode(),
                        task.getProgressPercent(), task.getErrorCode(), LocalDateTime.now());
            } catch (Exception ex) {
                log.error("retry dispatch failed, taskId={}", task.getTaskId(), ex);
            }
        }
    }

    private TaskDispatchMessage buildMessage(TaskMainEntity task) throws Exception {
        Map<String, Object> payload = objectMapper.readValue(task.getRequestPayload(), new TypeReference<>() {
        });
        return TaskDispatchMessage.builder()
                .taskId(task.getTaskId())
                .traceId(task.getTraceId())
                .tenantId(task.getTenantId())
                .taskType(task.getTaskType())
                .templateCode(task.getTemplateCode())
                .priority(task.getPriority())
                .requestTime(LocalDateTime.now())
                .attemptNo(task.getRetryCount())
                .producer("retry-dispatch-job")
                .schemaVersion("v1")
                .payload(payload)
                .build();
    }

    private String resolveTopic(String priority) {
        return "HIGH".equalsIgnoreCase(priority) || "URGENT".equalsIgnoreCase(priority)
                ? RocketMqTopics.TASK_DISPATCH_HIGH
                : RocketMqTopics.TASK_DISPATCH_NORMAL;
    }
}
