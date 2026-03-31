package cn.leo.taskplatform.dto.admin;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditPageQueryRequest {

    private long pageNo = 1;
    private long pageSize = 20;
    private String actionType;
    private String operatorUserId;
    private String targetType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
