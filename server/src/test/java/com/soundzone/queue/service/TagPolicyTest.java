package com.soundzone.queue.service;

import static org.junit.jupiter.api.Assertions.*;

import com.soundzone.common.BizException;
import com.soundzone.common.ResultCode;
import com.soundzone.track.entity.Track;
import com.soundzone.zone.entity.FilterMode;
import com.soundzone.zone.entity.Zone;

import org.junit.jupiter.api.Test;

import java.util.Set;

class TagPolicyTest {
    private final TagPolicy policy = new TagPolicy();

    @Test
    void banRejectsAnySelectedTagAndAllowsNonMatchingTrack() {
        Zone zone = zone(FilterMode.BAN, "日语", "电子");

        BizException rejected =
                assertThrows(BizException.class, () -> policy.check(zone, track("日语", "流行")));
        assertEquals(ResultCode.SONG_BANNED_BY_ZONE, rejected.getResultCode());
        assertDoesNotThrow(() -> policy.check(zone, track("英语", "民谣")));
    }

    @Test
    void allowUsesOrWithinCategoryAndAndAcrossCategories() {
        Zone zone = zone(FilterMode.ALLOW, "日语", "韩语", "流行", "舒缓");

        assertDoesNotThrow(() -> policy.check(zone, track("韩语", "流行", "舒缓")));
        assertFiltered(zone, track("日语", "摇滚", "舒缓"));
        assertFiltered(zone, track("英语", "流行", "舒缓"));
        assertFiltered(zone, track());
    }

    @Test
    void newFiltersRejectSceneTagsButTrackCatalogMayStillKeepThem() {
        BizException error =
                assertThrows(
                        BizException.class,
                        () ->
                                policy.validateForCreation(
                                        FilterMode.BAN, Set.of("通勤"), Set.of()));
        assertEquals(ResultCode.PARAM_INVALID, error.getResultCode());
    }

    private void assertFiltered(Zone zone, Track track) {
        BizException error = assertThrows(BizException.class, () -> policy.check(zone, track));
        assertEquals(ResultCode.SONG_FILTERED_BY_ZONE, error.getResultCode());
    }

    private Zone zone(FilterMode mode, String... tags) {
        Zone zone = new Zone();
        zone.setFilterMode(mode);
        zone.getFilterTags().addAll(Set.of(tags));
        return zone;
    }

    private Track track(String... tags) {
        Track track = new Track();
        track.setTitle("测试曲目");
        track.getTags().addAll(Set.of(tags));
        return track;
    }
}
