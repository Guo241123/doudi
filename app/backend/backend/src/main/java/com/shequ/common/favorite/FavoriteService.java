package com.shequ.common.favorite;

import com.shequ.common.result.CursorResult;

import java.util.Collection;
import java.util.Set;

/**
 * 通用收藏服务：跨模块收藏二手 / 帖子时只调本接口，禁止各模块直查 t_favorite。
 */
public interface FavoriteService {

    /**
     * 批量查询某用户对一组目标的收藏情况，用于列表页一次性回填 favorited，避免 N+1。
     *
     * @return 命中的目标 ID 集合（未收藏的不在其中）
     */
    Set<Long> favoritedTargetIds(long userId, int targetType, Collection<Long> targetIds);

    /** 是否已收藏单个目标。 */
    boolean isFavorited(long userId, int targetType, long targetId);

    /**
     * 收藏（幂等）：已收藏时静默返回 false，不抛异常。
     *
     * @return 本次是否新增了收藏记录
     */
    boolean favorite(long userId, int targetType, long targetId);

    /**
     * 取消收藏（幂等）：未收藏时静默返回 false。
     *
     * @return 本次是否删除了收藏记录
     */
    boolean unfavorite(long userId, int targetType, long targetId);

    /**
     * 某用户某类目标的收藏列表（游标分页，按收藏 ID 倒序，即最近收藏在前）。
     */
    CursorResult<Favorite> page(long userId, int targetType, long cursor, int size);

}
