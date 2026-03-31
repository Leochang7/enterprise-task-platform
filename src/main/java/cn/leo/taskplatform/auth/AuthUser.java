package cn.leo.taskplatform.auth;

import lombok.Builder;
import lombok.Data;

import java.util.LinkedHashSet;
import java.util.Set;

@Data
@Builder
public class AuthUser {

    private String userId;
    private String accountName;
    private String userName;
    private String tenantId;
    private String tenantCode;
    private String deptId;
    private String email;
    private String avatarUrl;
    private String sessionId;
    private String tokenVersion;
    @Builder.Default
    private Set<String> roleCodes = new LinkedHashSet<>();
}
