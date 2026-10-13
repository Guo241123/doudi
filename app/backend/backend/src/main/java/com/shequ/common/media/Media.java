package com.shequ.common.media;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通用媒体表实体（数据库设计 五、7）：以 biz_type + biz_id 关联任意业务，跨模块复用。
 * <p>
 * 放在 common 而非具体业务模块，是为了让 task / trade / forum / ads 都只依赖这里的
 * {@link MediaService} 接口取媒体，不跨模块直查表（架构 ADR-001）。
 */
@Data
@TableName("t_media")
public class Media {

    /** 业务类型：任务 */
    public static final String BIZ_TASK = "TASK";

    /** 业务类型：二手物品 */
    public static final String BIZ_ITEM = "ITEM";

    /** 业务类型：论坛帖子 */
    public static final String BIZ_POST = "POST";

    /** 媒体类型：图片 */
    public static final int TYPE_IMAGE = 1;

    /** 媒体类型：视频 */
    public static final int TYPE_VIDEO = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务类型 TASK / ITEM / POST / AVATAR / AD */
    private String bizType;

    /** 所属业务 ID */
    private Long bizId;

    /** 媒体类型，取值见本类常量 */
    private Integer mediaType;

    /** OSS fileKey */
    private String fileKey;

    /** 排序（轮播顺序） */
    private Integer sort;

    /** 机审状态 0 待机审 / 1 通过 / 2 不通过 */
    private Integer auditStatus;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 软删除：0 正常 / 1 已删除 */
    private Integer isDeleted;

}
