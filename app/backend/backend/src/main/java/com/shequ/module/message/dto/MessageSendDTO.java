package com.shequ.module.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发送消息请求体（接口文档 #39：1～500 字）。
 */
@Data
public class MessageSendDTO {

    @NotBlank(message = "消息内容不能为空")
    @Size(min = 1, max = 500, message = "消息内容为 1～500 字")
    private String content;

}
