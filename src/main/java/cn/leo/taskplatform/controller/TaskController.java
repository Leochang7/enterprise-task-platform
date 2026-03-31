package cn.leo.taskplatform.controller;

import cn.leo.taskplatform.annotation.OperationLog;
import cn.leo.taskplatform.annotation.SlidingWindowLimit;
import cn.leo.taskplatform.dto.task.TaskCreateRequest;
import cn.leo.taskplatform.response.ApiResponse;
import cn.leo.taskplatform.service.TaskService;
import cn.leo.taskplatform.vo.task.TaskCreateResponse;
import cn.leo.taskplatform.vo.task.TaskDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "任务接口")
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "创建任务")
    @PostMapping
    @OperationLog(module = "TASK", action = "CREATE")
    @SlidingWindowLimit(key = "task:create", windowSeconds = 60, maxRequests = 30)
    public ApiResponse<TaskCreateResponse> createTask(@RequestBody @Valid TaskCreateRequest request) {
        return ApiResponse.success(taskService.createTask(request));
    }

    @Operation(summary = "查询任务详情")
    @GetMapping("/{taskId}")
    public ApiResponse<TaskDetailVO> detail(@PathVariable String taskId) {
        return ApiResponse.success(taskService.getTaskDetail(taskId));
    }
}
