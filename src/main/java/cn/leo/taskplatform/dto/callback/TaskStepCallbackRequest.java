package cn.leo.taskplatform.dto.callback;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class TaskStepCallbackRequest {

    @NotBlank
    private String taskId;

    @NotBlank
    private String stepCode;

    @NotBlank
    private String status;

    private Map<String, Object> outputPayload;
    private String errorCode;
    private String errorMessage;
    private Long version;
}
