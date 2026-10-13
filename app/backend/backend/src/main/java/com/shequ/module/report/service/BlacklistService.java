package com.shequ.module.report.service;

import com.shequ.common.result.CursorResult;
import com.shequ.module.report.vo.BlacklistItemVO;

import java.util.Set;

/**
 * 拉黑服务（接口文档 #43/#44；实现注记 3 三口过滤的数据源）。
 */
public interface BlacklistService {

    /**
     * 拉黑：不能拉黑自己与不存在的用户；重复拉黑幂等成功。
     */
    void block(long userId, long targetUserId);

    /**
     * 解除拉黑：未拉黑过也返回成功（幂等）。
     */
    void unblock(long userId, long targetUserId);

    /**
     * #44 我的拉黑列表，按记录 id 游标倒序。
     */
    CursorResult<BlacklistItemVO> list(long userId, int cursor, int size);

    /**
     * 我拉黑的人的 ID 集合。
     */
    Set<Long> blockedByMe(long userId);

    /**
     * 我被谁拉黑的 ID 集合（"双方内容互不可见"的另一半）。
     */
    Set<Long> blockedMe(long userId);

    /**
     * 双向黑名单并集：我拉黑的 + 拉黑我的。内容互不可见场景统一用它。
     */
    Set<Long> hiddenUserIds(long userId);

    /**
     * 两人之间是否存在任一方向的拉黑（发私信/评论前校验）。
     */
    boolean blockedBetween(long userIdA, long userIdB);

}
