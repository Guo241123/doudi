package com.shequ.module.trade.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shequ.common.exception.BusinessException;
import com.shequ.common.favorite.Favorite;
import com.shequ.common.favorite.FavoriteService;
import com.shequ.common.media.Media;
import com.shequ.common.media.MediaService;
import com.shequ.common.result.CursorResult;
import com.shequ.common.result.ErrorCode;
import com.shequ.common.storage.FileUrlResolver;
import com.shequ.module.trade.dto.ItemPublishDTO;
import com.shequ.module.trade.entity.ItemStatus;
import com.shequ.module.trade.entity.TradeCategory;
import com.shequ.module.trade.entity.TradeItem;
import com.shequ.module.trade.mapper.TradeCategoryMapper;
import com.shequ.module.trade.mapper.TradeItemMapper;
import com.shequ.module.trade.service.TradeService;
import com.shequ.module.trade.vo.CategoryVO;
import com.shequ.module.trade.vo.FavoriteItemVO;
import com.shequ.module.trade.vo.ItemAuditVO;
import com.shequ.module.trade.vo.ItemCardVO;
import com.shequ.module.trade.vo.ItemDetailVO;
import com.shequ.module.trade.vo.MyItemVO;
import com.shequ.module.trade.vo.SellerVO;
import com.shequ.module.user.entity.User;
import com.shequ.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 二手交易服务实现（接口文档 2.3 #17～#25）。
 * <p>
 * 分页口径与任务模块一致：{@code WHERE id < ? ORDER BY id DESC LIMIT size+1}。
 * 列表页封面 / 收藏态批量装配，详情页单独查卖家，避免 N+1。
 */
