package com.shequ.common.favorite;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shequ.common.result.CursorResult;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 通用收藏服务实现。
 * <p>
 * 幂等靠唯一键 uk_user_target 兜底：并发重复收藏会撞唯一键，捕获后静默返回成功。
 * 游标分页与列表模块口径一致：{@code WHERE id < ? ORDER BY id DESC LIMIT size+1}。
 */
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final FavoriteMapper favoriteMapper;

    @Override
    public Set<Long> favoritedTargetIds(long userId, int targetType, Collection<Long> targetIds) {
        if (targetIds == null || targetIds.isEmpty()) {
            return Set.of();
        }
        List<Favorite> rows = favoriteMapper.selectList(Wrappers.<Favorite>lambdaQuery()
                .select(Favorite::getTargetId)
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getTargetType, targetType)
                .in(Favorite::getTargetId, targetIds));
        Set<Long> ids = new HashSet<>(rows.size());
        for (Favorite row : rows) {
            ids.add(row.getTargetId());
        }
        return ids;
    }

    @Override
    public boolean isFavorited(long userId, int targetType, long targetId) {
        Long count = favoriteMapper.selectCount(Wrappers.<Favorite>lambdaQuery()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getTargetType, targetType)
                .eq(Favorite::getTargetId, targetId));
        return count != null && count > 0;
    }

    @Override
    public boolean favorite(long userId, int targetType, long targetId) {
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setTargetType(targetType);
        favorite.setTargetId(targetId);
        favorite.setCreateTime(LocalDateTime.now());
        try {
            favoriteMapper.insert(favorite);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    @Override
    public boolean unfavorite(long userId, int targetType, long targetId) {
        int rows = favoriteMapper.delete(Wrappers.<Favorite>lambdaQuery()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getTargetType, targetType)
                .eq(Favorite::getTargetId, targetId));
        return rows > 0;
    }

    @Override
    public CursorResult<Favorite> page(long userId, int targetType, long cursor, int size) {
        int pageSize = normalizeSize(size);
        List<Favorite> rows = favoriteMapper.selectList(Wrappers.<Favorite>lambdaQuery()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getTargetType, targetType)
                .lt(cursor > 0, Favorite::getId, cursor)
                .orderByDesc(Favorite::getId)
                .last("LIMIT " + (pageSize + 1)));

        boolean hasMore = rows.size() > pageSize;
        List<Favorite> page = hasMore ? new ArrayList<>(rows.subList(0, pageSize)) : rows;
        long nextCursor = page.isEmpty() ? 0 : page.get(page.size() - 1).getId();
        return new CursorResult<>(page, nextCursor, hasMore);
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

}
