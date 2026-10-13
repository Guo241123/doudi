package com.shequ.module.report.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 拉黑表实体（数据库设计：双方内容互不可见）。
 * 无 is_deleted：关系记录用物理删除，唯一键保证幂等。
 */
@Data
@TableName("t_blacklist")
public class Blacklist {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 拉黑人 ID */
    private Long userId;

    /** 被拉黑人 ID */
    private Long blockedUserId;

    private LocalDateTime createTime;

}
