package com.shequ.module.report.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shequ.common.exception.BusinessException;
import com.shequ.common.result.CursorResult;
import com.shequ.common.result.ErrorCode;
import com.shequ.module.report.entity.Blacklist;
import com.shequ.module.report.mapper.BlacklistMapper;
import com.shequ.module.report.service.BlacklistService;
import com.shequ.module.report.vo.BlacklistItemVO;
import com.shequ.module.user.entity.User;
import com.shequ.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 拉黑服务实现：黑名单量小（个人维度上限几十人），直接服务层查询，
 * 三口过滤（信息流 / 评论列表 / 站内信）调 {@link #hiddenUserIds(long)} 即可。
 */
@Service
@RequiredArgsConstructor
public class BlacklistServiceImpl implements BlacklistService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final BlacklistMapper blacklistMapper;
    private final UserService userService;

    @Override
    public void block(long userId, long targetUserId) {
        if (userId == targetUserId) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能拉黑自己");
        }
        User target = userService.getById(targetUserId);
        if (target == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        Blacklist row = new Blacklist();
        row.setUserId(userId);
        row.setBlockedUserId(targetUserId);
        row.setCreateTime(LocalDateTime.now());
        try {
            blacklistMapper.insert(row);
        } catch (DuplicateKeyException e) {
            // 唯一键兜底：重复拉黑幂等成功
        }
    }

    @Override
    public void unblock(long userId, long targetUserId) {
        blacklistMapper.delete(Wrappers.<Blacklist>lambdaQuery()
                .eq(Blacklist::getUserId, userId)
                .eq(Blacklist::getBlockedUserId, targetUserId));
    }

    @Override
    public CursorResult<BlacklistItemVO> list(long userId, int cursor, int size) {
        int pageSize = normalizeSize(size);
        List<Blacklist> rows = blacklistMapper.selectList(Wrappers.<Blacklist>lambdaQuery()
                .eq(Blacklist::getUserId, userId)
                .lt(cursor > 0, Blacklist::getId, cursor)
                .orderByDesc(Blacklist::getId)
                .last("LIMIT " + (pageSize + 1)));

        boolean hasMore = rows.size() > pageSize;
        List<Blacklist> page = hasMore ? new ArrayList<>(rows.subList(0, pageSize)) : rows;

        Set<Long> targetIds = page.stream().map(Blacklist::getBlockedUserId).collect(Collectors.toSet());
        Map<Long, User> users = targetIds.isEmpty() ? Map.of()
                : userService.listByIds(targetIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));

        List<BlacklistItemVO> list = new ArrayList<>(page.size());
        for (Blacklist row : page) {
            User user = users.get(row.getBlockedUserId());
            list.add(new BlacklistItemVO(
                    row.getBlockedUserId(),
                    user == null ? "已注销用户" : user.getNickname(),
                    user == null ? null : user.getAvatarUrl(),
                    row.getCreateTime()));
        }
        long nextCursor = page.isEmpty() ? 0 : page.get(page.size() - 1).getId();
        return new CursorResult<>(list, nextCursor, hasMore);
    }

    @Override
    public Set<Long> blockedByMe(long userId) {
        return blacklistMapper.selectList(Wrappers.<Blacklist>lambdaQuery()
                        .eq(Blacklist::getUserId, userId))
                .stream().map(Blacklist::getBlockedUserId).collect(Collectors.toSet());
    }

    @Override
    public Set<Long> blockedMe(long userId) {
        return blacklistMapper.selectList(Wrappers.<Blacklist>lambdaQuery()
                        .eq(Blacklist::getBlockedUserId, userId))
                .stream().map(Blacklist::getUserId).collect(Collectors.toSet());
    }

    @Override
    public Set<Long> hiddenUserIds(long userId) {
        Set<Long> hidden = new HashSet<>(blockedByMe(userId));
        hidden.addAll(blockedMe(userId));
        return hidden;
    }

    @Override
    public boolean blockedBetween(long userIdA, long userIdB) {
        if (userIdA == userIdB) {
            return false;
        }
        return Boolean.TRUE.equals(blacklistMapper.exists(Wrappers.<Blacklist>lambdaQuery()
                .and(q -> q.eq(Blacklist::getUserId, userIdA).eq(Blacklist::getBlockedUserId, userIdB))
                .or(q -> q.eq(Blacklist::getUserId, userIdB).eq(Blacklist::getBlockedUserId, userIdA))));
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

}
