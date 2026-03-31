package cn.leo.taskplatform.auth;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AccountLoginRecord {

    private String accountId;
    private String userId;
    private String tenantId;
    private String tenantCode;
    private String tenantName;
    private String tenantStatus;
    private String loginName;
    private String passwordHash;
    private String passwordSalt;
    private String credentialStatus;
    private LocalDateTime lockedUntil;
    private String username;
    private String realName;
    private String nickname;
    private String email;
    private String avatarUrl;
    private String deptId;
    private String userStatus;
    private String userType;
}
