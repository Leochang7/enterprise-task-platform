package cn.leo.taskplatform.controller;

import cn.leo.taskplatform.annotation.OperationLog;
import cn.leo.taskplatform.dto.callback.TaskCompleteCallbackRequest;
import cn.leo.taskplatform.dto.callback.TaskFailCallbackRequest;
import cn.leo.taskplatform.dto.callback.TaskStepCallbackRequest;
import cn.leo.taskplatform.response.ApiResponse;
import cn.leo.taskplatform.service.TaskCallbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "内部回调接口")
@RestController
@RequestMapping("/internal/callback")
@RequiredArgsConstructor
public class InternalCallbackController {

    private final TaskCallbackService taskCallbackService;

    @Operation(summary = "步骤状态回写")
    @PostMapping("/task-step")
    @OperationLog(module = "CALLBACK", action = "STEP_STATUS")
    public ApiResponse<Void> taskStep(@RequestBody @Valid TaskStepCallbackRequest request) {
        taskCallbackService.handleTaskStepCallback(request);
        return ApiResponse.success(null);
    }

    @Operation(summary = "任务完成回写")
    @PostMapping("/task-complete")
    @OperationLog(module = "CALLBACK", action = "TASK_COMPLETE")
    public ApiResponse<Void> taskComplete(@RequestBody @Valid TaskCompleteCallbackRequest request) {
        taskCallbackService.handleTaskCompleteCallback(request);
        return ApiResponse.success(null);
    }

    @Operation(summary = "任务失败回写")
    @PostMapping("/task-fail")
    @OperationLog(module = "CALLBACK", action = "TASK_FAIL")
    public ApiResponse<Void> taskFail(@RequestBody @Valid TaskFailCallbackRequest request) {
        taskCallbackService.handleTaskFailCallback(request);
        return ApiResponse.success(null);
    }
}
