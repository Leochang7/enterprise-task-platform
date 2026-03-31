package cn.leo.taskplatform.mapper;

import cn.leo.taskplatform.entity.SysLoginSessionEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LoginSessionMapper extends BaseMapper<SysLoginSessionEntity> {
}
