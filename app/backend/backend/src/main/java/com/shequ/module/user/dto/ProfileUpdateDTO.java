package com.shequ.module.user.dto;

import com.shequ.common.constant.ValidationConst;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 编辑资料（接口文档 #11）。四个字段均可选，只更新传入的项。
 */
@Data
public class ProfileUpdateDTO {

    @Size(min = 1, max = 12, message = "昵称需 1～12 字")
    private String nickname;

    @Pattern(regexp = ValidationConst.GENDER, message = "性别只能是 M/F/U")
    private String gender;

    @Size(max = 100, message = "简介最多 100 字")
    private String bio;

    /** 新头像的 fileKey；不传则保留原头像 */
    private String avatarKey;

}
