package cn.leo.taskplatform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("sys_operation_audit")
@EqualsAndHashCode(callSuper = true)
public class SysOperationAuditEntity extends BaseEntity {

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
    private String requestPayload;
    private String resultCode;
    private String resultMessage;
}
