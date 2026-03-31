package cn.leo.taskplatform.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank
    private String loginName;

    @NotBlank
    private String password;

    private String tenantCode;
}
