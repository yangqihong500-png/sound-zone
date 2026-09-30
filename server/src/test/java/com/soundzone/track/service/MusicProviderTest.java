package com.soundzone.track.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundzone.track.entity.Track;
import com.soundzone.track.repository.TrackRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
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

        var source = provider(properties, null).resolve(track);

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

        var source = provider(properties, null).resolve(track);

        assertEquals("STREAM", source.kind());
        assertEquals("https://media.example.com/demo-one.mp3", source.streamUrl());
    }

    @Test
    void localCatalogCanResolveImportedAudioFile(@TempDir Path root) throws Exception {
        MusicProperties properties = new MusicProperties();
        MusicProperties.LocalTrack local = new MusicProperties.LocalTrack();
        local.setKey("my-demo");
        local.setAudioFile("my demo.mp3");
        properties.getLocalCatalog().add(local);
        Files.createDirectories(root.resolve("audio"));
        Files.write(root.resolve("audio/my demo.mp3"), new byte[] {1, 2, 3});
        Track track = new Track();
        track.setSource("LOCAL_LICENSED");
        track.setExternalId("my-demo");

        var source = provider(properties, root).resolve(track);

        assertEquals("STREAM", source.kind());
        assertEquals("/media/audio/my%20demo.mp3", source.streamUrl());
    }

    @Test
    void manifestImportsLocalAudioAndCover(@TempDir Path root) throws Exception {
        Path audio = Files.createDirectories(root.resolve("audio"));
        Path covers = Files.createDirectories(root.resolve("covers"));
        Files.write(audio.resolve("public song.mp3"), new byte[] {1, 2, 3});
        Files.write(covers.resolve("public song.jpg"), new byte[] {4, 5, 6});
        Files.writeString(
                root.resolve("catalog.json"),
                """
                {"version":1,"tracks":[{
                  "key":"public-song-001","title":"Public Song","artist":"Public Artist",
                  "audioFile":"public song.mp3","coverFile":"public song.jpg",
                  "coverColor":"#9FB7C9","durationSec":180,
                  "tags":["纯音乐","舒缓"],
                  "attribution":"Public Artist · Public Domain",
                  "licenseReference":"https://example.test/public-domain-proof"
                }]}
                """);

        MusicProperties properties = new MusicProperties();
        LocalCatalogRegistry registry = registry(properties, root);
        TrackRepository tracks = mock(TrackRepository.class);
        when(tracks.findFirstBySourceAndExternalId("LOCAL_LICENSED", "public-song-001"))
                .thenReturn(Optional.empty());
        when(tracks.save(any(Track.class))).thenAnswer(call -> call.getArgument(0));

        new LocalCatalogInitializer(registry, tracks, new TrackDurationPolicy(properties))
                .run(null);

        var saved = org.mockito.ArgumentCaptor.forClass(Track.class);
        verify(tracks).save(saved.capture());
        assertEquals("Public Song", saved.getValue().getTitle());
        assertEquals("/media/covers/public%20song.jpg", saved.getValue().getCoverUrl());
        assertEquals(Set.of("纯音乐", "舒缓"), saved.getValue().getTags());
        var source = new ConfiguredMusicProvider(properties, registry).resolve(saved.getValue());
        assertEquals("/media/audio/public%20song.mp3", source.streamUrl());
    }

    @Test
    void audiusMetadataMapsOnlyToKnownProductTags() {
        Set<String> tags = TrackCatalogImporter.tags("Dance & EDM / Hip-Hop", "Energizing");

        assertEquals(Set.of("电子", "说唱", "亢奋"), tags);
        assertTrue(tags.stream().allMatch(TagCatalog::isKnown));
    }

    private static ConfiguredMusicProvider provider(MusicProperties properties, Path root) {
        return new ConfiguredMusicProvider(properties, registry(properties, root));
    }

    private static LocalCatalogRegistry registry(MusicProperties properties, Path root) {
        try {
            Path base = root == null ? Files.createTempDirectory("soundzone-catalog-test") : root;
            LocalCatalogRegistry registry =
                    new LocalCatalogRegistry(
                            properties,
                            new ObjectMapper(),
                            base.resolve("catalog.json").toString(),
                            base.resolve("audio").toString(),
                            base.resolve("covers").toString());
            registry.reload();
            return registry;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
