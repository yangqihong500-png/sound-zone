package com.soundzone.auth.service;

/** 宿主适配契约：必须由服务端校验一次性登录凭证，不能信任客户端自报 subject。 */
public interface HostIdentityProvider {
    Identity verify(String code);

    record Identity(String subject, String displayName) {}
}
