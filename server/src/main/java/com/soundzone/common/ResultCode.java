package com.soundzone.common;

import lombok.Getter;

/**
 * 业务错误码
 * 1xxx 参数类 / 2xxx 资源类 / 3xxx 业务规则类（docs/02 机制）/ 5xx 系统类
 */
@Getter
public enum ResultCode {

    SUCCESS(0, "成功"),

    PARAM_INVALID(1001, "参数校验失败"),

    ZONE_NOT_FOUND(2001, "域不存在"),
    TRACK_NOT_FOUND(2002, "歌曲不存在"),
    USER_NOT_FOUND(2003, "用户不存在"),
    QUEUE_ITEM_NOT_FOUND(2004, "队列条目不存在"),
    MOMENT_NOT_FOUND(2005, "图片分享不存在"),

    ZONE_CREATE_TRACKS_NOT_ENOUGH(3001, "创建域至少需要 3 首歌"),
    SONG_BANNED_BY_ZONE(3002, "该歌曲的标签被本域禁止"),
    SONG_NOT_ALLOWED_IN_PERIOD(3003, "该歌曲不符合当前时段的曲风要求，已转入预存队列"),
    ZONE_ALREADY_ENDED(3004, "域已结束"),
    UPLOAD_COOLDOWN(3005, "上传冷却中"),
    ZONE_PRIVATE_NEED_AUTH(3006, "私密域需要密码或邀请链接"),
    ZONE_PASSWORD_WRONG(3007, "私密域密码错误"),
    NOT_RESOURCE_OWNER(3008, "仅本人可操作"),
    SONG_FILTERED_BY_ZONE(3009, "该歌曲不在本域允许的标签范围内"),

    SYSTEM_ERROR(5000, "系统内部错误");

    private final int code;
    private final String msg;

    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
