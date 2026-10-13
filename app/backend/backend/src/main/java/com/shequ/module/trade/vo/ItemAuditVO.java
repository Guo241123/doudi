package com.shequ.module.trade.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 发布 / 编辑二手返回体（接口文档 2.3 #20 / #21）：{@code { id, auditStatus }}。
 */
@Data
@AllArgsConstructor
public class ItemAuditVO {

    private Long id;

    /** 提交后的状态：UNDER_REVIEW 待审核 / ON_SALE 在售（auto-approve 开启时） */
    private String auditStatus;

}
