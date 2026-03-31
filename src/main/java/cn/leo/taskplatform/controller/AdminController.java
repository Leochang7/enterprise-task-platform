package cn.leo.taskplatform.controller;

import cn.leo.taskplatform.auth.annotation.RequireRoles;
import cn.leo.taskplatform.dto.admin.AuditPageQueryRequest;
import cn.leo.taskplatform.dto.task.TaskPageQueryRequest;
import cn.leo.taskplatform.response.ApiResponse;
import cn.leo.taskplatform.response.PageResponse;
import cn.leo.taskplatform.service.AdminQueryService;
import cn.leo.taskplatform.service.OperationAuditService;
import cn.leo.taskplatform.vo.admin.OperationAuditVO;
import cn.leo.taskplatform.vo.task.TaskSimpleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "后台管理接口")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@RequireRoles({"TENANT_ADMIN", "PLATFORM_ADMIN"})
public class AdminController {

    private final AdminQueryService adminQueryService;
    private final OperationAuditService operationAuditService;

    @Operation(summary = "后台任务分页查询")
    @GetMapping("/tasks")
    public ApiResponse<PageResponse<TaskSimpleVO>> pageTasks(@ModelAttribute TaskPageQueryRequest request) {
        return ApiResponse.success(adminQueryService.pageTasks(request));
    }

    @Operation(summary = "审计日志分页查询")
    @GetMapping("/audits")
    public ApiResponse<PageResponse<OperationAuditVO>> pageAudits(@ModelAttribute AuditPageQueryRequest request) {
        return ApiResponse.success(adminQueryService.pageAudits(request));
    }
}
