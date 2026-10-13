package com.shequ.module.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shequ.module.message.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会话表 Mapper。
 */
@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {

}
