package com.shequ.job;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shequ.common.auth.TokenService;
import com.shequ.common.constant.RedisKeys;
import com.shequ.module.user.entity.User;
import com.shequ.module.user.entity.UserStatus;
import com.shequ.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 注销清理定时任务（任务模块审查报告缺口 2：注销到期后账号状态流转 + 数据清理）。
 * <p>
 * 每天凌晨 3 点扫一遍：status=DEACTIVATING 且 deactivate_deadline 已过的用户——
 * ① 状态流转 DEACTIVATING → DELETED；② 软删（is_deleted=1）；
 * ③ 昵称改"已注销用户"、头像/简介清空、手机号改写为不可用值（释放给重新注册，
 * 唯一键 uk_mobile 不受影响）；④ 清全部 token 与封禁键。
 * 物理清数据（删帖、删物品）等真正有需要再做，当前只做状态与资料清理（报告建议口径）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeactivateCleanupJob {

    private final UserMapper userMapper;
    private final TokenService tokenService;
    private final StringRedisTemplate redis;

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanup() {
        List<User> expired = userMapper.selectList(Wrappers.<User>lambdaQuery()
                .eq(User::getStatus, UserStatus.DEACTIVATING.name())
                .lt(User::getDeactivateDeadline, LocalDateTime.now())
                .last("LIMIT 500"));

        if (expired.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (User user : expired) {
            user.setStatus(UserStatus.DELETED.name());
            user.setNickname("已注销用户");
            user.setAvatarUrl(null);
            user.setBio(null);
            // 手机号释放：改成不可注册的占位值，保留唯一键语义
            user.setMobile("deleted_" + user.getId());
            user.setDeactivateDeadline(null);
            user.setUpdateTime(now);
            user.setIsDeleted(1);
            userMapper.updateById(user);

            tokenService.revokeAll(user.getId());
            redis.delete(RedisKeys.banUser(user.getId()));
        }
        log.info("注销清理完成：本次处理 {} 个到期账号", expired.size());
    }

}
