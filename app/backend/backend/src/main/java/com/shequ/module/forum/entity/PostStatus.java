package com.shequ.module.forum.entity;

/**
 * 帖子状态（接口文档 1.6）与 status 数值的映射。
 * <p>
 * 库里存 TINYINT，接口只暴露大写枚举名，避免前端硬编码数字。
 * 帖子为"先发后审"，故没有待审核状态。
 */
public enum PostStatus {

    /** 已发布 */
    PUBLISHED(0),

    /** 已删除（软删除标记，#35 仅本人可删） */
    DELETED(1),

    /** 公告（醒目样式，最新一条固定置顶） */
    ANNOUNCEMENT(2);

    private final int code;

    PostStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    /** 数值 → 枚举名；未知返回 null。 */
    public static String labelOf(Integer code) {
        if (code == null) {
            return null;
        }
        for (PostStatus status : values()) {
            if (status.code == code) {
                return status.name();
            }
        }
        return null;
    }

}
