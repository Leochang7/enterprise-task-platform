package cn.leo.taskplatform.vo.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationAuditVO {

    private String auditId;
    private String tenantId;
    private String operatorUserId;
    private String operatorName;
    private String actionType;
    private String targetType;
    private String targetId;
    private String requestPath;
    private String requestMethod;
    private String requestIp;
    private String resultCode;
    private String resultMessage;
    private LocalDateTime createdAt;
}
