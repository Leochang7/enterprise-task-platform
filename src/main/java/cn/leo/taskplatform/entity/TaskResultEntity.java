package cn.leo.taskplatform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("task_result")
@EqualsAndHashCode(callSuper = true)
public class TaskResultEntity extends BaseEntity {

    private String taskId;
    private String resultType;
    private String resultStatus;
    private String resultSummary;
    private String resultPayload;
    private Integer attachmentCount;
    private String attachmentManifest;
    private String storageProvider;
    private String storagePath;
    private String checksum;
}
