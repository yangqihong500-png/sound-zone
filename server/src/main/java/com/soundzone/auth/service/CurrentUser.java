package com.soundzone.auth.service;

import com.soundzone.common.*;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/** 控制器唯一身份来源；兼容请求中的 userId 不参与授权。 */
@Component
@RequestScope
@RequiredArgsConstructor
public class CurrentUser {
    private final HttpServletRequest request;
    private final SessionService sessions;
    private Long userId;

    public Long id() {
        if (userId != null) return userId;
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer "))
            throw new BizException(ResultCode.UNAUTHENTICATED);
        userId = sessions.authenticate(header.substring(7));
        return userId;
    }
}
