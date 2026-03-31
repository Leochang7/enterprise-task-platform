package cn.leo.taskplatform.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDispatchMessage {

    private String taskId;
    private String traceId;
    private String tenantId;
    private String taskType;
    private String templateCode;
    private String priority;
    private LocalDateTime requestTime;
    private Integer attemptNo;
    private String producer;
    private String schemaVersion;
    private Map<String, Object> payload;
}
