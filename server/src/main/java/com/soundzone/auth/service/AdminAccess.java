package com.soundzone.auth.service;

import com.soundzone.common.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** 运营接口独立凭证；未配置时关闭，不复用普通用户身份。 */
@Component
public class AdminAccess {
    @Value("${soundzone.admin-key:}")
    private String key;

    public void check(String candidate) {
        if (key.isBlank()
                || candidate == null
                || !MessageDigest.isEqual(
                        key.getBytes(StandardCharsets.UTF_8),
                        candidate.getBytes(StandardCharsets.UTF_8)))
            throw new BizException(ResultCode.FORBIDDEN);
    }
}
