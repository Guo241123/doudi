package com.shequ.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 游标分页返回体（接口文档 1.4）：App 端列表统一使用。
 * <p>
 * 查询实现为 {@code WHERE id < cursor ORDER BY id DESC LIMIT size}，
 * nextCursor 取本页最后一条 ID，首页请求传 0；无数据时为 0。
 */
@Data
@AllArgsConstructor
public class CursorResult<T> {

    /** 本页数据 */
    private List<T> list;

    /** 下一页游标 */
    private long nextCursor;

    /** 是否还有下一页 */
    private boolean hasMore;

}
