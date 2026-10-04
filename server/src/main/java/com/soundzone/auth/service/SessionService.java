package com.soundzone.auth.service;

import com.soundzone.activity.service.ActivityService;
import com.soundzone.auth.entity.AuthSession;
import com.soundzone.auth.repository.AuthSessionRepository;
import com.soundzone.auth.entity.UserCredential;
import com.soundzone.auth.repository.UserCredentialRepository;
import com.soundzone.common.*;
import com.soundzone.user.entity.User;
import com.soundzone.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SessionService {
    private final AuthSessionRepository sessions;
    private final UserRepository users;
    private final UserCredentialRepository credentials;
    private final ObjectProvider<HostIdentityProvider> hostProvider;
    private final ActivityService activity;
    private final PasswordService passwords;
    private final Clock clock;

    @Value("${soundzone.guest-enabled:true}")
    private boolean guestEnabled;

    @Value("${soundzone.password-auth-enabled:true}")
    private boolean passwordAuthEnabled;

    public boolean guestEnabled() {
        return guestEnabled;
    }

    public boolean hostEnabled() {
        return hostProvider.getIfAvailable() != null;
    }

    @Transactional
    public SessionDTO guest() {
        if (!guestEnabled) throw new BizException(ResultCode.FORBIDDEN, "游客入口未开启");
        User user = new User();
        user.setName("同频游客" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        return issue(users.save(user), true);
    }

    @Transactional
    public SessionDTO host(String code) {
        HostIdentityProvider provider = hostProvider.getIfAvailable();
        if (provider == null) throw new BizException(ResultCode.INTEGRATION_UNAVAILABLE);
        var identity = provider.verify(code);
        if (identity == null || identity.subject() == null || identity.subject().length() > 128)
            throw new BizException(ResultCode.UNAUTHENTICATED);
        User user =
                users.findByHostSubject(identity.subject())
                        .orElseGet(
                                () -> {
                                    User u = new User();
                                    u.setHostSubject(identity.subject());
                                    // 昵称不作为身份；保留旧库昵称唯一约束，添加短随机后缀避免同名冲突。
                                    String name = Objects.toString(identity.displayName(), "同频用户");
                                    u.setName(
                                            name.substring(0, Math.min(20, name.length()))
                                                    + "·"
                                                    + UUID.randomUUID().toString().substring(0, 8));
                                    return users.save(u);
                                });
        return issue(user, false);
    }

    @Transactional
    public SessionDTO register(String username, String password, String existingToken) {
        requirePasswordAuth();
        String loginName = normalizeLogin(username);
        if (credentials.existsByLoginName(loginName))
            throw new BizException(ResultCode.PARAM_INVALID, "该用户名已被使用");

        User user = findUser(existingToken).filter(this::canUpgradeGuest).orElseGet(User::new);
        String displayName = username.strip();
        Optional<User> duplicate = users.findByNameIgnoreCase(displayName);
        if (duplicate.isPresent()
                && (user.getId() == null || !duplicate.get().getId().equals(user.getId())))
            throw new BizException(ResultCode.PARAM_INVALID, "该用户名已被使用");
        user.setName(displayName);
        user = users.saveAndFlush(user);

        UserCredential credential = new UserCredential();
        credential.setUser(user);
        credential.setLoginName(loginName);
        credential.setPasswordHash(passwords.hash(password));
        credentials.saveAndFlush(credential);
        sessions.deleteByUserId(user.getId());
        return issue(user, false);
    }

    @Transactional
    public SessionDTO login(String username, String password) {
        requirePasswordAuth();
        UserCredential credential =
                credentials.findByLoginName(normalizeLogin(username))
                        .orElseThrow(
                                () -> new BizException(ResultCode.UNAUTHENTICATED, "用户名或密码错误"));
        if (!passwords.matches(password, credential.getPasswordHash()))
            throw new BizException(ResultCode.UNAUTHENTICATED, "用户名或密码错误");
        sessions.deleteByUserId(credential.getUser().getId());
        return issue(credential.getUser(), false);
    }

    @Transactional
    public void logout(String token) {
        if (token != null && !token.isBlank()) sessions.deleteByTokenHash(digest(token));
    }

    @Transactional(readOnly = true)
    public CurrentSessionDTO current(String token) {
        Long userId = authenticate(token);
        User user = users.findById(userId).orElseThrow(() -> new BizException(ResultCode.UNAUTHENTICATED));
        return new CurrentSessionDTO(user.getId(), user.getName(), isGuest(user));
    }

    private SessionDTO issue(User user, boolean guest) {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        AuthSession session = new AuthSession();
        session.setUser(user);
        session.setTokenHash(digest(token));
        session.setExpiresAt(LocalDateTime.now(clock).plusDays(7));
        sessions.save(session);
        activity.visit(user.getId());
        return new SessionDTO(token, user.getId(), user.getName(), guest);
    }

    @Transactional(readOnly = true)
    public Long authenticate(String token) {
        if (token == null || token.length() > 128)
            throw new BizException(ResultCode.UNAUTHENTICATED);
        return sessions.findByTokenHashAndExpiresAtAfter(digest(token), LocalDateTime.now(clock))
                .map(s -> s.getUser().getId())
                .orElseThrow(() -> new BizException(ResultCode.UNAUTHENTICATED));
    }

    private String digest(String value) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private Optional<User> findUser(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        return sessions.findByTokenHashAndExpiresAtAfter(digest(token), LocalDateTime.now(clock))
                .map(AuthSession::getUser);
    }

    private boolean canUpgradeGuest(User user) {
        return user.getHostSubject() == null && !credentials.existsByUserId(user.getId());
    }

    private boolean isGuest(User user) {
        return user.getHostSubject() == null && !credentials.existsByUserId(user.getId());
    }

    private String normalizeLogin(String value) {
        return value.strip().toLowerCase(Locale.ROOT);
    }

    public boolean passwordAuthEnabled() {
        return passwordAuthEnabled;
    }

    private void requirePasswordAuth() {
        if (!passwordAuthEnabled)
            throw new BizException(ResultCode.FORBIDDEN, "账号登录将在 HTTPS 配置完成后开放");
    }

    public record SessionDTO(String token, Long userId, String name, boolean guest) {}

    public record CurrentSessionDTO(Long userId, String name, boolean guest) {}
}
