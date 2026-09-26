package com.soundzone.common;

import lombok.extern.slf4j.Slf4j;

import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理：把各类异常统一转换为 Result 响应体 - BizException → 业务错误码 - 参数校验失败（@Validated） → 1001 + 字段级错误信息 -
 * 其他未捕获异常 → 5000（记录日志，不暴露内部细节）
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public org.springframework.http.ResponseEntity<Result<Object>> handleBiz(BizException e) {
        int status =
                switch (e.getResultCode()) {
                    case UNAUTHENTICATED -> 401;
                    case FORBIDDEN, NOT_RESOURCE_OWNER -> 403;
                    case ZONE_NOT_FOUND,
                                    TRACK_NOT_FOUND,
                                    USER_NOT_FOUND,
                                    QUEUE_ITEM_NOT_FOUND,
                                    MOMENT_NOT_FOUND ->
                            404;
                    default -> 400;
                };
        return org.springframework.http.ResponseEntity.status(status)
                .body(Result.fail(e.getResultCode(), e.getMessage(), e.getData()));
    }

    /**
     * @RequestBody 对象校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleInvalid(MethodArgumentNotValidException e) {
        return Result.fail(ResultCode.PARAM_INVALID, formatFieldErrors(e));
    }

    /** 表单/路径参数绑定校验失败 */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBind(BindException e) {
        return Result.fail(ResultCode.PARAM_INVALID, formatFieldErrors(e));
    }

    @ExceptionHandler({
        IllegalArgumentException.class,
        org.springframework.web.bind.MissingServletRequestParameterException.class,
        org.springframework.http.converter.HttpMessageNotReadableException.class,
        org.springframework.web.multipart.MaxUploadSizeExceededException.class
    })
    public Result<Void> handleInput(Exception e) {
        return Result.fail(ResultCode.PARAM_INVALID, "参数无效或图片超过大小限制");
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public org.springframework.http.ResponseEntity<Result<Void>> handleMissingResource(
            org.springframework.web.servlet.resource.NoResourceFoundException e) {
        return org.springframework.http.ResponseEntity.status(404)
                .body(Result.fail(ResultCode.ENDPOINT_NOT_FOUND, ResultCode.ENDPOINT_NOT_FOUND.getMsg()));
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
