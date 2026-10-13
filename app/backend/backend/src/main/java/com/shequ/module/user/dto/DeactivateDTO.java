package com.shequ.module.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 注销账号（接口文档 #9）。
 */
@Data
public class DeactivateDTO {

    /** 前端二次确认后传 true；传 false 直接 40001 */
    @NotNull(message = "confirm 不能为空")
    private Boolean confirm;

}