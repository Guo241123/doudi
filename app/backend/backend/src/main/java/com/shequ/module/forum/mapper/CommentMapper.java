package com.shequ.module.forum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shequ.module.forum.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 评论数据访问。
 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    /**
     * 批量取多个帖子各自点赞数最高的前 N 条评论（信息流高赞评论预览），一次查询避免 N+1。
     * <p>
     * 依赖 MySQL 8 窗口函数按 post_id 分组排名；RN 列不映射到实体，自动忽略。
     *
     * @param postIds 帖子 ID 集合（非空）
     * @param limit   每个帖子取前几条
     */
    @Select("<script>"
            + "SELECT id, post_id, user_id, content, reply_to_id, like_count, create_time, update_time FROM ("
            + "  SELECT id, post_id, user_id, content, reply_to_id, like_count, create_time, update_time,"
            + "         ROW_NUMBER() OVER (PARTITION BY post_id ORDER BY like_count DESC, id ASC) AS rn"
            + "  FROM t_comment"
            + "  WHERE is_deleted = 0 AND post_id IN"
            + "  <foreach item='pid' collection='postIds' open='(' separator=',' close=')'>#{pid}</foreach>"
            + ") t WHERE t.rn &lt;= #{limit}"
            + " ORDER BY t.post_id ASC, t.rn ASC"
            + "</script>")
    List<Comment> selectTopLikedByPostIds(@Param("postIds") Collection<Long> postIds, @Param("limit") int limit);

}
