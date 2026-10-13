package com.shequ.module.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shequ.module.task.entity.Task;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务数据访问。
 */
@Mapper
public interface TaskMapper extends BaseMapper<Task> {

}
