package com.shequ.module.trade.service;

import com.shequ.common.result.CursorResult;
import com.shequ.module.trade.dto.ItemPublishDTO;
import com.shequ.module.trade.vo.CategoryVO;
import com.shequ.module.trade.vo.FavoriteItemVO;
import com.shequ.module.trade.vo.ItemAuditVO;
import com.shequ.module.trade.vo.ItemCardVO;
import com.shequ.module.trade.vo.ItemDetailVO;
import com.shequ.module.trade.vo.MyItemVO;

import java.util.List;

/**
 * 二手交易服务（接口文档 2.3 #17～#25，App 端）。
 * <p>
 * 后台交易管理 / 分类管理（#50 / #51）依赖未落地的 admin 模块，暂不在本服务内。
 */
public interface TradeService {

    /** #17 分类列表，按 sort 升序（交易页 2×4 八宫格）。 */
    List<CategoryVO> categories();

    /**
     * #18 物品列表：只返回在售 / 已售，可按分类筛选；favorited 依赖登录态（游客恒 false）。
     */
    CursorResult<ItemCardVO> listItems(Long categoryId, int cursor, int size, Long viewerId);

    /**
     * #19 物品详情：在售 / 已售对游客可见，其余状态仅发布者本人可见（否则 40401）。
     */
    ItemDetailVO detail(long id, Long viewerId);

    /** #20 发布二手（先审后发，auto-approve 开启时直接上架）。 */
    ItemAuditVO publish(long userId, ItemPublishDTO dto);

    /** #21 编辑二手（仅本人；待审核 / 在售可编辑，编辑后重新审核）。 */
    ItemAuditVO edit(long userId, long id, ItemPublishDTO dto);

    /** #22 卖家状态操作（仅本人）：OFF_SHELF / MARK_SOLD / PUT_ON。 */
    void changeStatus(long userId, long id, String action);

    /** #23 我的发布（仅本人），可按 auditStatus 筛选。 */
    CursorResult<MyItemVO> myItems(long userId, String auditStatus, int cursor, int size);

    /** #24 收藏（幂等）。 */
    void favorite(long userId, long itemId);

    /** #24 取消收藏（幂等）。 */
    void unfavorite(long userId, long itemId);

    /** #25 我的收藏（仅本人），已售 / 已下架仍返回并标 unavailable。 */
    CursorResult<FavoriteItemVO> myFavorites(long userId, int cursor, int size);

}
