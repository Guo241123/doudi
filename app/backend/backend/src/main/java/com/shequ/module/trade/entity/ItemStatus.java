package com.shequ.module.trade.entity;

/**
 * 二手物品状态（接口文档 1.6）与 status 数值的映射。
 * <p>
 * 库里存 TINYINT（0~4），接口只暴露大写枚举名，避免前端硬编码数字。
 */
public enum ItemStatus {

    /** 待审核（先审后发） */
    UNDER_REVIEW(0),

    /** 在售 */
    ON_SALE(1),

    /** 已售 */
    SOLD(2),

    /** 已拒绝 */
    REJECTED(3),

    /** 已下架（含超期自动下架） */
    OFF_SHELF(4);

    private final int code;

    ItemStatus(int code) {
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
        for (ItemStatus status : values()) {
            if (status.code == code) {
                return status.name();
            }
        }
        return null;
    }

    /** 枚举名 → 数值；未知返回 null。 */
    public static Integer codeOf(String label) {
        if (label == null) {
            return null;
        }
        for (ItemStatus status : values()) {
            if (status.name().equals(label)) {
                return status.code;
            }
        }
        return null;
    }

}
