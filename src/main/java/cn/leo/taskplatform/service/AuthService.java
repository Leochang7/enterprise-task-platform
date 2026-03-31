package cn.leo.taskplatform.service;

import cn.leo.taskplatform.auth.AccountLoginRecord;
import cn.leo.taskplatform.auth.AuthPermissionService;
import cn.leo.taskplatform.auth.AuthUser;
import cn.leo.taskplatform.auth.JwtTokenService;
import cn.leo.taskplatform.auth.UserContextHolder;
import cn.leo.taskplatform.dto.auth.LoginRequest;
import cn.leo.taskplatform.entity.SysLoginSessionEntity;
import cn.leo.taskplatform.enums.RoleCode;
import cn.leo.taskplatform.exception.AuthException;
import cn.leo.taskplatform.mapper.AuthQueryMapper;
import cn.leo.taskplatform.mapper.LoginSessionMapper;
import cn.leo.taskplatform.redis.RedisKeyConstants;
import cn.leo.taskplatform.utils.PasswordHashUtils;
import cn.leo.taskplatform.vo.auth.LoginResponse;
import cn.leo.taskplatform.vo.auth.UserInfoVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthQueryMapper authQueryMapper;
    private final LoginSessionMapper loginSessionMapper;
    private final JwtTokenService jwtTokenService;
    private final StringRedisTemplate stringRedisTemplate;
    private final AuthPermissionService authPermissionService;

    @Transactional(rollbackFor = Exception.class)
    public LoginResponse login(LoginRequest request, HttpServletRequest servletRequest) {
        AccountLoginRecord account = loadAccount(request);
        validateAccountStatus(account);
        if (!PasswordHashUtils.matches(request.getPassword(), account.getPasswordSalt(), account.getPasswordHash())) {
            authQueryMapper.incrementFailedLoginCount(account.getAccountId());
            throw new AuthException("AUTH_401", "用户名或密码错误");
        }

        authQueryMapper.clearFailedLoginState(account.getAccountId());

        Set<String> roleCodes = loadRoleCodes(account);
        Set<String> permissionCodes = new LinkedHashSet<>(authQueryMapper.findPermissionCodes(account.getTenantId(), account.getUserId()));

        String sessionId = generateId();
        String tokenVersion = generateId();
        persistSession(account, sessionId, tokenVersion, servletRequest);

        String accessToken = jwtTokenService.createAccessToken(account, sessionId, tokenVersion, roleCodes);
        stringRedisTemplate.opsForValue().set(
                RedisKeyConstants.AUTH_TOKEN + sessionId,
                tokenVersion,
                Duration.ofSeconds(jwtTokenService.getExpireSeconds())
        );
        authPermissionService.cachePermissionCodes(account.getTenantId(), account.getUserId(), permissionCodes);

        LocalDateTime now = LocalDateTime.now();
        authQueryMapper.updateUserLoginInfo(account.getTenantId(), account.getUserId(), now, extractClientIp(servletRequest));

        return LoginResponse.builder()
                .accessToken(accessToken)
                .expiresIn(jwtTokenService.getExpireSeconds())
                .userInfo(buildUserInfo(account, roleCodes, permissionCodes))
                .build();
    }

    public void logout() {
        AuthUser authUser = UserContextHolder.getRequiredUser();
        stringRedisTemplate.delete(RedisKeyConstants.AUTH_TOKEN + authUser.getSessionId());
        authQueryMapper.offlineSession(authUser.getSessionId(), LocalDateTime.now());
    }

    public UserInfoVO currentUser() {
        AuthUser authUser = UserContextHolder.getRequiredUser();
        Set<String> permissionCodes = authPermissionService.loadPermissionCodes(authUser);
        return UserInfoVO.builder()
                .userId(authUser.getUserId())
                .accountName(authUser.getAccountName())
                .userName(authUser.getUserName())
                .tenantId(authUser.getTenantId())
                .tenantCode(authUser.getTenantCode())
                .deptId(authUser.getDeptId())
                .email(authUser.getEmail())
                .avatarUrl(authUser.getAvatarUrl())
                .roleCodes(authUser.getRoleCodes())
                .permissionCodes(permissionCodes)
                .build();
    }

    private AccountLoginRecord loadAccount(LoginRequest request) {
        if (request.getTenantCode() != null && !request.getTenantCode().isBlank()) {
            AccountLoginRecord account = authQueryMapper.findAccountByTenantCodeAndLoginName(request.getTenantCode(), request.getLoginName());
            if (account == null) {
                throw new AuthException("AUTH_401", "用户名或密码错误");
            }
            return account;
        }

        List<AccountLoginRecord> accounts = authQueryMapper.findAccountsByLoginName(request.getLoginName());
        if (accounts.isEmpty()) {
            throw new AuthException("AUTH_401", "用户名或密码错误");
        }
        if (accounts.size() > 1) {
            throw new AuthException("AUTH_400", "该账号存在多租户归属，请补充 tenantCode");
        }
        return accounts.get(0);
    }

    private void validateAccountStatus(AccountLoginRecord account) {
        if (!"ENABLED".equalsIgnoreCase(account.getTenantStatus())) {
            throw new AuthException("AUTH_403", "租户已停用");
        }
        if (!"ENABLED".equalsIgnoreCase(account.getUserStatus())) {
            throw new AuthException("AUTH_403", "用户已停用");
        }
        if (!"ACTIVE".equalsIgnoreCase(account.getCredentialStatus())) {
            throw new AuthException("AUTH_403", "账号凭证不可用");
        }
        if (account.getLockedUntil() != null && account.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new AuthException("AUTH_423", "账号已锁定，请稍后再试");
        }
    }

    private Set<String> loadRoleCodes(AccountLoginRecord account) {
        Set<String> roleCodes = new LinkedHashSet<>(authQueryMapper.findRoleCodes(account.getTenantId(), account.getUserId()));
        if (roleCodes.isEmpty() && RoleCode.isBuiltIn(account.getUserType())) {
            roleCodes.add(account.getUserType().toUpperCase());
        }
        if (roleCodes.isEmpty()) {
            roleCodes.add(RoleCode.EMPLOYEE.name());
        }
        return RoleCode.normalize(roleCodes);
    }

    private void persistSession(AccountLoginRecord account,
                                String sessionId,
                                String tokenVersion,
                                HttpServletRequest servletRequest) {
        LocalDateTime now = LocalDateTime.now();
        SysLoginSessionEntity entity = new SysLoginSessionEntity();
        entity.setSessionId(sessionId);
        entity.setUserId(account.getUserId());
        entity.setTenantId(account.getTenantId());
        entity.setAccountId(account.getAccountId());
        entity.setTokenVersion(tokenVersion);
        entity.setLoginIp(extractClientIp(servletRequest));
        entity.setLoginDevice(servletRequest.getHeader("User-Agent"));
        entity.setLoginOs("UNKNOWN");
        entity.setLoginBrowser("UNKNOWN");
        entity.setStatus("ACTIVE");
        entity.setLoginAt(now);
        entity.setLastAccessAt(now);
        entity.setExpiredAt(now.plusSeconds(jwtTokenService.getExpireSeconds()));
        loginSessionMapper.insert(entity);
    }

    private UserInfoVO buildUserInfo(AccountLoginRecord account, Set<String> roleCodes, Set<String> permissionCodes) {
        return UserInfoVO.builder()
                .userId(account.getUserId())
                .accountName(account.getUsername())
                .userName(resolveDisplayName(account))
                .tenantId(account.getTenantId())
                .tenantCode(account.getTenantCode())
                .deptId(account.getDeptId())
                .email(account.getEmail())
                .avatarUrl(account.getAvatarUrl())
                .roleCodes(roleCodes)
                .permissionCodes(permissionCodes)
                .build();
    }

    private String resolveDisplayName(AccountLoginRecord account) {
        if (account.getRealName() != null && !account.getRealName().isBlank()) {
            return account.getRealName();
        }
        if (account.getNickname() != null && !account.getNickname().isBlank()) {
            return account.getNickname();
        }
        return account.getUsername();
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
