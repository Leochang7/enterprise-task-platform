package cn.leo.taskplatform.auth;

import cn.leo.taskplatform.auth.annotation.RequirePermissions;
import cn.leo.taskplatform.auth.annotation.RequireRoles;
import cn.leo.taskplatform.exception.PermissionException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Set;

@Component
public class PermissionInterceptor implements HandlerInterceptor {

    private final AuthPermissionService authPermissionService;

    public PermissionInterceptor(AuthPermissionService authPermissionService) {
        this.authPermissionService = authPermissionService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        AuthUser authUser = UserContextHolder.getRequiredUser();
        validateRoles(authUser, handlerMethod);
        validatePermissions(authUser, handlerMethod);
        return true;
    }

    private void validateRoles(AuthUser authUser, HandlerMethod handlerMethod) {
        RequireRoles requireRoles = findAnnotation(handlerMethod, RequireRoles.class);
        if (requireRoles == null) {
            return;
        }
        boolean matched = Arrays.stream(requireRoles.value())
                .map(String::toUpperCase)
                .anyMatch(requiredRole -> authUser.getRoleCodes().contains(requiredRole));
        if (!matched) {
            throw new PermissionException("AUTH_403", "无权限访问当前资源");
        }
    }

    private void validatePermissions(AuthUser authUser, HandlerMethod handlerMethod) {
        RequirePermissions requirePermissions = findAnnotation(handlerMethod, RequirePermissions.class);
        if (requirePermissions == null) {
            return;
        }
        Set<String> permissionCodes = authPermissionService.loadPermissionCodes(authUser);
        boolean matched = Arrays.stream(requirePermissions.value()).allMatch(permissionCodes::contains);
        if (!matched) {
            throw new PermissionException("AUTH_403", "权限不足");
        }
    }

    private <T extends java.lang.annotation.Annotation> T findAnnotation(HandlerMethod handlerMethod, Class<T> annotationClass) {
        T methodAnnotation = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), annotationClass);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        return AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), annotationClass);
    }
}
