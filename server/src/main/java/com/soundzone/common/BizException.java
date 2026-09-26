package com.soundzone.common;

import lombok.Getter;

/** 业务异常：服务层抛出，由 GlobalExceptionHandler 统一转换为 Result */
@Getter
public class BizException extends RuntimeException {

    private final ResultCode resultCode;
    private Object data;

    public BizException(ResultCode code, String detail, Object data) {
        this(code, detail);
        this.data = data;
    }

    public BizException(ResultCode resultCode) {
        super(resultCode.getMsg());
        this.resultCode = resultCode;
    }

    public BizException(ResultCode resultCode, String detail) {
        super(resultCode.getMsg() + "：" + detail);
        this.resultCode = resultCode;
    }
}
