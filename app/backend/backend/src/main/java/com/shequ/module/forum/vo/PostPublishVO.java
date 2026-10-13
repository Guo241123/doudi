package com.shequ.module.forum.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 发帖返回（接口文档 2.6 #28）：只回新帖 ID。
 */
@Data
@AllArgsConstructor
public class PostPublishVO {

    private Long id;

}
