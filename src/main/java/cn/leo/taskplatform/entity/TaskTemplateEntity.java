package cn.leo.taskplatform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("task_template")
@EqualsAndHashCode(callSuper = true)
public class TaskTemplateEntity extends BaseEntity {

    private String templateCode;
    private String templateName;
    private String taskType;
    private Integer templateVersion;
    private String status;
    private String workflowDefinition;
    private String inputSchema;
    private String outputSchema;
    private String defaultPriority;
    private Integer timeoutSeconds;
    private String ownerTeam;
    private String description;
    private String createdBy;
    private String updatedBy;
    private Integer isDeleted;
}
