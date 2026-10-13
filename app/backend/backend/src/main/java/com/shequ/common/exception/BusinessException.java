package com.shequ.common.exception;

import com.shequ.common.result.ErrorCode;
import lombok.Getter;

/**
 * 业务异常：由全局异常处理器统一转换为 Result（对应错误码）。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

}