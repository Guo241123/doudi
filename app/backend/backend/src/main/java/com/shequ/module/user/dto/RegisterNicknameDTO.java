package com.shequ.module.user.dto;

import com.shequ.common.constant.ValidationConst;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册③：设置昵称，完成注册（接口文档 #4）。
 */
@Data
public class RegisterNicknameDTO {

    @NotBlank(message = "注册凭证不能为空")
    private String registerToken;

    @NotBlank(message = "昵称不能为空")
    @Size(min = 1, max = 12, message = "昵称需 1～12 字")
    private String nickname;

    @Pattern(regexp = ValidationConst.GENDER, message = "性别只能是 M/F/U")
    private String gender;

    /** 上传后返回的 fileKey；不传用默认头像 */
    private String avatarKey;

}