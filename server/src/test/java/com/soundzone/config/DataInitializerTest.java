package com.soundzone.config;

import static org.junit.jupiter.api.Assertions.*;

import com.soundzone.track.entity.Track;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class DataInitializerTest {

    @Test
    void completeTaggedPlaylistIsKeptForItsResidentZone() {
        List<Track> catalog = new ArrayList<>();
        for (int i = 0; i < 7; i++) catalog.add(track("Ye " + i, Set.of("亢奋", "说唱")));
        for (int i = 0; i < 4; i++) catalog.add(track("BTTB " + i, Set.of("古典", "纯音乐")));

        List<Track> selected = DataInitializer.selectPlaylist(catalog, Set.of("亢奋"), 2);

        assertEquals(7, selected.size());
        assertTrue(selected.stream().allMatch(track -> track.getTags().contains("亢奋")));
        assertEquals("Ye 0", selected.get(0).getTitle());
    }

    @Test
    void incompleteTaggedGroupFallsBackToFourCatalogTracks() {
        List<Track> catalog =
                List.of(
                        track("One", Set.of("专注")),
                        track("Two", Set.of("流行")),
                        track("Three", Set.of("电子")),
                        track("Four", Set.of("民谣")),
                        track("Five", Set.of("摇滚")));

        List<Track> selected = DataInitializer.selectPlaylist(catalog, Set.of("专注"), 1);

        assertEquals(
                List.of("Two", "Three", "Four", "Five"),
                selected.stream().map(Track::getTitle).toList());
    }

    @Test
    void nightPlaylistShowsDifferentCoversFromCompatibleSongs() {
        List<Track> catalog = new ArrayList<>();
        for (int i = 0; i < 4; i++) catalog.add(track("Love " + i, Set.of("情歌"), "album-a"));
        catalog.add(track("Soft", Set.of("舒缓"), "album-b"));
        catalog.add(track("City", Set.of("流行"), "album-c"));

        List<Track> selected = DataInitializer.selectNightPlaylist(catalog, 0);

        assertEquals(List.of("Love 0", "Soft", "City"),
                selected.stream().map(Track::getTitle).toList());
        assertEquals(3, selected.stream().map(Track::getCoverUrl).distinct().count());
    }

    private Track track(String title, Set<String> tags) {
        return track(title, tags, null);
    }

    private Track track(String title, Set<String> tags, String coverUrl) {
        Track track = new Track();
        track.setTitle(title);
        track.setCoverUrl(coverUrl);
        track.getTags().addAll(tags);
        return track;
    }
}
