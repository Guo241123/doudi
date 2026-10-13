package com.shequ.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shequ.module.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户数据访问。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 我的发布 - 二手数（接口文档 #14）。
     * <p>
     * 说明：user 是基础模块，trade / forum 均依赖 user，若反向注入其 Service 会形成循环依赖，
     * 故这里直接按 user_id 聚合计数（只读、走 idx_user 索引）。
     */
    @Select("SELECT COUNT(*) FROM t_trade_item WHERE user_id = #{userId} AND is_deleted = 0")
    long countTradeItems(@Param("userId") long userId);

    /**
     * 我的发布 - 帖子数（接口文档 #14）：排除软删与已删帖（status=1）。
     */
    @Select("SELECT COUNT(*) FROM t_post WHERE user_id = #{userId} AND is_deleted = 0 AND status <> 1")
    long countPosts(@Param("userId") long userId);

}