@Service
@RequiredArgsConstructor
public class TradeServiceImpl implements TradeService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    /** 公开列表 / 详情可见的状态：在售、已售（其余状态仅本人可见） */
    private static final List<Integer> PUBLIC_STATUSES =
            List.of(ItemStatus.ON_SALE.code(), ItemStatus.SOLD.code());

    private final TradeItemMapper tradeItemMapper;
    private final TradeCategoryMapper tradeCategoryMapper;
    private final MediaService mediaService;
    private final FavoriteService favoriteService;
    private final UserService userService;
    private final FileUrlResolver fileUrlResolver;

    /** 发布 / 编辑后是否直接上架；关闭则回待审核（先审后发，需求 4.7.2） */
    @Value("${trade.auto-approve:true}")
    private boolean autoApprove;

    /** 自动下架天数：expire_at = 发布时间 + N 天 */
    @Value("${trade.expire-days:30}")
    private int expireDays;

    /** 详情页免责声明文案（★ 后端下发，需求 4.2.7） */
    @Value("${trade.disclaimer:平台仅提供信息撮合服务，交易风险由双方自行承担，请当面验货、谨慎汇款}")
    private String disclaimer;

    // ------------------------------------------------------------------ #17 分类列表

    @Override
    public List<CategoryVO> categories() {
        List<TradeCategory> categories = tradeCategoryMapper.selectList(Wrappers.<TradeCategory>lambdaQuery()
                .orderByAsc(TradeCategory::getSort)
                .orderByAsc(TradeCategory::getId));
        List<CategoryVO> list = new ArrayList<>(categories.size());
        for (TradeCategory category : categories) {
            CategoryVO vo = new CategoryVO();
            vo.setId(category.getId());
            vo.setName(category.getName());
            vo.setSort(category.getSort());
            vo.setIconUrl(resolveUrl(category.getIconKey()));
            list.add(vo);
        }
        return list;
    }

    // ------------------------------------------------------------------ #18 物品列表

    @Override
    public CursorResult<ItemCardVO> listItems(Long categoryId, int cursor, int size, Long viewerId) {
        int pageSize = normalizeSize(size);
        List<TradeItem> items = tradeItemMapper.selectList(Wrappers.<TradeItem>lambdaQuery()
                .in(TradeItem::getStatus, PUBLIC_STATUSES)
                .eq(categoryId != null, TradeItem::getCategoryId, categoryId)
                .lt(cursor > 0, TradeItem::getId, cursor)
                .orderByDesc(TradeItem::getId)
                .last("LIMIT " + (pageSize + 1)));

        boolean hasMore = items.size() > pageSize;
        List<TradeItem> page = hasMore ? new ArrayList<>(items.subList(0, pageSize)) : items;

        Map<Long, String> covers = coverUrls(page);
        Set<Long> favoritedIds = favoritedIds(viewerId, page);

        List<ItemCardVO> list = new ArrayList<>(page.size());
        for (TradeItem item : page) {
            ItemCardVO vo = new ItemCardVO();
            fillCard(vo, item, covers.get(item.getId()), favoritedIds.contains(item.getId()));
            list.add(vo);
        }
        return new CursorResult<>(list, nextCursor(page), hasMore);
    }

    // ------------------------------------------------------------------ #19 物品详情

    @Override
    public ItemDetailVO detail(long id, Long viewerId) {
        TradeItem item = tradeItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "物品不存在或已删除");
        }
        boolean isMine = viewerId != null && Objects.equals(item.getUserId(), viewerId);
        if (!isMine && !PUBLIC_STATUSES.contains(item.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "物品不存在或已下架");
        }

        List<Media> medias = mediaService.listByBiz(Media.BIZ_ITEM, List.of(id));
        List<String> images = new ArrayList<>();
        String videoUrl = null;
        for (Media media : medias) {
            if (Objects.equals(media.getMediaType(), Media.TYPE_VIDEO)) {
                if (videoUrl == null) {
                    videoUrl = resolveUrl(media.getFileKey());
                }
            } else {
                images.add(resolveUrl(media.getFileKey()));
            }
        }

        ItemDetailVO vo = new ItemDetailVO();
        String coverUrl = !images.isEmpty() ? images.get(0) : videoUrl;
        boolean favorited = viewerId != null
                && favoriteService.isFavorited(viewerId, Favorite.TARGET_ITEM, id);
        fillCard(vo, item, coverUrl, favorited);
        vo.setImages(images);
        vo.setVideoUrl(videoUrl);
        vo.setDescription(item.getDescription());
        vo.setPublishTime(item.getCreateTime());
        vo.setSeller(sellerOf(item.getUserId()));
        vo.setCanChat(viewerId != null && !isMine && Objects.equals(item.getStatus(), ItemStatus.ON_SALE.code()));
        vo.setDisclaimer(disclaimer);
        vo.setIsMine(isMine);
        return vo;
    }

    // ------------------------------------------------------------------ #20 发布

    @Override
    @Transactional
    public ItemAuditVO publish(long userId, ItemPublishDTO dto) {
        List<String> imageKeys = imageKeys(dto);
        validateMedia(imageKeys, dto.getVideoKey());
        requireCategory(dto.getCategoryId());

        LocalDateTime now = LocalDateTime.now();
        int status = submitStatus();
        TradeItem item = new TradeItem();
        item.setUserId(userId);
        item.setCategoryId(dto.getCategoryId());
        item.setTitle(dto.getTitle());
        item.setDescription(dto.getDescription());
        item.setPriceFen(dto.getPriceFen());
        item.setBargainable(toFlag(dto.getBargainable()));
        item.setContact(dto.getContact());
        item.setStatus(status);
        item.setAuditTime(autoApprove ? now : null);
        item.setExpireAt(now.plusDays(expireDays));
        item.setCreateTime(now);
        item.setUpdateTime(now);
        tradeItemMapper.insert(item);

        mediaService.saveAll(buildMedias(item.getId(), imageKeys, dto.getVideoKey(), now));

        return new ItemAuditVO(item.getId(), ItemStatus.labelOf(status));
    }

    // ------------------------------------------------------------------ #21 编辑

    @Override
    @Transactional
    public ItemAuditVO edit(long userId, long id, ItemPublishDTO dto) {
        List<String> imageKeys = imageKeys(dto);
        validateMedia(imageKeys, dto.getVideoKey());
        requireCategory(dto.getCategoryId());

        TradeItem item = requireOwned(userId, id);
        int current = item.getStatus();
        if (current != ItemStatus.UNDER_REVIEW.code() && current != ItemStatus.ON_SALE.code()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "当前状态不可编辑");
        }

        LocalDateTime now = LocalDateTime.now();
        int status = submitStatus();
        item.setCategoryId(dto.getCategoryId());
        item.setTitle(dto.getTitle());
        item.setDescription(dto.getDescription());
        item.setPriceFen(dto.getPriceFen());
        item.setBargainable(toFlag(dto.getBargainable()));
        item.setContact(dto.getContact());
        item.setStatus(status);
        item.setRejectReason(null);
        item.setAuditTime(autoApprove ? now : null);
        item.setUpdateTime(now);
        tradeItemMapper.updateById(item);

        // 编辑后媒体整体替换：旧行软删留痕（便于 OSS 清理与机审追溯），再插新行
        mediaService.deleteByBiz(Media.BIZ_ITEM, id);
        mediaService.saveAll(buildMedias(id, imageKeys, dto.getVideoKey(), now));

        return new ItemAuditVO(id, ItemStatus.labelOf(status));
    }

    // ------------------------------------------------------------------ #22 卖家状态操作

    @Override
    @Transactional
    public void changeStatus(long userId, long id, String action) {
        TradeItem item = requireOwned(userId, id);
        int current = item.getStatus();
        LocalDateTime now = LocalDateTime.now();

        switch (action) {
            case "OFF_SHELF" -> {
                if (current != ItemStatus.ON_SALE.code() && current != ItemStatus.UNDER_REVIEW.code()) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "当前状态不可下架");
                }
                item.setStatus(ItemStatus.OFF_SHELF.code());
            }
            case "MARK_SOLD" -> {
                if (current != ItemStatus.ON_SALE.code()) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "仅在售物品可标记已售");
                }
                item.setStatus(ItemStatus.SOLD.code());
            }
            case "PUT_ON" -> {
                if (current != ItemStatus.OFF_SHELF.code()) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "仅已下架物品可重新上架");
                }
                // 重新上架 = 重新提交审核；auto-approve 开启时直接上架
                int status = submitStatus();
                item.setStatus(status);
                item.setAuditTime(autoApprove ? now : null);
                // 重新上架刷新有效期，避免超期下架后立即再次超期
                item.setExpireAt(now.plusDays(expireDays));
            }
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的 action");
        }

        item.setUpdateTime(now);
        tradeItemMapper.updateById(item);
    }

    // ------------------------------------------------------------------ #23 我的发布

    @Override
    public CursorResult<MyItemVO> myItems(long userId, String auditStatus, int cursor, int size) {
        Integer statusCode = null;
        if (StringUtils.hasText(auditStatus)) {
            statusCode = ItemStatus.codeOf(auditStatus);
            if (statusCode == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "auditStatus 取值非法");
            }
        }

        int pageSize = normalizeSize(size);
        List<TradeItem> items = tradeItemMapper.selectList(Wrappers.<TradeItem>lambdaQuery()
                .eq(TradeItem::getUserId, userId)
                .eq(statusCode != null, TradeItem::getStatus, statusCode)
                .lt(cursor > 0, TradeItem::getId, cursor)
                .orderByDesc(TradeItem::getId)
                .last("LIMIT " + (pageSize + 1)));

        boolean hasMore = items.size() > pageSize;
        List<TradeItem> page = hasMore ? new ArrayList<>(items.subList(0, pageSize)) : items;

        Map<Long, String> covers = coverUrls(page);
        Set<Long> favoritedIds = favoritedIds(userId, page);

        List<MyItemVO> list = new ArrayList<>(page.size());
        for (TradeItem item : page) {
            MyItemVO vo = new MyItemVO();
            fillCard(vo, item, covers.get(item.getId()), favoritedIds.contains(item.getId()));
            vo.setAuditStatus(ItemStatus.labelOf(item.getStatus()));
            vo.setRejectReason(item.getRejectReason());
            list.add(vo);
        }
        return new CursorResult<>(list, nextCursor(page), hasMore);
    }

    // ------------------------------------------------------------------ #24 收藏 / 取消收藏

    @Override
    public void favorite(long userId, long itemId) {
        TradeItem item = tradeItemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "物品不存在或已删除");
        }
        favoriteService.favorite(userId, Favorite.TARGET_ITEM, itemId);
    }

    @Override
    public void unfavorite(long userId, long itemId) {
        favoriteService.unfavorite(userId, Favorite.TARGET_ITEM, itemId);
    }

    // ------------------------------------------------------------------ #25 我的收藏

    @Override
    public CursorResult<FavoriteItemVO> myFavorites(long userId, int cursor, int size) {
        CursorResult<Favorite> favoritePage =
                favoriteService.page(userId, Favorite.TARGET_ITEM, cursor, size);
        List<Long> itemIds = favoritePage.getList().stream().map(Favorite::getTargetId).toList();
        if (itemIds.isEmpty()) {
            return new CursorResult<>(List.of(), favoritePage.getNextCursor(), favoritePage.isHasMore());
        }

        Map<Long, TradeItem> itemMap = new HashMap<>();
        for (TradeItem item : tradeItemMapper.selectBatchIds(itemIds)) {
            itemMap.put(item.getId(), item);
        }
        Map<Long, String> covers = coverUrls(new ArrayList<>(itemMap.values()));

        List<FavoriteItemVO> list = new ArrayList<>(itemIds.size());
        for (Long itemId : itemIds) {
            TradeItem item = itemMap.get(itemId);
            if (item == null) {
                // 物品已被彻底删除（软删的也会被 MP 过滤），收藏记录无法展示，跳过
                continue;
            }
            FavoriteItemVO vo = new FavoriteItemVO();
            fillCard(vo, item, covers.get(itemId), true);
            vo.setUnavailable(!Objects.equals(item.getStatus(), ItemStatus.ON_SALE.code()));
            list.add(vo);
        }
        return new CursorResult<>(list, favoritePage.getNextCursor(), favoritePage.isHasMore());
    }

    // ------------------------------------------------------------------ 私有方法

    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    /** 提交后的落库状态：auto-approve 开启直接上架，否则待审核。 */
    private int submitStatus() {
        return autoApprove ? ItemStatus.ON_SALE.code() : ItemStatus.UNDER_REVIEW.code();
    }

    private int toFlag(Boolean value) {
        return Boolean.TRUE.equals(value) ? 1 : 0;
    }

    private long nextCursor(List<? extends TradeItem> page) {
        return page.isEmpty() ? 0 : page.get(page.size() - 1).getId();
    }

    /** 图片 fileKey 列表：过滤空串，最多 9 张（超过由 @Size 拦截）。 */
    private List<String> imageKeys(ItemPublishDTO dto) {
        List<String> keys = new ArrayList<>();
        if (dto.getImages() != null) {
            for (String key : dto.getImages()) {
                if (StringUtils.hasText(key)) {
                    keys.add(key);
                }
            }
        }
        return keys;
    }

    /** images 与 videoKey 互斥、至少其一（annotation 表达不了，放这里）。 */
    private void validateMedia(List<String> imageKeys, String videoKey) {
        boolean hasVideo = StringUtils.hasText(videoKey);
        boolean hasImages = !imageKeys.isEmpty();
        if (!hasImages && !hasVideo) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "图片与视频至少上传其一");
        }
        if (hasImages && hasVideo) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "图片与视频不能同时上传");
        }
        if (imageKeys.size() > 9) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "图片最多 9 张");
        }
    }

    private void requireCategory(Long categoryId) {
        if (categoryId == null || tradeCategoryMapper.selectById(categoryId) == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "分类不存在");
        }
    }

    /** 校验物品存在且属于当前用户，否则 40401 / 40301。 */
    private TradeItem requireOwned(long userId, long id) {
        TradeItem item = tradeItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "物品不存在或已删除");
        }
        if (!Objects.equals(item.getUserId(), userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅可操作自己发布的物品");
        }
        return item;
    }

    /** 组装媒体行：有视频则只写 1 条视频，否则按顺序写图片。 */
    private List<Media> buildMedias(long itemId, List<String> imageKeys, String videoKey, LocalDateTime now) {
        List<Media> medias = new ArrayList<>();
        int sort = 0;
        if (StringUtils.hasText(videoKey)) {
            medias.add(newMedia(itemId, Media.TYPE_VIDEO, videoKey, sort, now));
        } else {
            for (String key : imageKeys) {
                medias.add(newMedia(itemId, Media.TYPE_IMAGE, key, sort++, now));
            }
        }
        return medias;
    }

    private Media newMedia(long itemId, int mediaType, String fileKey, int sort, LocalDateTime now) {
        Media media = new Media();
        media.setBizType(Media.BIZ_ITEM);
        media.setBizId(itemId);
        media.setMediaType(mediaType);
        media.setFileKey(fileKey);
        media.setSort(sort);
        // 内容安全服务尚未接入，先置机审通过，避免详情页因机审状态被拦
        media.setAuditStatus(1);
        media.setCreateTime(now);
        media.setUpdateTime(now);
        return media;
    }

    /** 批量取封面：listByBiz 已按 sort 升序，首个命中即封面（图片或视频首帧）。 */
    private Map<Long, String> coverUrls(List<TradeItem> items) {
        Map<Long, String> covers = new HashMap<>();
        if (items.isEmpty()) {
            return covers;
        }
        List<Long> ids = items.stream().map(TradeItem::getId).toList();
        for (Media media : mediaService.listByBiz(Media.BIZ_ITEM, ids)) {
            covers.putIfAbsent(media.getBizId(), resolveUrl(media.getFileKey()));
        }
        return covers;
    }

    /** 批量取收藏态；游客返回空集合（favorited 恒 false）。 */
    private Set<Long> favoritedIds(Long viewerId, List<TradeItem> items) {
        if (viewerId == null || items.isEmpty()) {
            return Set.of();
        }
        Collection<Long> ids = items.stream().map(TradeItem::getId).toList();
        return favoriteService.favoritedTargetIds(viewerId, Favorite.TARGET_ITEM, ids);
    }

    private void fillCard(ItemCardVO vo, TradeItem item, String coverUrl, boolean favorited) {
        vo.setId(item.getId());
        vo.setTitle(item.getTitle());
        vo.setCoverUrl(coverUrl);
        vo.setPriceFen(item.getPriceFen());
        vo.setBargainable(Objects.equals(item.getBargainable(), 1));
        vo.setStatus(ItemStatus.labelOf(item.getStatus()));
        vo.setFavorited(favorited);
    }

    private SellerVO sellerOf(Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            return null;
        }
        SellerVO seller = new SellerVO();
        seller.setUserId(user.getId());
        seller.setNickname(user.getNickname());
        seller.setAvatar(user.getAvatarUrl());
        return seller;
    }

    /** fileKey → 可访问 URL，统一由 {@link FileUrlResolver} 决定（本地存储 → /files/**；OSS → endpoint）。 */
    private String resolveUrl(String fileKey) {
        return fileUrlResolver.resolve(fileKey);
    }

}
