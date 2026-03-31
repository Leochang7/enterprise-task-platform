package cn.leo.taskplatform.auth;

import cn.leo.taskplatform.exception.AuthException;
import cn.leo.taskplatform.redis.RedisKeyConstants;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String authorization = request.getHeader("Authorization");
            if (authorization != null && authorization.startsWith("Bearer ")) {
                String token = authorization.substring(7);
                AuthUser authUser = parseAndValidate(token);
                UserContextHolder.set(authUser);
            }
            filterChain.doFilter(request, response);
        } finally {
            UserContextHolder.clear();
        }
    }

    private AuthUser parseAndValidate(String token) {
        try {
            AuthUser authUser = jwtTokenService.parseToken(token);
            String redisTokenVersion = stringRedisTemplate.opsForValue()
                    .get(RedisKeyConstants.AUTH_TOKEN + authUser.getSessionId());
            if (redisTokenVersion == null || !redisTokenVersion.equals(authUser.getTokenVersion())) {
                throw new AuthException("AUTH_401", "登录已失效，请重新登录");
            }
            return authUser;
        } catch (JwtException | IllegalArgumentException ex) {
            throw new AuthException("AUTH_401", "Token 非法或已过期");
        }
    }
}
