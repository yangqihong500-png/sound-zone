package com.soundzone.queue.service;

import com.soundzone.common.*;
import com.soundzone.track.entity.Track;
import com.soundzone.track.service.TagCatalog;
import com.soundzone.zone.entity.*;

import org.springframework.stereotype.Service;

import java.util.Set;

/** 域级过滤按任一标签命中；NONE 不过滤，BAN／ALLOW 创建时必须至少选择一个标签。 */
@Service
public class TagPolicy {
    public void validate(FilterMode mode, Set<String> tags, Set<String> displayTags) {
        for (Set<String> values : java.util.List.of(tags, displayTags)) {
            if (values.size() > 60
                    || values.stream().anyMatch(t -> t == null || !TagCatalog.isKnown(t)))
                throw new BizException(ResultCode.PARAM_INVALID, "请选择目录内标签");
        }
        if (mode == FilterMode.ALLOW && tags.isEmpty())
            throw new BizException(ResultCode.PARAM_INVALID, "白名单至少选择一个标签");
        if (mode == FilterMode.NONE && (!tags.isEmpty() || !displayTags.isEmpty()))
            throw new BizException(ResultCode.PARAM_INVALID, "不限制模式不能选择过滤标签");
    }

    public void validateForCreation(FilterMode mode, Set<String> tags, Set<String> displayTags) {
        validate(mode, tags, displayTags);
        if (mode != FilterMode.NONE && tags.isEmpty())
            throw new BizException(ResultCode.PARAM_INVALID, "请至少选择一个过滤标签");
    }

    public void check(Zone zone, Track track) {
        if (zone.getFilterMode() == FilterMode.NONE) return;
        boolean hit = track.getTags().stream().anyMatch(zone.getFilterTags()::contains);
        if (zone.getFilterMode() == FilterMode.BAN && hit)
            throw new BizException(ResultCode.SONG_BANNED_BY_ZONE, track.getTitle());
        if (zone.getFilterMode() == FilterMode.ALLOW && !hit)
            throw new BizException(ResultCode.SONG_FILTERED_BY_ZONE, track.getTitle());
    }
}
