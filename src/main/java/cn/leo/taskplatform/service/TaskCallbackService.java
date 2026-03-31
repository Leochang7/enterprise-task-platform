package cn.leo.taskplatform.service;

import cn.leo.taskplatform.dto.callback.TaskCompleteCallbackRequest;
import cn.leo.taskplatform.dto.callback.TaskFailCallbackRequest;
import cn.leo.taskplatform.dto.callback.TaskStepCallbackRequest;
import cn.leo.taskplatform.entity.TaskMainEntity;
import cn.leo.taskplatform.entity.TaskResultEntity;
import cn.leo.taskplatform.entity.TaskStepEntity;
import cn.leo.taskplatform.exception.BizException;
import cn.leo.taskplatform.mapper.TaskMainMapper;
import cn.leo.taskplatform.mapper.TaskResultMapper;
import cn.leo.taskplatform.mapper.TaskStepMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskCallbackService {

    private final TaskMainMapper taskMainMapper;
    private final TaskStepMapper taskStepMapper;
    private final TaskResultMapper taskResultMapper;
    private final TaskCacheService taskCacheService;
    private final ObjectMapper objectMapper;

    @Transactional(rollbackFor = Exception.class)
    public void handleTaskStepCallback(TaskStepCallbackRequest request) {
        TaskMainEntity taskMain = getTaskMain(request.getTaskId());
        TaskStepEntity stepEntity = taskStepMapper.selectOne(new LambdaQueryWrapper<TaskStepEntity>()
                .eq(TaskStepEntity::getTaskId, request.getTaskId())
                .eq(TaskStepEntity::getStepCode, request.getStepCode())
                .last("LIMIT 1"));

        LocalDateTime now = LocalDateTime.now();
        if (stepEntity == null) {
            stepEntity = new TaskStepEntity();
            stepEntity.setTaskId(request.getTaskId());
            stepEntity.setStepId(generateId());
            stepEntity.setStepCode(request.getStepCode());
            stepEntity.setStepName(request.getStepCode());
            stepEntity.setStepType("CALLBACK");
            stepEntity.setSequenceNo(nextSequenceNo(request.getTaskId()));
            stepEntity.setRetryCount(0);
            stepEntity.setMaxRetryCount(3);
            stepEntity.setCostMs(0L);
            stepEntity.setTokenUsage(0);
            populateStep(stepEntity, request, now);
            taskStepMapper.insert(stepEntity);
        } else {
            populateStep(stepEntity, request, now);
            taskStepMapper.updateById(stepEntity);
        }

        taskMain.setCurrentStepCode(request.getStepCode());
        taskMain.setStatus(resolveTaskStatusOnStep(request.getStatus()));
        taskMain.setProgressPercent(calculateProgressPercent(request.getTaskId()));
        taskMain.setErrorCode(request.getErrorCode());
        taskMain.setErrorMessage(request.getErrorMessage());
        taskMain.setUpdatedBy("callback");
        taskMainMapper.updateById(taskMain);
        refreshTaskCache(taskMain);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleTaskCompleteCallback(TaskCompleteCallbackRequest request) {
        TaskMainEntity taskMain = getTaskMain(request.getTaskId());
        LocalDateTime now = LocalDateTime.now();

        taskMain.setStatus("SUCCESS");
        taskMain.setProgressPercent(100);
        taskMain.setResultSummary(request.getResultSummary());
        taskMain.setErrorCode(null);
        taskMain.setErrorMessage(null);
        taskMain.setFinishedAt(now);
        taskMain.setUpdatedBy("callback");
        taskMainMapper.updateById(taskMain);

        TaskResultEntity taskResult = taskResultMapper.selectOne(new LambdaQueryWrapper<TaskResultEntity>()
                .eq(TaskResultEntity::getTaskId, request.getTaskId())
                .last("LIMIT 1"));
        if (taskResult == null) {
            taskResult = new TaskResultEntity();
            taskResult.setTaskId(request.getTaskId());
            taskResult.setResultType(defaultIfBlank(request.getResultType(), "DEFAULT"));
            taskResult.setResultStatus("SUCCESS");
            taskResult.setAttachmentCount(defaultIfNull(request.getAttachmentCount(), 0));
            updateResult(taskResult, request);
            taskResultMapper.insert(taskResult);
        } else {
            taskResult.setResultType(defaultIfBlank(request.getResultType(), taskResult.getResultType()));
            taskResult.setResultStatus("SUCCESS");
            taskResult.setAttachmentCount(defaultIfNull(request.getAttachmentCount(), taskResult.getAttachmentCount()));
            updateResult(taskResult, request);
            taskResultMapper.updateById(taskResult);
        }

        refreshTaskCache(taskMain);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleTaskFailCallback(TaskFailCallbackRequest request) {
        TaskMainEntity taskMain = getTaskMain(request.getTaskId());
        LocalDateTime now = LocalDateTime.now();

        if (request.isRetryable() && taskMain.getRetryCount() < taskMain.getMaxRetryCount()) {
            taskMain.setStatus("RETRYING");
            taskMain.setRetryCount(taskMain.getRetryCount() + 1);
            taskMain.setNextRetryAt(request.getNextRetryAt() == null ? now.plusMinutes(1) : request.getNextRetryAt());
        } else {
            taskMain.setStatus("FAILED");
            taskMain.setFinishedAt(now);
        }
        taskMain.setCurrentStepCode(request.getStepCode());
        taskMain.setErrorCode(request.getErrorCode());
        taskMain.setErrorMessage(request.getErrorMessage());
        taskMain.setUpdatedBy("callback");
        taskMainMapper.updateById(taskMain);

        if (request.getStepCode() != null && !request.getStepCode().isBlank()) {
            TaskStepEntity stepEntity = taskStepMapper.selectOne(new LambdaQueryWrapper<TaskStepEntity>()
                    .eq(TaskStepEntity::getTaskId, request.getTaskId())
                    .eq(TaskStepEntity::getStepCode, request.getStepCode())
                    .last("LIMIT 1"));
            if (stepEntity != null) {
                stepEntity.setStatus("FAILED");
                stepEntity.setErrorCode(request.getErrorCode());
                stepEntity.setErrorMessage(request.getErrorMessage());
                stepEntity.setFinishedAt(now);
                taskStepMapper.updateById(stepEntity);
            }
        }

        refreshTaskCache(taskMain);
    }

    private void populateStep(TaskStepEntity stepEntity, TaskStepCallbackRequest request, LocalDateTime now) {
        stepEntity.setStatus(request.getStatus());
        stepEntity.setOutputPayload(toJson(request.getOutputPayload()));
        stepEntity.setErrorCode(request.getErrorCode());
        stepEntity.setErrorMessage(request.getErrorMessage());
        if ("RUNNING".equalsIgnoreCase(request.getStatus()) && stepEntity.getStartedAt() == null) {
            stepEntity.setStartedAt(now);
        }
        if ("SUCCESS".equalsIgnoreCase(request.getStatus()) || "FAILED".equalsIgnoreCase(request.getStatus())) {
            if (stepEntity.getStartedAt() == null) {
                stepEntity.setStartedAt(now);
            }
            stepEntity.setFinishedAt(now);
        }
    }

    private void updateResult(TaskResultEntity taskResult, TaskCompleteCallbackRequest request) {
        taskResult.setResultSummary(request.getResultSummary());
        taskResult.setResultPayload(toJson(request.getResultPayload()));
        taskResult.setStorageProvider(request.getStorageProvider());
        taskResult.setStoragePath(request.getStoragePath());
        taskResult.setChecksum(request.getChecksum());
    }

    private TaskMainEntity getTaskMain(String taskId) {
        TaskMainEntity taskMain = taskMainMapper.selectOne(new LambdaQueryWrapper<TaskMainEntity>()
                .eq(TaskMainEntity::getTaskId, taskId)
                .eq(TaskMainEntity::getIsDeleted, 0)
                .last("LIMIT 1"));
        if (taskMain == null) {
            throw new BizException("TASK_404", "任务不存在");
        }
        return taskMain;
    }

    private int calculateProgressPercent(String taskId) {
        long total = taskStepMapper.selectCount(new LambdaQueryWrapper<TaskStepEntity>()
                .eq(TaskStepEntity::getTaskId, taskId));
        if (total <= 0) {
            return 0;
        }
        long completed = taskStepMapper.selectCount(new LambdaQueryWrapper<TaskStepEntity>()
                .eq(TaskStepEntity::getTaskId, taskId)
                .in(TaskStepEntity::getStatus, "SUCCESS", "FAILED"));
        return (int) Math.min(100, completed * 100 / total);
    }

    private int nextSequenceNo(String taskId) {
        TaskStepEntity lastStep = taskStepMapper.selectOne(new LambdaQueryWrapper<TaskStepEntity>()
                .eq(TaskStepEntity::getTaskId, taskId)
                .orderByDesc(TaskStepEntity::getSequenceNo)
                .last("LIMIT 1"));
        return lastStep == null || lastStep.getSequenceNo() == null ? 1 : lastStep.getSequenceNo() + 1;
    }

    private void refreshTaskCache(TaskMainEntity taskMain) {
        taskCacheService.cacheTaskStatus(
                taskMain.getTaskId(),
                taskMain.getStatus(),
                taskMain.getCurrentStepCode(),
                taskMain.getProgressPercent(),
                taskMain.getErrorCode(),
                LocalDateTime.now()
        );
        taskCacheService.evictTaskDetail(taskMain.getTaskId());
    }

    private String resolveTaskStatusOnStep(String stepStatus) {
        if ("RUNNING".equalsIgnoreCase(stepStatus)) {
            return "RUNNING";
        }
        if ("SUCCESS".equalsIgnoreCase(stepStatus)) {
            return "RUNNING";
        }
        if ("FAILED".equalsIgnoreCase(stepStatus)) {
            return "FAILED";
        }
        return "DISPATCHED";
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BizException("CALLBACK_400", "回调数据序列化失败");
        }
    }

    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private Integer defaultIfNull(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }
}
