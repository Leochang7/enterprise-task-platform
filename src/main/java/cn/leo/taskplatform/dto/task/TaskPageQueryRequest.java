package cn.leo.taskplatform.dto.task;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskPageQueryRequest {

    private long pageNo = 1;
    private long pageSize = 20;
    private String taskType;
    private String status;
    private String templateCode;
    private String keyword;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
