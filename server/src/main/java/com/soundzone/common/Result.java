package com.soundzone.common;

import lombok.Getter;

/**
 * 统一响应体：所有 REST 接口的返回结构
 * code = 0 表示成功，其余为错误码（见 ResultCode）
 */
@Getter
public class Result<T> {

    private final int code;
    private final String msg;
    private final T data;

    private Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), "ok", data);
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(ResultCode rc, String msg) {
        return new Result<>(rc.getCode(), msg, null);
    }
}
