package com.soundzone.zone.dto;

/** 域级共享番茄钟状态；phaseEndsAt 与 serverTime 配合，避免客户端后台计时漂移。 */
public record PomodoroStateDTO(
        boolean enabled,
        String preset,
        String phase,
        long remainingSeconds,
        long phaseEndsAt,
        int periodIndex,
        int focusRound,
        int focusRounds,
        boolean longBreak,
        boolean musicByPhase) {

    public static PomodoroStateDTO disabled() {
        return new PomodoroStateDTO(false, null, null, 0, 0, 0, 0, 0, false, false);
    }
}
