package com.shequ.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表实体。
 */
@Data
@TableName("t_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 手机号（11 位） */
    private String mobile;

    /** 密码加盐哈希（BCrypt），禁止明文 */
    private String passwordHash;

    /** 昵称（1～12 字，允许重名） */
    private String nickname;

    /** 性别 M / F / U */
    private String gender;

    /** 头像完整 URL（OSS + CDN） */
    private String avatarUrl;

    /** 个人简介，≤100 字 */
    private String bio;

    /** 状态，取值见 {@link UserStatus} */
    private String status;

    /** 封禁原因（登录时回传） */
    private String banReason;

    /** 注销处理截止时间 */
    private LocalDateTime deactivateDeadline;

    private LocalDateTime registerTime;

    private LocalDateTime updateTime;

    /** 软删除：0 正常 / 1 已删除 */
    private Integer isDeleted;

}