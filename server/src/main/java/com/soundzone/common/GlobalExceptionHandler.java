package com.soundzone.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理：把各类异常统一转换为 Result 响应体
 * - BizException                → 业务错误码
 * - 参数校验失败（@Validated）   → 1001 + 字段级错误信息
 * - 其他未捕获异常               → 5000（记录日志，不暴露内部细节）
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        return Result.fail(e.getResultCode(), e.getMessage());
    }

    /** @RequestBody 对象校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleInvalid(MethodArgumentNotValidException e) {
        return Result.fail(ResultCode.PARAM_INVALID, formatFieldErrors(e));
    }

    /** 表单/路径参数绑定校验失败 */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBind(BindException e) {
        return Result.fail(ResultCode.PARAM_INVALID, formatFieldErrors(e));
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnknown(Exception e) {
        log.error("未捕获异常", e);
        return Result.fail(ResultCode.SYSTEM_ERROR, ResultCode.SYSTEM_ERROR.getMsg());
    }

    private String formatFieldErrors(BindException e) {
        return e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
    }
}
