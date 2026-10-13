package com.shequ.module.trade.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 卖家状态操作请求体（接口文档 2.3 #22）。
 */
@Data
public class ItemStatusActionDTO {

    /** OFF_SHELF 下架 / MARK_SOLD 标记已售 / PUT_ON 重新上架 */
    @NotBlank(message = "action 不能为空")
    @Pattern(regexp = "OFF_SHELF|MARK_SOLD|PUT_ON", message = "action 取值非法")
    private String action;

}
