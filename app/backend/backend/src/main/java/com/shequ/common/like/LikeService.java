package com.shequ.common.like;

import java.util.Collection;
import java.util.Set;

/**
 * 通用点赞服务：跨模块点赞帖子 / 评论时只调本接口，禁止各模块直查 t_like_record。
 * <p>
 * 计数由调用方自行维护（UPDATE 目标表 like_count）：本服务只管点赞关系记录的幂等增删，
 * 返回"本次是否真的新增/删除了记录"，调用方据此决定是否加减计数。
 */
public interface LikeService {

    /**
     * 批量查询某用户对一组目标的点赞情况，用于列表页一次性回填 liked，避免 N+1。
     *
     * @return 命中的目标 ID 集合（未点赞的不在其中）
     */
    Set<Long> likedTargetIds(long userId, int targetType, Collection<Long> targetIds);

    /** 是否已点赞单个目标。 */
    boolean isLiked(long userId, int targetType, long targetId);

    /**
     * 点赞（幂等）：已点赞时静默返回 false，不抛异常（唯一键兜底并发重复）。
     *
     * @return 本次是否新增了点赞记录
     */
    boolean like(long userId, int targetType, long targetId);

    /**
     * 取消点赞（幂等）：未点赞时静默返回 false。
     *
     * @return 本次是否删除了点赞记录
     */
    boolean unlike(long userId, int targetType, long targetId);

}
