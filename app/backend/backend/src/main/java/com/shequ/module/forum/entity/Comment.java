package com.shequ.module.forum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论表实体（数据库设计 五、9）。
 * <p>
 * reply_to_id 只支持两级：回复的回复仍指向根评论或被回复评论，前端拼"@xxx"，不做无限嵌套。
 */
@Data
@TableName("t_comment")
public class Comment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属帖子 ID */
    private Long postId;

    /** 评论人 ID */
    private Long userId;

    /** 内容（≤200 字） */
    private String content;

    /** 被回复评论 ID（可空） */
    private Long replyToId;

    /** 点赞数冗余 */
    private Integer likeCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 软删除：0 正常 / 1 已删除 */
    private Integer isDeleted;

}
