package com.shequ.module.upload.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

/**
 * #26 直传签名返回（接口文档 2.5）。
 * <p>
 * 字段名必须与前端 {@code api/upload.uts} 读取的 key 完全一致：
 * {@code uploadUrl} / {@code fileKey} / {@code headers}。
 */
@Data
@AllArgsConstructor
public class UploadTokenVO {

    /** 直传目标地址：前端对它发 PUT，body 为文件二进制 */
    private String uploadUrl;

    /** 上传成功后拿到的文件标识，发布/改头像时把这个值提交给业务接口 */
    private String fileKey;

    /** 直传时需要携带的请求头 */
    private Map<String, String> headers;
}
