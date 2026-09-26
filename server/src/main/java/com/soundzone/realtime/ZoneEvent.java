package com.soundzone.realtime;

/** 只在业务事务提交成功后广播；消息携带版本，客户端随后读取权威快照。 */
public record ZoneEvent(Long zoneId, String type, Long toUserId, Long itemId) {
    public ZoneEvent(Long zoneId) {
        this(zoneId, "CHANGED", null, null);
    }
}
