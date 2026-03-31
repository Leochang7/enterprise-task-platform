package cn.leo.taskplatform.controller;

import cn.leo.taskplatform.annotation.SlidingWindowLimit;
import cn.leo.taskplatform.dto.auth.LoginRequest;
import cn.leo.taskplatform.response.ApiResponse;
import cn.leo.taskplatform.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证接口")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    @SlidingWindowLimit(key = "auth:login", windowSeconds = 60, maxRequests = 10)
    public ApiResponse<?> login(@RequestBody @Valid LoginRequest request, HttpServletRequest httpServletRequest) {
        return ApiResponse.success(authService.login(request, httpServletRequest));
    }

    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.success(null);
    }

    @Operation(summary = "获取当前登录用户")
    @GetMapping("/me")
    public ApiResponse<?> me() {
        return ApiResponse.success(authService.currentUser());
    }
}
