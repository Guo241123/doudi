package com.shequ.module.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shequ.module.message.entity.Message;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息表 Mapper。
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {

}
