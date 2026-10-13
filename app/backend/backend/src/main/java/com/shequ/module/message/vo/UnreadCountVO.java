package com.shequ.module.message.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

/**
 * 未读数（接口文档 #40：只查 Redis 计数器，配合动态轮询）。
 */
@Data
@AllArgsConstructor
public class UnreadCountVO {

    private int total;

    /** 按会话类型分组的未读数 */
    private Map<String, Integer> byType;

}
