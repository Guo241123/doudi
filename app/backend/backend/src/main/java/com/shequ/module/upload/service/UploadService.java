package com.shequ.module.upload.service;

import com.shequ.module.upload.dto.UploadTokenDTO;
import com.shequ.module.upload.vo.UploadTokenVO;

/**
 * 媒体直传服务（接口文档 #26）。
 */
public interface UploadService {

    /** 换取直传凭据：返回上传地址、fileKey 与所需请求头。 */
    UploadTokenVO createToken(UploadTokenDTO dto);

    /** 接收直传文件字节（provider=local 时落到本地磁盘）。 */
    void receive(String fileKey, long expireAt, String signature, byte[] content);
}
