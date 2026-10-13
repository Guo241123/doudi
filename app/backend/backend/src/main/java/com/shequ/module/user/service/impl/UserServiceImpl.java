package com.shequ.module.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shequ.common.auth.TokenService;
import com.shequ.common.exception.BusinessException;
import com.shequ.common.result.ErrorCode;
import com.shequ.common.sms.SmsCodeVerifier;
import com.shequ.common.storage.FileUrlResolver;
import com.shequ.module.message.service.MessageService;
import com.shequ.module.user.dto.MobileChangeDTO;
import com.shequ.module.user.dto.PasswordChangeDTO;
import com.shequ.module.user.dto.ProfileUpdateDTO;
import com.shequ.module.user.entity.User;
import com.shequ.module.user.mapper.UserMapper;
import com.shequ.module.user.service.UserService;
import com.shequ.module.user.vo.UserProfileVO;
import com.shequ.module.user.vo.UserSummaryVO;
import com.shequ.module.user.vo.UserVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 用户基础服务 + 个人资料与账号安全实现（接口文档 2.1 #10～#14）。
 * <p>
 * 高危操作统一规则（实现注记 10）：换手机号 / 改密码成功后清除该用户全部设备 token，需重新登录。
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final String SCENE_CHANGE_MOBILE = "CHANGE_MOBILE";
    private static final String SCENE_RESET = "RESET";

    private final PasswordEncoder passwordEncoder;
    private final SmsCodeVerifier smsCodeVerifier;
    private final TokenService tokenService;
    private final FileUrlResolver fileUrlResolver;
    /** 概览要汇总未读数：MessageService 反向依赖 UserService，故用 @Lazy 打断构造期循环。 */
    private final MessageService messageService;

    public UserServiceImpl(PasswordEncoder passwordEncoder,
                           SmsCodeVerifier smsCodeVerifier,
                           TokenService tokenService,
                           FileUrlResolver fileUrlResolver,
                           @Lazy MessageService messageService) {
        this.passwordEncoder = passwordEncoder;
        this.smsCodeVerifier = smsCodeVerifier;
        this.tokenService = tokenService;
        this.fileUrlResolver = fileUrlResolver;
        this.messageService = messageService;
    }

    @Override
    public User getByMobile(String mobile) {
        return lambdaQuery().eq(User::getMobile, mobile).one();
    }

    // ------------------------------------------------------------------ #10 我的资料

    @Override
    public UserProfileVO getProfile(long userId) {
        User user = requireUser(userId);
        return UserProfileVO.from(user, maskMobile(user.getMobile()));
    }

    // ------------------------------------------------------------------ #11 编辑资料

    @Override
    public UserVO updateProfile(long userId, ProfileUpdateDTO dto) {
        User user = requireUser(userId);

        if (dto.getNickname() != null) {
            String nickname = dto.getNickname().trim();
            if (nickname.isEmpty()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "昵称不能为空");
            }
            user.setNickname(nickname);
        }
        if (StringUtils.hasText(dto.getGender())) {
            user.setGender(dto.getGender());
        }
        if (dto.getBio() != null) {
            user.setBio(dto.getBio());
        }
        if (StringUtils.hasText(dto.getAvatarKey())) {
            user.setAvatarUrl(resolveUrl(dto.getAvatarKey()));
        }
        user.setUpdateTime(LocalDateTime.now());
        updateById(user);
        return UserVO.from(user);
    }

    // ------------------------------------------------------------------ #12 更换手机号

    @Override
    public void changeMobile(long userId, MobileChangeDTO dto) {
        User user = requireUser(userId);
        String newMobile = dto.getNewMobile();
        if (newMobile.equals(user.getMobile())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "新手机号与当前手机号相同");
        }
        if (getByMobile(newMobile) != null) {
            throw new BusinessException(ErrorCode.DUPLICATE_OPERATION, "手机号已注册，请直接登录");
        }
        smsCodeVerifier.verify(SCENE_CHANGE_MOBILE, newMobile, dto.getSmsCode());

        user.setMobile(newMobile);
        user.setUpdateTime(LocalDateTime.now());
        try {
            updateById(user);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_OPERATION, "手机号已注册，请直接登录");
        }
        // 高危操作：换绑成功后清除全部设备 token，用户需重新登录（防 token 被盗后改绑盗号）
        tokenService.revokeAll(userId);
    }

    // ------------------------------------------------------------------ #13 修改密码

    @Override
    public void changePassword(long userId, PasswordChangeDTO dto) {
        User user = requireUser(userId);
        boolean hasOld = StringUtils.hasText(dto.getOldPassword());
        boolean hasSms = StringUtils.hasText(dto.getSmsCode());
        if (!hasOld && !hasSms) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请提供原密码或短信验证码");
        }
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "两次输入的密码不一致");
        }
        if (hasOld) {
            if (!passwordEncoder.matches(dto.getOldPassword(), user.getPasswordHash())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "原密码不正确");
            }
        } else {
            smsCodeVerifier.verify(SCENE_RESET, user.getMobile(), dto.getSmsCode());
        }

        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        user.setUpdateTime(LocalDateTime.now());
        updateById(user);
        // 高危操作：改密后清除全部设备 token，用户需重新登录
        tokenService.revokeAll(userId);
    }

    // ------------------------------------------------------------------ #14 我的信息概览

    @Override
    public UserSummaryVO getSummary(long userId) {
        User user = requireUser(userId);
        UserSummaryVO vo = new UserSummaryVO();
        vo.setUser(UserVO.from(user));
        // 未读数实时取自站内信 Redis 计数器（进会话即清零）
        vo.setUnreadCount(messageService.unreadCount(userId).getTotal());
        vo.setMyItemCount((int) baseMapper.countTradeItems(userId));
        vo.setMyPostCount((int) baseMapper.countPosts(userId));
        return vo;
    }

    // ------------------------------------------------------------------ 私有方法

    private User requireUser(long userId) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }

    /** 手机号脱敏：138****5678。 */
    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() != 11) {
            return mobile;
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(7);
    }

    /**
     * 头像 URL 组装：统一走 {@link FileUrlResolver}（本地存储 → /files/**；OSS → endpoint）。
     */
    private String resolveUrl(String fileKey) {
        return fileUrlResolver.resolve(fileKey);
    }

}
