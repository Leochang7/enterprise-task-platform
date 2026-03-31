package cn.leo.taskplatform.vo.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSimpleVO {

    private String taskId;
    private String traceId;
    private String taskType;
    private String templateCode;
    private String title;
    private String priority;
    private String status;
    private String currentStepCode;
    private Integer progressPercent;
    private String requesterName;
    private String errorCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
