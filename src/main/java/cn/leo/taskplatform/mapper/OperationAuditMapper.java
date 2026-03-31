package cn.leo.taskplatform.mapper;

import cn.leo.taskplatform.entity.SysOperationAuditEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OperationAuditMapper extends BaseMapper<SysOperationAuditEntity> {
}
