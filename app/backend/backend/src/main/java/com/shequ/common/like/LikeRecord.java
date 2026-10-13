package com.shequ.common.like;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 点赞记录表实体（数据库设计 五、10）：帖子与评论共用一张表，靠 target_type 区分。
 * <p>
 * 放在 common 而非具体业务模块，是为了让 forum 等模块都只依赖 {@link LikeService}，
 * 不跨模块直查表（架构 ADR-001，与 media / favorite 一致）。
 * <p>
 * 无 is_deleted 字段：关系记录走物理删除，唯一键 uk_user_target 保证幂等（取消后可再次点赞）。
 */
@Data
@TableName("t_like_record")
public class LikeRecord {

    /** 点赞目标：帖子 */
    public static final int TARGET_POST = 1;

    /** 点赞目标：评论 */
    public static final int TARGET_COMMENT = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 点赞人 ID */
    private Long userId;

    /** 目标类型，取值见本类常量 */
    private Integer targetType;

    /** 帖子 ID 或评论 ID */
    private Long targetId;

    /** 点赞时间 */
    private LocalDateTime createTime;

}
