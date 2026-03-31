package cn.leo.taskplatform.aop;

import cn.leo.taskplatform.annotation.SlidingWindowLimit;
import cn.leo.taskplatform.auth.AuthUser;
import cn.leo.taskplatform.auth.UserContextHolder;
import cn.leo.taskplatform.redis.RedisKeyConstants;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
public class SlidingWindowLimitAspect {

    private final SlidingWindowRateLimitSupport slidingWindowRateLimitSupport;

    @Around("@annotation(limit)")
    public Object around(ProceedingJoinPoint joinPoint, SlidingWindowLimit limit) throws Throwable {
        slidingWindowRateLimitSupport.check(buildKey(limit.key()), limit.windowSeconds(), limit.maxRequests());
        return joinPoint.proceed();
    }

    private String buildKey(String keyPrefix) {
        AuthUser authUser = UserContextHolder.get();
        String tenantId = authUser != null ? authUser.getTenantId() : "anonymous";
        String userId = authUser != null ? authUser.getUserId() : "anonymous";
        HttpServletRequest request = currentRequest();
        String uri = request != null ? request.getRequestURI() : "unknown";
        String ip = request != null ? request.getRemoteAddr() : "unknown";
        return RedisKeyConstants.RATE_LIMIT + keyPrefix + ":" + tenantId + ":" + userId + ":" + ip + ":" + uri;
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }
}
