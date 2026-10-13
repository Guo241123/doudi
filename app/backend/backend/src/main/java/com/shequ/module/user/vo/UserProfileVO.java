package com.shequ.module.user.vo;

import com.shequ.module.user.entity.User;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 我的资料（接口文档 #10）：User 对象（1.7）+ 脱敏手机号。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserProfileVO extends UserVO {

    /** 脱敏手机号，如 138****5678 */
    private String mobile;

    public static UserProfileVO from(User user, String maskedMobile) {
        UserProfileVO vo = new UserProfileVO();
        vo.setUserId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setGender(user.getGender());
        vo.setBio(user.getBio());
        vo.setRegisterTime(user.getRegisterTime());
        vo.setMobile(maskedMobile);
        return vo;
    }

}
