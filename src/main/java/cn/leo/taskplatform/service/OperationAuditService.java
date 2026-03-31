package cn.leo.taskplatform.service;

import cn.leo.taskplatform.dto.admin.AuditPageQueryRequest;
import cn.leo.taskplatform.entity.SysOperationAuditEntity;
import cn.leo.taskplatform.mapper.OperationAuditMapper;
import cn.leo.taskplatform.response.PageResponse;
import cn.leo.taskplatform.vo.admin.OperationAuditVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OperationAuditService {

    private final OperationAuditMapper operationAuditMapper;

    public void save(SysOperationAuditEntity entity) {
        operationAuditMapper.insert(entity);
    }

    public PageResponse<OperationAuditVO> pageAudits(AuditPageQueryRequest request, boolean platformAdmin, String tenantId) {
        Page<SysOperationAuditEntity> page = new Page<>(request.getPageNo(), request.getPageSize());
        LambdaQueryWrapper<SysOperationAuditEntity> wrapper = new LambdaQueryWrapper<SysOperationAuditEntity>()
                .eq(request.getActionType() != null && !request.getActionType().isBlank(),
                        SysOperationAuditEntity::getActionType, request.getActionType())
                .eq(request.getOperatorUserId() != null && !request.getOperatorUserId().isBlank(),
                        SysOperationAuditEntity::getOperatorUserId, request.getOperatorUserId())
                .eq(request.getTargetType() != null && !request.getTargetType().isBlank(),
                        SysOperationAuditEntity::getTargetType, request.getTargetType())
                .ge(request.getStartTime() != null, SysOperationAuditEntity::getCreatedAt, request.getStartTime())
                .le(request.getEndTime() != null, SysOperationAuditEntity::getCreatedAt, request.getEndTime())
                .orderByDesc(SysOperationAuditEntity::getCreatedAt);
        if (!platformAdmin) {
            wrapper.eq(SysOperationAuditEntity::getTenantId, tenantId);
        }

        Page<SysOperationAuditEntity> resultPage = operationAuditMapper.selectPage(page, wrapper);
        List<OperationAuditVO> records = resultPage.getRecords().stream()
                .map(this::toVo)
                .toList();
        return PageResponse.<OperationAuditVO>builder()
                .pageNo(resultPage.getCurrent())
                .pageSize(resultPage.getSize())
                .total(resultPage.getTotal())
                .records(records)
                .build();
    }

    private OperationAuditVO toVo(SysOperationAuditEntity entity) {
        return OperationAuditVO.builder()
                .auditId(entity.getAuditId())
                .tenantId(entity.getTenantId())
                .operatorUserId(entity.getOperatorUserId())
                .operatorName(entity.getOperatorName())
                .actionType(entity.getActionType())
                .targetType(entity.getTargetType())
                .targetId(entity.getTargetId())
                .requestPath(entity.getRequestPath())
                .requestMethod(entity.getRequestMethod())
                .requestIp(entity.getRequestIp())
                .resultCode(entity.getResultCode())
                .resultMessage(entity.getResultMessage())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
