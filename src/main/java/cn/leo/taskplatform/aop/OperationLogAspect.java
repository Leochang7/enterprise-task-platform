package cn.leo.taskplatform.aop;

import cn.leo.taskplatform.annotation.OperationLog;
import cn.leo.taskplatform.auth.AuthUser;
import cn.leo.taskplatform.auth.UserContextHolder;
import cn.leo.taskplatform.entity.SysOperationAuditEntity;
import cn.leo.taskplatform.response.ApiResponse;
import cn.leo.taskplatform.service.OperationAuditService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.UUID;

@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    private final OperationAuditService operationAuditService;
    private final ObjectMapper objectMapper;

    public OperationLogAspect(OperationAuditService operationAuditService, ObjectMapper objectMapper) {
        this.operationAuditService = operationAuditService;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        Throwable throwable = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable ex) {
            throwable = ex;
            throw ex;
        } finally {
            log.info("module={} action={} cost={}ms", operationLog.module(), operationLog.action(), System.currentTimeMillis() - start);
            persistAudit(joinPoint, operationLog, result, throwable);
        }
    }

    private void persistAudit(ProceedingJoinPoint joinPoint, OperationLog operationLog, Object result, Throwable throwable) {
        try {
            HttpServletRequest request = currentRequest();
            AuthUser authUser = UserContextHolder.get();
            SysOperationAuditEntity entity = new SysOperationAuditEntity();
            entity.setAuditId(UUID.randomUUID().toString().replace("-", ""));
            entity.setTenantId(authUser == null ? null : authUser.getTenantId());
            entity.setOperatorUserId(authUser == null ? null : authUser.getUserId());
            entity.setOperatorName(authUser == null ? null : authUser.getUserName());
            entity.setActionType(operationLog.module() + ":" + operationLog.action());
            entity.setTargetType(joinPoint.getSignature().getDeclaringTypeName());
            entity.setTargetId(joinPoint.getSignature().getName());
            entity.setRequestPath(request == null ? null : request.getRequestURI());
            entity.setRequestMethod(request == null ? null : request.getMethod());
            entity.setRequestIp(request == null ? null : request.getRemoteAddr());
            entity.setRequestPayload(serializeArgs(joinPoint.getArgs()));
            entity.setResultCode(resolveResultCode(result, throwable));
            entity.setResultMessage(resolveResultMessage(result, throwable));
            operationAuditService.save(entity);
        } catch (Exception ex) {
            log.warn("persist audit failed", ex);
        }
    }

    private String serializeArgs(Object[] args) {
        Object[] filteredArgs = Arrays.stream(args)
                .filter(arg -> !(arg instanceof HttpServletRequest))
                .filter(arg -> !(arg instanceof jakarta.servlet.http.HttpServletResponse))
                .toArray();
        try {
            return objectMapper.writeValueAsString(filteredArgs);
        } catch (JsonProcessingException ex) {
            return "[\"SERIALIZE_FAILED\"]";
        }
    }

    private String resolveResultCode(Object result, Throwable throwable) {
        if (throwable != null) {
            return "ERROR";
        }
        if (result instanceof ApiResponse<?> apiResponse) {
            return apiResponse.getCode();
        }
        return "0";
    }

    private String resolveResultMessage(Object result, Throwable throwable) {
        if (throwable != null) {
            return throwable.getMessage();
        }
        if (result instanceof ApiResponse<?> apiResponse) {
            return apiResponse.getMessage();
        }
        return "success";
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }
}
