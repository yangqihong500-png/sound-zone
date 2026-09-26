package com.soundzone.auth.service;

import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** 私密域密码只保存随机盐 PBKDF2 摘要；不擅自增加复杂度或错误次数规则。 */
@Service
public class PasswordService {
    private static final int ITERATIONS = 210000;

    public String hash(String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return ITERATIONS + ":" + encode(salt) + ":" + encode(derive(password, salt, ITERATIONS));
    }

    public boolean matches(String password, String encoded) {
        if (password == null || encoded == null) return false;
        try {
            String[] parts = encoded.split(":");
            int iterations = Integer.parseInt(parts[0]);
            if (parts.length != 3 || iterations < 10000 || iterations > 1000000) return false;
            return MessageDigest.isEqual(
                    Base64.getDecoder().decode(parts[2]),
                    derive(password, Base64.getDecoder().decode(parts[1]), iterations));
        } catch (RuntimeException e) {
            return false;
        }
    }

    private byte[] derive(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("密码摘要生成失败", e);
        } finally {
            spec.clearPassword();
        }
    }

    private String encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
}
