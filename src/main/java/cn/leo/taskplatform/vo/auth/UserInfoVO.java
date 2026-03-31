package cn.leo.taskplatform.vo.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoVO {

    private String userId;
    private String accountName;
    private String userName;
    private String tenantId;
    private String tenantCode;
    private String deptId;
    private String email;
    private String avatarUrl;
    private Set<String> roleCodes;
    private Set<String> permissionCodes;
}
