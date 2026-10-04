package com.soundzone.user.dto;

public record UserProfileDTO(
        Long id,
        String name,
        String avatarColor,
        String avatarUrl,
        String coverUrl,
        Stats stats,
        boolean followed) {
    public record Stats(long uploads, long likes, long moments, long following) {}
}
