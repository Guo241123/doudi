package com.shequ.common.like;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 通用点赞服务实现。
 * <p>
 * 幂等靠唯一键 uk_user_target 兜底：并发重复点赞会撞唯一键，捕获后静默返回 false。
 * （数据库设计 §10 的写流程原设计先用 Redis SET 预判，这里省略——用户量级下唯一键已足够，
 * 少一处缓存与 DB 的一致性问题。）
 */
@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

    private final LikeRecordMapper likeRecordMapper;

    @Override
    public Set<Long> likedTargetIds(long userId, int targetType, Collection<Long> targetIds) {
        if (targetIds == null || targetIds.isEmpty()) {
            return Set.of();
        }
        List<LikeRecord> rows = likeRecordMapper.selectList(Wrappers.<LikeRecord>lambdaQuery()
                .select(LikeRecord::getTargetId)
                .eq(LikeRecord::getUserId, userId)
                .eq(LikeRecord::getTargetType, targetType)
                .in(LikeRecord::getTargetId, targetIds));
        Set<Long> ids = new HashSet<>(rows.size());
        for (LikeRecord row : rows) {
            ids.add(row.getTargetId());
        }
        return ids;
    }

    @Override
    public boolean isLiked(long userId, int targetType, long targetId) {
        Long count = likeRecordMapper.selectCount(Wrappers.<LikeRecord>lambdaQuery()
                .eq(LikeRecord::getUserId, userId)
                .eq(LikeRecord::getTargetType, targetType)
                .eq(LikeRecord::getTargetId, targetId));
        return count != null && count > 0;
    }

    @Override
    public boolean like(long userId, int targetType, long targetId) {
        LikeRecord record = new LikeRecord();
        record.setUserId(userId);
        record.setTargetType(targetType);
        record.setTargetId(targetId);
        record.setCreateTime(LocalDateTime.now());
        try {
            likeRecordMapper.insert(record);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    @Override
    public boolean unlike(long userId, int targetType, long targetId) {
        int rows = likeRecordMapper.delete(Wrappers.<LikeRecord>lambdaQuery()
                .eq(LikeRecord::getUserId, userId)
                .eq(LikeRecord::getTargetType, targetType)
                .eq(LikeRecord::getTargetId, targetId));
        return rows > 0;
    }

}
