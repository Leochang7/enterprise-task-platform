package cn.leo.taskplatform.service;

import cn.leo.taskplatform.auth.AuthUser;
import cn.leo.taskplatform.auth.UserContextHolder;
import cn.leo.taskplatform.dto.task.TaskCreateRequest;
import cn.leo.taskplatform.entity.TaskMainEntity;
import cn.leo.taskplatform.entity.TaskResultEntity;
import cn.leo.taskplatform.entity.TaskStepEntity;
import cn.leo.taskplatform.entity.TaskTemplateEntity;
import cn.leo.taskplatform.exception.BizException;
import cn.leo.taskplatform.mapper.TaskMainMapper;
import cn.leo.taskplatform.mapper.TaskResultMapper;
import cn.leo.taskplatform.mapper.TaskStepMapper;
import cn.leo.taskplatform.mapper.TaskTemplateMapper;
import cn.leo.taskplatform.mq.RocketMqTopics;
import cn.leo.taskplatform.mq.TaskDispatchMessage;
import cn.leo.taskplatform.mq.producer.RocketMqProducer;
import cn.leo.taskplatform.vo.task.TaskCreateResponse;
import cn.leo.taskplatform.vo.task.TaskDetailVO;
import cn.leo.taskplatform.vo.task.TaskStepVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskMainMapper taskMainMapper;
    private final TaskStepMapper taskStepMapper;
    private final TaskTemplateMapper taskTemplateMapper;
    private final TaskResultMapper taskResultMapper;
    private final RocketMqProducer rocketMqProducer;
    private final TaskCacheService taskCacheService;
    private final TaskIdempotencyService taskIdempotencyService;
    private final ObjectMapper objectMapper;

    @Transactional(rollbackFor = Exception.class)
    public TaskCreateResponse createTask(TaskCreateRequest request) {
        AuthUser authUser = UserContextHolder.getRequiredUser();
        validateBizKey(request, authUser);

        TaskTemplateEntity template = loadTemplate(request);
        String taskId = generateTaskId();
        String traceId = generateTraceId();
        String priority = resolvePriority(request, template);
        LocalDateTime now = LocalDateTime.now();

        TaskMainEntity entity = buildTaskEntity(request, authUser, template, taskId, traceId, priority, now);
        try {
            taskMainMapper.insert(entity);
            taskCacheService.cacheTaskStatus(taskId, entity.getStatus(), entity.getCurrentStepCode(), entity.getProgressPercent(), entity.getErrorCode(), now);

            TaskDispatchMessage message = buildDispatchMessage(entity, request.getPayload());
            rocketMqProducer.sendTaskDispatchMessage(resolveTopic(priority), message);

            if (hasBizKey(request)) {
                taskIdempotencyService.markSuccess(authUser.getTenantId(), request.getBizKey(), taskId);
            }

            return TaskCreateResponse.builder()
                    .taskId(taskId)
                    .traceId(traceId)
                    .status(entity.getStatus())
                    .build();
        } catch (Exception ex) {
            if (hasBizKey(request)) {
                taskIdempotencyService.release(authUser.getTenantId(), request.getBizKey());
            }
            taskCacheService.evictTaskStatus(taskId);
            taskCacheService.evictTaskDetail(taskId);
            throw wrapCreateException(ex);
        }
    }

    public TaskDetailVO getTaskDetail(String taskId) {
        TaskDetailVO cached = taskCacheService.getTaskDetail(taskId);
        if (cached != null) {
            return cached;
        }

        AuthUser authUser = UserContextHolder.getRequiredUser();
        TaskMainEntity taskMain = taskMainMapper.selectOne(new LambdaQueryWrapper<TaskMainEntity>()
                .eq(TaskMainEntity::getTaskId, taskId)
                .eq(TaskMainEntity::getTenantId, authUser.getTenantId())
                .eq(TaskMainEntity::getIsDeleted, 0)
                .last("LIMIT 1"));
        if (taskMain == null) {
            throw new BizException("TASK_404", "任务不存在");
        }

        List<TaskStepVO> steps = taskStepMapper.selectList(new LambdaQueryWrapper<TaskStepEntity>()
                        .eq(TaskStepEntity::getTaskId, taskId)
                        .orderByAsc(TaskStepEntity::getSequenceNo))
                .stream()
                .map(this::toTaskStepVO)
                .toList();

        TaskResultEntity taskResult = taskResultMapper.selectOne(new LambdaQueryWrapper<TaskResultEntity>()
                .eq(TaskResultEntity::getTaskId, taskId)
                .last("LIMIT 1"));

        TaskDetailVO detail = TaskDetailVO.builder()
                .taskId(taskMain.getTaskId())
                .traceId(taskMain.getTraceId())
                .taskType(taskMain.getTaskType())
                .templateCode(taskMain.getTemplateCode())
                .priority(taskMain.getPriority())
                .title(taskMain.getTitle())
                .status(taskMain.getStatus())
                .currentStepCode(taskMain.getCurrentStepCode())
                .progressPercent(taskMain.getProgressPercent())
                .resultSummary(taskMain.getResultSummary())
                .errorCode(taskMain.getErrorCode())
                .errorMessage(taskMain.getErrorMessage())
                .storagePath(taskResult == null ? null : taskResult.getStoragePath())
                .createdAt(taskMain.getCreatedAt())
                .updatedAt(taskMain.getUpdatedAt())
                .finishedAt(taskMain.getFinishedAt())
                .steps(steps)
                .build();
        taskCacheService.cacheTaskDetail(detail);
        return detail;
    }

    private void validateBizKey(TaskCreateRequest request, AuthUser authUser) {
        if (!hasBizKey(request)) {
            return;
        }
        if (!taskIdempotencyService.tryAcquire(authUser.getTenantId(), request.getBizKey())) {
            String existingTaskId = taskIdempotencyService.get(authUser.getTenantId(), request.getBizKey());
            if (existingTaskId != null && !taskIdempotencyService.isPending(existingTaskId)) {
                throw new BizException("TASK_4003", "任务已提交，请勿重复操作，taskId=" + existingTaskId);
            }
            throw new BizException("TASK_4003", "重复提交，请稍后再试");
        }
    }

    private TaskTemplateEntity loadTemplate(TaskCreateRequest request) {
        TaskTemplateEntity template = taskTemplateMapper.selectOne(new LambdaQueryWrapper<TaskTemplateEntity>()
                .eq(TaskTemplateEntity::getTemplateCode, request.getTemplateCode())
                .eq(TaskTemplateEntity::getTaskType, request.getTaskType())
                .eq(TaskTemplateEntity::getStatus, "PUBLISHED")
                .eq(TaskTemplateEntity::getIsDeleted, 0)
                .orderByDesc(TaskTemplateEntity::getTemplateVersion)
                .last("LIMIT 1"));
        if (template == null) {
            throw new BizException("TASK_4002", "模板不存在或未发布");
        }
        return template;
    }

    private TaskMainEntity buildTaskEntity(TaskCreateRequest request,
                                           AuthUser authUser,
                                           TaskTemplateEntity template,
                                           String taskId,
                                           String traceId,
                                           String priority,
                                           LocalDateTime now) {
        TaskMainEntity entity = new TaskMainEntity();
        entity.setTaskId(taskId);
        entity.setTraceId(traceId);
        entity.setTenantId(authUser.getTenantId());
        entity.setBizKey(request.getBizKey());
        entity.setTaskType(request.getTaskType());
        entity.setTemplateCode(request.getTemplateCode());
        entity.setTemplateVersion(template.getTemplateVersion());
        entity.setSourceChannel("api");
        entity.setTitle(request.getTitle());
        entity.setPriority(priority);
        entity.setStatus("QUEUED");
        entity.setProgressPercent(0);
        entity.setRetryCount(0);
        entity.setMaxRetryCount(3);
        entity.setRequesterId(authUser.getUserId());
        entity.setRequesterName(authUser.getUserName());
        entity.setExecutorMode("auto");
        entity.setRequestPayload(toJson(request.getPayload()));
        entity.setContextSnapshot(toJson(Map.of(
                "tenantId", authUser.getTenantId(),
                "userId", authUser.getUserId(),
                "roleCodes", authUser.getRoleCodes()
        )));
        entity.setAcceptedAt(now);
        entity.setCreatedBy(authUser.getUserId());
        entity.setUpdatedBy(authUser.getUserId());
        entity.setIsDeleted(0);
        return entity;
    }

    private TaskDispatchMessage buildDispatchMessage(TaskMainEntity taskMainEntity, Map<String, Object> payload) {
        return TaskDispatchMessage.builder()
                .taskId(taskMainEntity.getTaskId())
                .traceId(taskMainEntity.getTraceId())
                .tenantId(taskMainEntity.getTenantId())
                .taskType(taskMainEntity.getTaskType())
                .templateCode(taskMainEntity.getTemplateCode())
                .priority(taskMainEntity.getPriority())
                .requestTime(taskMainEntity.getAcceptedAt())
                .attemptNo(0)
                .producer("java-access-layer")
                .schemaVersion("v1")
                .payload(payload)
                .build();
    }

    private TaskStepVO toTaskStepVO(TaskStepEntity entity) {
        return TaskStepVO.builder()
                .stepCode(entity.getStepCode())
                .stepName(entity.getStepName())
                .agentType(entity.getAgentType())
                .status(entity.getStatus())
                .retryCount(entity.getRetryCount())
                .errorCode(entity.getErrorCode())
                .errorMessage(entity.getErrorMessage())
                .startedAt(entity.getStartedAt())
                .finishedAt(entity.getFinishedAt())
                .build();
    }

    private String resolvePriority(TaskCreateRequest request, TaskTemplateEntity template) {
        if (request.getPriority() != null && !request.getPriority().isBlank()) {
            return request.getPriority().trim().toUpperCase();
        }
        return template.getDefaultPriority();
    }

    private String resolveTopic(String priority) {
        return "HIGH".equalsIgnoreCase(priority) || "URGENT".equalsIgnoreCase(priority)
                ? RocketMqTopics.TASK_DISPATCH_HIGH
                : RocketMqTopics.TASK_DISPATCH_NORMAL;
    }

    private BizException wrapCreateException(Exception ex) {
        if (ex instanceof BizException bizException) {
            return bizException;
        }
        log.error("create task failed", ex);
        return new BizException("TASK_5001", "任务投递失败");
    }

    private boolean hasBizKey(TaskCreateRequest request) {
        return request.getBizKey() != null && !request.getBizKey().isBlank();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BizException("TASK_4001", "请求数据序列化失败");
        }
    }

    private String generateTaskId() {
        return "T" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private String generateTraceId() {
        return "TRACE-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
