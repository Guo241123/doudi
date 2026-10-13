package com.shequ.module.forum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子表实体（数据库设计 五、8）。
 * <p>
 * like_count / comment_count 为冗余计数列：点赞 / 评论时同步增减，避免信息流对每条帖子 COUNT。
 */
@Data
@TableName("t_post")
public class Post {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发布者用户 ID（公告帖为管理员账号） */
    private Long userId;

    /** 文字内容（≤1000 字） */
    private String content;

    /** 状态，取值见 {@link PostStatus} */
    private Integer status;

    /** 点赞数冗余 */
    private Integer likeCount;

    /** 评论数冗余 */
    private Integer commentCount;

    /** 创建时间（即发布时间） */
    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 软删除：0 正常 / 1 已删除 */
    private Integer isDeleted;

}
