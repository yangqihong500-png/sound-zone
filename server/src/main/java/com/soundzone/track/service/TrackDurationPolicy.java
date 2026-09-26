package com.soundzone.track.service;

import com.soundzone.common.BizException;
import com.soundzone.common.ResultCode;
import com.soundzone.track.entity.Track;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

/** 单曲时长准入规则。搜索过滤用于体验，服务端写入与播放检查才是最终约束。 */
@Service
@RequiredArgsConstructor
public class TrackDurationPolicy {
    private final MusicProperties properties;

    public int maxSeconds() {
        return Math.max(60, properties.getMaxTrackDurationSeconds());
    }

    public boolean isAllowed(Track track) {
        return track != null
                && track.getDurationSec() != null
                && isAllowed(track.getDurationSec());
    }

    public boolean isAllowed(int durationSec) {
        return durationSec > 0 && durationSec <= maxSeconds();
    }

    public void check(Track track) {
        if (!isAllowed(track))
            throw new BizException(
                    ResultCode.SONG_TOO_LONG,
                    "单曲最长 " + format(maxSeconds()) + "，请选择其他歌曲");
    }

    public String limitLabel() {
        return format(maxSeconds());
    }

    private static String format(int seconds) {
        int minutes = seconds / 60;
        int remain = seconds % 60;
        return remain == 0 ? minutes + " 分钟" : minutes + " 分 " + remain + " 秒";
    }
}
