package com.soundzone.track.service;

import static org.junit.jupiter.api.Assertions.*;

import com.soundzone.track.entity.Track;

import org.junit.jupiter.api.Test;

import java.util.Set;

class MusicProviderTest {
    @Test
    void audiusPlaybackUsesOfficialStreamEndpoint() {
        MusicProperties properties = new MusicProperties();
        properties.getAudius().setBaseUrl("https://api.audius.co/v1/");
        properties.getAudius().setAppName("SoundZone Demo");
        Track track = new Track();
        track.setSource("AUDIUS");
        track.setExternalId("Y96Ry");

        var source = new ConfiguredMusicProvider(properties).resolve(track);

        assertEquals("STREAM", source.kind());
        assertEquals("AUDIUS", source.source());
        assertEquals(
                "https://api.audius.co/v1/tracks/Y96Ry/stream?app_name=SoundZone%20Demo",
                source.streamUrl());
    }

    @Test
    void localCatalogRequiresConfiguredLicensedStream() {
        MusicProperties properties = new MusicProperties();
        MusicProperties.LocalTrack local = new MusicProperties.LocalTrack();
        local.setKey("demo-one");
        local.setStreamUrl("https://media.example.com/demo-one.mp3");
        properties.getLocalCatalog().add(local);
        Track track = new Track();
        track.setSource("LOCAL_LICENSED");
        track.setExternalId("demo-one");

        var source = new ConfiguredMusicProvider(properties).resolve(track);

        assertEquals("STREAM", source.kind());
        assertEquals("https://media.example.com/demo-one.mp3", source.streamUrl());
    }

    @Test
    void localCatalogCanResolveImportedAudioFile() {
        MusicProperties properties = new MusicProperties();
        MusicProperties.LocalTrack local = new MusicProperties.LocalTrack();
        local.setKey("my-demo");
        local.setAudioFile("my demo.mp3");
        properties.getLocalCatalog().add(local);
        Track track = new Track();
        track.setSource("LOCAL_LICENSED");
        track.setExternalId("my-demo");

        var source = new ConfiguredMusicProvider(properties).resolve(track);

        assertEquals("STREAM", source.kind());
        assertEquals("/media/audio/my%20demo.mp3", source.streamUrl());
    }

    @Test
    void audiusMetadataMapsOnlyToKnownProductTags() {
        Set<String> tags = TrackCatalogImporter.tags("Dance & EDM / Hip-Hop", "Energizing");

        assertEquals(Set.of("电子", "说唱", "亢奋"), tags);
        assertTrue(tags.stream().allMatch(TagCatalog::isKnown));
    }
}
