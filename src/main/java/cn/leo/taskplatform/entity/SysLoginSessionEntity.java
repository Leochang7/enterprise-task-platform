package cn.leo.taskplatform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@TableName("sys_login_session")
@EqualsAndHashCode(callSuper = true)
public class SysLoginSessionEntity extends BaseEntity {

    private String sessionId;
    private String userId;
    private String tenantId;
    private String accountId;
    private String tokenVersion;
    private String loginIp;
    private String loginDevice;
    private String loginOs;
    private String loginBrowser;
    private String status;
    private LocalDateTime loginAt;
    private LocalDateTime lastAccessAt;
    private LocalDateTime expiredAt;
    private LocalDateTime logoutAt;
}
