package cn.leo.taskplatform.dto.callback;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskFailCallbackRequest {

    @NotBlank
    private String taskId;

    private String stepCode;
    private String errorCode;
    private String errorMessage;
    private boolean retryable;
    private LocalDateTime nextRetryAt;
}
