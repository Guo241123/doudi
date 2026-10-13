package com.shequ.module.trade.vo;

import lombok.Data;

/**
 * 交易分类（接口文档 2.3 #17）。
 */
@Data
public class CategoryVO {

    private Long id;

    private String name;

    /** 排序（即 2×4 八宫格顺序） */
    private Integer sort;

    /** 图标完整 URL */
    private String iconUrl;

}
