package cn.leo.taskplatform.mapper;

import cn.leo.taskplatform.auth.AccountLoginRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AuthQueryMapper {

    @Select("""
            SELECT
                a.account_id AS accountId,
                a.user_id AS userId,
                a.tenant_id AS tenantId,
                a.login_name AS loginName,
                a.password_hash AS passwordHash,
                a.password_salt AS passwordSalt,
                a.credential_status AS credentialStatus,
                a.locked_until AS lockedUntil,
                u.username AS username,
                u.real_name AS realName,
                u.nickname AS nickname,
                u.email AS email,
                u.avatar_url AS avatarUrl,
                u.dept_id AS deptId,
                u.status AS userStatus,
                u.user_type AS userType,
                t.tenant_code AS tenantCode,
                t.tenant_name AS tenantName,
                t.status AS tenantStatus
            FROM sys_user_account a
            INNER JOIN sys_user u
                ON u.user_id = a.user_id
               AND u.tenant_id = a.tenant_id
               AND u.is_deleted = 0
            INNER JOIN sys_tenant t
                ON t.tenant_id = a.tenant_id
               AND t.is_deleted = 0
            WHERE a.login_name = #{loginName}
            ORDER BY a.id DESC
            """)
    List<AccountLoginRecord> findAccountsByLoginName(@Param("loginName") String loginName);

    @Select("""
            SELECT
                a.account_id AS accountId,
                a.user_id AS userId,
                a.tenant_id AS tenantId,
                a.login_name AS loginName,
                a.password_hash AS passwordHash,
                a.password_salt AS passwordSalt,
                a.credential_status AS credentialStatus,
                a.locked_until AS lockedUntil,
                u.username AS username,
                u.real_name AS realName,
                u.nickname AS nickname,
                u.email AS email,
                u.avatar_url AS avatarUrl,
                u.dept_id AS deptId,
                u.status AS userStatus,
                u.user_type AS userType,
                t.tenant_code AS tenantCode,
                t.tenant_name AS tenantName,
                t.status AS tenantStatus
            FROM sys_user_account a
            INNER JOIN sys_user u
                ON u.user_id = a.user_id
               AND u.tenant_id = a.tenant_id
               AND u.is_deleted = 0
            INNER JOIN sys_tenant t
                ON t.tenant_id = a.tenant_id
               AND t.is_deleted = 0
            WHERE a.login_name = #{loginName}
              AND t.tenant_code = #{tenantCode}
            ORDER BY a.id DESC
            LIMIT 1
            """)
    AccountLoginRecord findAccountByTenantCodeAndLoginName(@Param("tenantCode") String tenantCode,
                                                           @Param("loginName") String loginName);

    @Select("""
            SELECT DISTINCT r.role_code
            FROM sys_user_role ur
            INNER JOIN sys_role r
                ON r.role_id = ur.role_id
               AND r.tenant_id = ur.tenant_id
               AND r.status = 'ENABLED'
               AND r.is_deleted = 0
            WHERE ur.tenant_id = #{tenantId}
              AND ur.user_id = #{userId}
            """)
    List<String> findRoleCodes(@Param("tenantId") String tenantId, @Param("userId") String userId);

    @Select("""
            SELECT DISTINCT p.permission_code
            FROM sys_user_role ur
            INNER JOIN sys_role_permission rp
                ON rp.role_id = ur.role_id
               AND rp.tenant_id = ur.tenant_id
            INNER JOIN sys_permission p
                ON p.permission_id = rp.permission_id
               AND p.tenant_id IN ('SYSTEM', #{tenantId})
               AND p.status = 'ENABLED'
               AND p.is_deleted = 0
            WHERE ur.tenant_id = #{tenantId}
              AND ur.user_id = #{userId}
            """)
    List<String> findPermissionCodes(@Param("tenantId") String tenantId, @Param("userId") String userId);

    @Update("""
            UPDATE sys_user
            SET last_login_at = #{lastLoginAt},
                last_login_ip = #{lastLoginIp},
                updated_at = #{lastLoginAt}
            WHERE tenant_id = #{tenantId}
              AND user_id = #{userId}
            """)
    int updateUserLoginInfo(@Param("tenantId") String tenantId,
                            @Param("userId") String userId,
                            @Param("lastLoginAt") LocalDateTime lastLoginAt,
                            @Param("lastLoginIp") String lastLoginIp);

    @Update("""
            UPDATE sys_user_account
            SET failed_login_count = failed_login_count + 1,
                updated_at = NOW(3)
            WHERE account_id = #{accountId}
            """)
    int incrementFailedLoginCount(@Param("accountId") String accountId);

    @Update("""
            UPDATE sys_user_account
            SET failed_login_count = 0,
                locked_until = NULL,
                updated_at = NOW(3)
            WHERE account_id = #{accountId}
            """)
    int clearFailedLoginState(@Param("accountId") String accountId);

    @Update("""
            UPDATE sys_login_session
            SET status = 'OFFLINE',
                logout_at = #{logoutAt},
                updated_at = #{logoutAt}
            WHERE session_id = #{sessionId}
            """)
    int offlineSession(@Param("sessionId") String sessionId, @Param("logoutAt") LocalDateTime logoutAt);
}
