package cn.leo.taskplatform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@TableName("task_main")
@EqualsAndHashCode(callSuper = true)
public class TaskMainEntity extends BaseEntity {

    private String taskId;
    private String traceId;
    private String tenantId;
    private String bizKey;
    private String batchId;
    private String taskType;
    private String templateCode;
    private Integer templateVersion;
    private String sourceSystem;
    private String sourceChannel;
    private String title;
    private String priority;
    private String status;
    private String currentStepCode;
    private Integer progressPercent;
    private Integer retryCount;
    private Integer maxRetryCount;
    private String callbackUrl;
    private String requesterId;
    private String requesterName;
    private String executorMode;
    private String requestPayload;
    private String contextSnapshot;
    private String resultSummary;
    private String resultCode;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime acceptedAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime nextRetryAt;
    private String createdBy;
    private String updatedBy;
    private Integer isDeleted;
}
