package cn.leo.taskplatform.dto.callback;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class TaskCompleteCallbackRequest {

    @NotBlank
    private String taskId;

    private String resultType;
    private String resultSummary;
    private Map<String, Object> resultPayload;
    private Integer attachmentCount;
    private String storageProvider;
    private String storagePath;
    private String checksum;
}
