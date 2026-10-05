package com.soundzone.zone.dto;

import com.soundzone.user.entity.User;

public record ZoneMemberPreviewDTO(Long userId, String name, String avatarColor) {
    public static ZoneMemberPreviewDTO from(User user) {
        return new ZoneMemberPreviewDTO(user.getId(), user.getName(), user.getAvatarColor());
    }
}
