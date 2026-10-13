package com.shequ.module.trade.controller;

import com.shequ.common.auth.AuthInterceptor;
import com.shequ.common.result.CursorResult;
import com.shequ.common.result.Result;
import com.shequ.module.trade.dto.ItemPublishDTO;
import com.shequ.module.trade.dto.ItemStatusActionDTO;
import com.shequ.module.trade.service.TradeService;
import com.shequ.module.trade.vo.CategoryVO;
import com.shequ.module.trade.vo.FavoriteItemVO;
import com.shequ.module.trade.vo.ItemAuditVO;
import com.shequ.module.trade.vo.ItemCardVO;
import com.shequ.module.trade.vo.ItemDetailVO;
import com.shequ.module.trade.vo.MyItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 二手交易接口（接口文档 2.3 #17～#25）。
 * <p>
 * #17～#19 游客可读（GET，带 token 则回填收藏态 / 本人标识）；
 * #20～#25 需登录，身份取自登录态，不存在 userId 入参。
 * 后台交易管理 / 分类管理（#50 / #51）待 admin 模块落地后实现。
 */
@Tag(name = "二手交易")
@RestController
@RequestMapping("/api/v1/trade")
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;

    @Operation(summary = "#17 分类列表")
    @GetMapping("/categories")
    public Result<List<CategoryVO>> categories() {
        return Result.ok(tradeService.categories());
    }

    @Operation(summary = "#18 物品列表")
    @GetMapping("/items")
    public Result<CursorResult<ItemCardVO>> items(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int cursor,
            @RequestParam(defaultValue = "20") int size,
            @RequestAttribute(value = AuthInterceptor.ATTR_USER_ID, required = false) Long userId) {
        return Result.ok(tradeService.listItems(categoryId, cursor, size, userId));
    }

    @Operation(summary = "#19 物品详情")
    @GetMapping("/items/{id}")
    public Result<ItemDetailVO> detail(
            @PathVariable long id,
            @RequestAttribute(value = AuthInterceptor.ATTR_USER_ID, required = false) Long userId) {
        return Result.ok(tradeService.detail(id, userId));
    }

    @Operation(summary = "#20 发布二手")
    @PostMapping("/items")
    public Result<ItemAuditVO> publish(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) long userId,
                                       @Valid @RequestBody ItemPublishDTO dto) {
        return Result.ok(tradeService.publish(userId, dto));
    }

    @Operation(summary = "#21 编辑二手")
    @PutMapping("/items/{id}")
    public Result<ItemAuditVO> edit(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) long userId,
                                    @PathVariable long id,
                                    @Valid @RequestBody ItemPublishDTO dto) {
        return Result.ok(tradeService.edit(userId, id, dto));
    }

    @Operation(summary = "#22 卖家状态操作")
    @PutMapping("/items/{id}/status")
    public Result<Void> changeStatus(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) long userId,
                                     @PathVariable long id,
                                     @Valid @RequestBody ItemStatusActionDTO dto) {
        tradeService.changeStatus(userId, id, dto.getAction());
        return Result.ok();
    }

    @Operation(summary = "#23 我的发布")
    @GetMapping("/my-items")
    public Result<CursorResult<MyItemVO>> myItems(
            @RequestAttribute(AuthInterceptor.ATTR_USER_ID) long userId,
            @RequestParam(required = false) String auditStatus,
            @RequestParam(defaultValue = "0") int cursor,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(tradeService.myItems(userId, auditStatus, cursor, size));
    }

    @Operation(summary = "#24 收藏")
    @PostMapping("/items/{id}/favorite")
    public Result<Void> favorite(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) long userId,
                                 @PathVariable long id) {
        tradeService.favorite(userId, id);
        return Result.ok();
    }

    @Operation(summary = "#24 取消收藏")
    @DeleteMapping("/items/{id}/favorite")
    public Result<Void> unfavorite(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) long userId,
                                   @PathVariable long id) {
        tradeService.unfavorite(userId, id);
        return Result.ok();
    }

    @Operation(summary = "#25 我的收藏")
    @GetMapping("/favorites")
    public Result<CursorResult<FavoriteItemVO>> myFavorites(
            @RequestAttribute(AuthInterceptor.ATTR_USER_ID) long userId,
            @RequestParam(defaultValue = "0") int cursor,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(tradeService.myFavorites(userId, cursor, size));
    }

}
