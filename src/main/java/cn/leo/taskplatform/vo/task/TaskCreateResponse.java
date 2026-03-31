package cn.leo.taskplatform.vo.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskCreateResponse {

    private String taskId;
    private String traceId;
    private String status;
}
