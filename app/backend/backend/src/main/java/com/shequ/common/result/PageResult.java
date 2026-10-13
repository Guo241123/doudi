package com.shequ.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 页码分页返回体：管理后台列表统一使用（接口文档 2.10 后台接口用 page/size 口径）。
 * <p>
 * App 端列表用 {@link CursorResult}（游标分页），后台用本类（页码分页）——两套口径
 * 分别对应文档 1.4 与 2.10，不要混用。
 */
@Data
@AllArgsConstructor
public class PageResult<T> {

    /** 本页数据 */
    private List<T> list;

    /** 总条数 */
    private long total;

    /** 当前页码（从 1 开始） */
    private int page;

    /** 每页大小 */
    private int size;

    /** 是否还有下一页 */
    private boolean hasMore;

}
