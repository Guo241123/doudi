package com.shequ.common.favorite;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收藏表实体（数据库设计 五、11）：二手与帖子共用一张表，靠 target_type 区分。
 * <p>
 * 放在 common 而非具体业务模块，是为了让 trade / forum 都只依赖 {@link FavoriteService}，
 * 不跨模块直查表（架构 ADR-001，与 media 一致）。
 * <p>
 * 无 is_deleted 字段：关系记录走物理删除，取消后唯一键可复用（重复收藏幂等）。
 */
@Data
@TableName("t_favorite")
public class Favorite {

    /** 收藏目标：二手物品 */
    public static final int TARGET_ITEM = 1;

    /** 收藏目标：帖子 */
    public static final int TARGET_POST = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 收藏人 ID */
    private Long userId;

    /** 目标类型，取值见本类常量 */
    private Integer targetType;

    /** 二手物品 ID 或帖子 ID */
    private Long targetId;

    /** 收藏时间 */
    private LocalDateTime createTime;

}
