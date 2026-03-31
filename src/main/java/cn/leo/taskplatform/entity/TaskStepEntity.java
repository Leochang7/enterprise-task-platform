package cn.leo.taskplatform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@TableName("task_step")
@EqualsAndHashCode(callSuper = true)
public class TaskStepEntity extends BaseEntity {

    private String taskId;
    private String stepId;
    private String stepCode;
    private String stepName;
    private String parentStepCode;
    private String stepType;
    private String agentType;
    private String agentName;
    private Integer sequenceNo;
    private String status;
    private String dependencyExpr;
    private String inputPayload;
    private String outputPayload;
    private String toolCalls;
    private Integer retryCount;
    private Integer maxRetryCount;
    private Long costMs;
    private Integer tokenUsage;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
