package cn.leo.taskplatform.dto.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class TaskCreateRequest {

    @NotBlank
    private String taskType;

    @NotBlank
    private String templateCode;

    private String priority;

    private String bizKey;

    private String title;

    @NotNull
    private Map<String, Object> payload;
}
