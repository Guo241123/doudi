package com.shequ.module.forum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shequ.module.forum.entity.Post;
import org.apache.ibatis.annotations.Mapper;

/**
 * 帖子数据访问。
 */
@Mapper
public interface PostMapper extends BaseMapper<Post> {

}
