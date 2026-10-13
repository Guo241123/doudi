package com.shequ.module.report.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 拉黑列表项（接口文档 #44）。
 */
@Data
@AllArgsConstructor
public class BlacklistItemVO {

    private Long userId;

    private String nickname;

    private String avatarUrl;

    private LocalDateTime blackTime;

}
