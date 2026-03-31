package cn.leo.taskplatform.vo.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDetailVO {

    private String taskId;
    private String traceId;
    private String taskType;
    private String templateCode;
    private String priority;
    private String title;
    private String status;
    private String currentStepCode;
    private Integer progressPercent;
    private String resultSummary;
    private String errorCode;
    private String errorMessage;
    private String storagePath;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime finishedAt;
    @Builder.Default
    private List<TaskStepVO> steps = new ArrayList<>();
}
