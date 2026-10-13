package com.shequ.module.trade.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 我的发布列表项（接口文档 2.3 #23）：#18 字段 + 审核状态 + 拒绝原因。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MyItemVO extends ItemCardVO {

    /** UNDER_REVIEW / ON_SALE / REJECTED / OFF_SHELF */
    private String auditStatus;

    /** 拒绝原因（审核不通过时有值） */
    private String rejectReason;

}
