package com.shequ.module.user.vo;

import com.shequ.module.user.entity.User;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户对象（接口文档 1.7，各接口复用）。
 */
@Data
public class UserVO {

    private Long userId;
    private String nickname;
    private String avatarUrl;
    private String gender;
    private String bio;

    /** 注册时间（仅本人资料返回） */
    private LocalDateTime registerTime;

    public static UserVO from(User user) {
        UserVO vo = new UserVO();
        vo.userId = user.getId();
        vo.nickname = user.getNickname();
        vo.avatarUrl = user.getAvatarUrl();
        vo.gender = user.getGender();
        vo.bio = user.getBio();
        vo.registerTime = user.getRegisterTime();
        return vo;
    }

}