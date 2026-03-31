package cn.leo.taskplatform.service;

import cn.leo.taskplatform.auth.AuthUser;
import cn.leo.taskplatform.auth.UserContextHolder;
import cn.leo.taskplatform.dto.admin.AuditPageQueryRequest;
import cn.leo.taskplatform.dto.task.TaskPageQueryRequest;
import cn.leo.taskplatform.entity.TaskMainEntity;
import cn.leo.taskplatform.enums.RoleCode;
import cn.leo.taskplatform.mapper.TaskMainMapper;
import cn.leo.taskplatform.response.PageResponse;
import cn.leo.taskplatform.vo.admin.OperationAuditVO;
import cn.leo.taskplatform.vo.task.TaskSimpleVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminQueryService {

    private final TaskMainMapper taskMainMapper;
    private final OperationAuditService operationAuditService;

    public PageResponse<TaskSimpleVO> pageTasks(TaskPageQueryRequest request) {
        AuthUser authUser = UserContextHolder.getRequiredUser();
        boolean platformAdmin = authUser.getRoleCodes().contains(RoleCode.PLATFORM_ADMIN.name());

        Page<TaskMainEntity> page = new Page<>(request.getPageNo(), request.getPageSize());
        LambdaQueryWrapper<TaskMainEntity> wrapper = new LambdaQueryWrapper<TaskMainEntity>()
                .eq(TaskMainEntity::getIsDeleted, 0)
                .eq(request.getTaskType() != null && !request.getTaskType().isBlank(), TaskMainEntity::getTaskType, request.getTaskType())
                .eq(request.getStatus() != null && !request.getStatus().isBlank(), TaskMainEntity::getStatus, request.getStatus())
                .eq(request.getTemplateCode() != null && !request.getTemplateCode().isBlank(), TaskMainEntity::getTemplateCode, request.getTemplateCode())
                .ge(request.getStartTime() != null, TaskMainEntity::getCreatedAt, request.getStartTime())
                .le(request.getEndTime() != null, TaskMainEntity::getCreatedAt, request.getEndTime())
                .orderByDesc(TaskMainEntity::getCreatedAt);
        if (!platformAdmin) {
            wrapper.eq(TaskMainEntity::getTenantId, authUser.getTenantId());
        }
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            wrapper.and(w -> w.like(TaskMainEntity::getTaskId, request.getKeyword())
                    .or()
                    .like(TaskMainEntity::getTitle, request.getKeyword())
                    .or()
                    .like(TaskMainEntity::getRequesterName, request.getKeyword()));
        }

        Page<TaskMainEntity> resultPage = taskMainMapper.selectPage(page, wrapper);
        List<TaskSimpleVO> records = resultPage.getRecords().stream()
                .map(this::toTaskSimpleVo)
                .toList();
        return PageResponse.<TaskSimpleVO>builder()
                .pageNo(resultPage.getCurrent())
                .pageSize(resultPage.getSize())
                .total(resultPage.getTotal())
                .records(records)
                .build();
    }

    public PageResponse<OperationAuditVO> pageAudits(AuditPageQueryRequest request) {
        AuthUser authUser = UserContextHolder.getRequiredUser();
        boolean platformAdmin = authUser.getRoleCodes().contains(RoleCode.PLATFORM_ADMIN.name());
        return operationAuditService.pageAudits(request, platformAdmin, authUser.getTenantId());
    }

    private TaskSimpleVO toTaskSimpleVo(TaskMainEntity entity) {
        return TaskSimpleVO.builder()
                .taskId(entity.getTaskId())
                .traceId(entity.getTraceId())
                .taskType(entity.getTaskType())
                .templateCode(entity.getTemplateCode())
                .title(entity.getTitle())
                .priority(entity.getPriority())
                .status(entity.getStatus())
                .currentStepCode(entity.getCurrentStepCode())
                .progressPercent(entity.getProgressPercent())
                .requesterName(entity.getRequesterName())
                .errorCode(entity.getErrorCode())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
