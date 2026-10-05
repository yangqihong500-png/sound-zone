package com.soundzone;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.soundzone.auth.service.*;
import com.soundzone.common.*;
import com.soundzone.feedback.repository.*;
import com.soundzone.feedback.service.FeedbackService;
import com.soundzone.message.dto.*;
import com.soundzone.message.repository.DirectMessageRepository;
import com.soundzone.message.service.DirectMessageService;
import com.soundzone.notification.entity.NotificationType;
import com.soundzone.notification.repository.NotificationRepository;
import com.soundzone.notification.service.NotificationService;
import com.soundzone.moment.dto.*;
import com.soundzone.moment.entity.*;
import com.soundzone.moment.repository.*;
import com.soundzone.moment.service.*;
import com.soundzone.queue.dto.*;
import com.soundzone.queue.entity.*;
import com.soundzone.queue.repository.*;
import com.soundzone.queue.service.*;
import com.soundzone.realtime.ZoneEvent;
import com.soundzone.track.entity.Track;
import com.soundzone.track.repository.TrackRepository;
import com.soundzone.track.service.TrackService;
import com.soundzone.user.entity.User;
import com.soundzone.user.repository.UserRepository;
import com.soundzone.user.service.UserService;
import com.soundzone.zone.dto.*;
import com.soundzone.zone.entity.*;
import com.soundzone.zone.repository.*;
import com.soundzone.zone.service.*;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.*;
import java.nio.file.Files;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;

/** 同一套行为测试可运行于 H2 或迁移后的独立 MySQL 8 测试库。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(CoreWorkflowTest.TimeConfig.class)
@RecordApplicationEvents
class CoreWorkflowTest {
    @Autowired ZoneService zones;
    @Autowired ZoneRepository zoneRepo;
    @Autowired ZoneMemberRepository members;
    @Autowired QueueService queue;
    @Autowired QueueItemRepository queueRepo;
    @Autowired QueueLikeRepository likeRepo;
    @Autowired PlaybackService playback;
    @Autowired MomentService moments;
    @Autowired ImageStorage imageStorage;
    @Autowired MomentRepository momentRepo;
    @Autowired TrainingExportService training;
    @Autowired FeedbackService feedback;
    @Autowired TrackCollectionRepository collections;
    @Autowired TrackRepository tracks;
    @Autowired TrackService trackService;
    @Autowired UserRepository users;
    @Autowired UserService userService;
    @Autowired DirectMessageService directMessages;
    @Autowired DirectMessageRepository directMessageRepo;
    @Autowired NotificationService notificationService;
    @Autowired NotificationRepository notificationRepo;
    @Autowired SessionService sessions;
    @Autowired MutableClock clock;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ApplicationEvents applicationEvents;
    @LocalServerPort int port;
    User host, listener, stranger;
    List<Track> songs;

    @TestConfiguration
    static class TimeConfig {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock();
        }
    }

    static class MutableClock extends Clock {
        private final AtomicReference<Instant> now = new AtomicReference<>();

        void set(Instant instant) {
            now.set(instant);
        }

        void advance(long seconds) {
            now.updateAndGet(t -> t.plusSeconds(seconds));
        }

        public ZoneId getZone() {
            return ZoneId.of("Asia/Shanghai");
        }

        public Clock withZone(ZoneId zone) {
            return this;
        }

        public Instant instant() {
            return now.get();
        }
    }

    @BeforeEach
    void setup() {
        String url = jdbc.execute((java.sql.Connection c) -> c.getMetaData().getURL());
        assertTrue(
                url.contains("soundzone_test") || url.contains("soundzone_codex_test_"),
                "禁止在业务库清理测试数据");
        for (String table :
                List.of(
                        "sz_notification",
                        "sz_training_export_item",
                        "sz_moment_reaction",
                        "sz_queue_like",
                        "sz_track_collection",
                        "sz_report",
                        "sz_activity_event",
                        "sz_feedback_event",
                        "sz_moment",
                        "sz_queue_item",
                        "sz_zone_member",
                        "sz_zone_period_tags",
                        "sz_zone_period",
                        "sz_zone_filter_tags",
                        "sz_zone_tags",
                        "sz_zone",
                        "sz_track_tags",
                        "sz_track",
                        "sz_direct_message",
                        "sz_follow",
                        "sz_auth_session",
                        "sz_user_credential",
                        "sz_user")) jdbc.update("DELETE FROM " + table);
        clock.set(Instant.parse("2026-09-25T10:00:00Z"));
        host = user("域主");
        listener = user("听众");
        stranger = user("旁观者");
        songs = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Track t = new Track();
            t.setTitle("测试曲" + i);
            t.setArtist("测试作者");
            t.setDurationSec(10);
            t.getTags().add("舒缓");
            songs.add(tracks.save(t));
        }
    }

    private User user(String name) {
        User u = new User();
        u.setName(name);
        return users.save(u);
    }

    private ZoneCreateRequest request(
            String visibility, String password, String mode, Set<String> filters, List<Long> ids) {
        return new ZoneCreateRequest(
                "考研自习",
                "自习",
                999L,
                ids,
                visibility,
                password,
                mode,
                filters,
                Set.of("舒缓"),
                null,
                null);
    }

    private ZoneDetailDTO create() {
        return zones.createZone(
                request(
                        "PUBLIC",
                        null,
                        "BAN",
                        Set.of("电子"),
                        songs.subList(0, 3).stream().map(Track::getId).toList()),
                host.getId());
    }

    private List<ZoneCreateRequest.PeriodConfig> classicPeriods(
            Set<String> focusTags, Set<String> breakTags) {
        int[] minutes = {25, 5, 25, 5, 25, 5, 25, 15};
        List<ZoneCreateRequest.PeriodConfig> result = new ArrayList<>();
        for (int i = 0; i < minutes.length; i++) {
            String type = i % 2 == 0 ? "FOCUS" : "BREAK";
            result.add(
                    new ZoneCreateRequest.PeriodConfig(
                            i, minutes[i], type, "FOCUS".equals(type) ? focusTags : breakTags));
        }
        return result;
    }

    private void join(Long id, Long user) {
        zones.joinZone(id, new JoinZoneRequest(999L, null, null), user);
    }

    private MockMultipartFile image() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", out);
        return new MockMultipartFile("file", "photo.png", "image/png", out.toByteArray());
    }

    private MomentDTO share(ZoneDetailDTO zone, boolean consent) throws Exception {
        return moments.create(
                zone.id(),
                host.getId(),
                new MomentCreateRequest(zone.imageCandidate().itemId(), null, consent),
                image());
    }

    private String tokenFor(Long userId) {
        // 测试通过真实随机会话，再将其绑定测试用户；生产不存在此入口。
        var guest = sessions.guest();
        jdbc.update("UPDATE sz_auth_session SET user_id=? WHERE user_id=?", userId, guest.userId());
        return guest.token();
    }

    @Test
    void initialOrderAndFilteringAreValidatedAtomically() {
        songs.forEach(song -> song.setDurationSec(200));
        tracks.saveAllAndFlush(songs);
        var ids = List.of(songs.get(2).getId(), songs.get(0).getId(), songs.get(1).getId());
        var zone = zones.createZone(request("PUBLIC", null, "BAN", Set.of("电子"), ids), host.getId());
        assertEquals(ids.get(0), zone.nowPlaying().trackId());
        assertEquals(ids.subList(1, 3), zone.queue().stream().map(QueueItemDTO::trackId).toList());
        assertEquals(host.getId(), zone.hostId());
        assertEquals(300, queue.cooldownRemainSeconds(zone.id(), host.getId()));
        assertEquals(5, queue.cooldown(zone.id(), host.getId()).cooldownMinutes());
        clock.advance(299);
        assertEquals(1, queue.cooldownRemainSeconds(zone.id(), host.getId()));
        clock.advance(1);
        assertEquals(0, queue.cooldownRemainSeconds(zone.id(), host.getId()));
        assertEquals(4, trackService.search("").size());
        assertTrue(zone.tags().isEmpty());
        assertTrue(zones.listActive("电子", null).isEmpty());
        assertTrue(zones.listActive(null, "电子").isEmpty());
        assertThrows(
                BizException.class,
                () -> zones.createZone(request("PUBLIC", null, "BAN", Set.of(), ids), host.getId()));
        assertThrows(
                BizException.class,
                () ->
                        zones.createZone(
                                request("PUBLIC", null, "ALLOW", Set.of(), ids), host.getId()));
        assertThrows(
                BizException.class,
                () ->
                        zones.createZone(
                                request("PUBLIC", null, "BAN", Set.of("舒缓"), ids), host.getId()));
        assertThrows(
                BizException.class,
                () ->
                        zones.createZone(
                                request(
                                        "PUBLIC",
                                        null,
                                        "BAN",
                                        Set.of("电子"),
                                        List.of(ids.get(0), ids.get(1), ids.get(2), 99999L)),
                                host.getId()));
        assertEquals(1, zoneRepo.count());
    }

    @Test
    void quickCreateRequiresSceneAndExactlyThreeTracksWhileKeepingOtherSafeDefaults() {
        var ids = songs.subList(0, 3).stream().map(Track::getId).toList();
        var zone =
                zones.createZone(
                        new ZoneCreateRequest(
                                "随便听听",
                                "自习",
                                null,
                                ids,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null),
                        host.getId());

        assertEquals("自习", zone.scene());
        assertEquals("PUBLIC", zone.visibility());
        assertEquals("NONE", zone.filterMode());
        assertTrue(zone.tags().isEmpty());
        assertTrue(zone.filterTags().isEmpty());
        assertEquals(ids.get(0), zone.nowPlaying().trackId());
        assertEquals(ids.subList(1, 3), zone.queue().stream().map(QueueItemDTO::trackId).toList());
        assertEquals(300, queue.cooldownRemainSeconds(zone.id(), host.getId()));

        assertThrows(
                BizException.class,
                () ->
                        zones.createZone(
                                new ZoneCreateRequest(
                                        "没有场景",
                                        null,
                                        null,
                                        ids,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null),
                                listener.getId()));

        assertThrows(
                BizException.class,
                () ->
                        zones.createZone(
                                new ZoneCreateRequest(
                                        "四首不允许",
                                        "自习",
                                        null,
                                        songs.stream().map(Track::getId).toList(),
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null),
                                listener.getId()));
        assertThrows(
                BizException.class,
                () ->
                        zones.createZone(
                                new ZoneCreateRequest(
                                        "不限制不能带标签",
                                        "自习",
                                        null,
                                        ids,
                                        null,
                                        null,
                                        "NONE",
                                        Set.of("舒缓"),
                                        null,
                                        null,
                                        null),
                                listener.getId()));
        assertEquals(1, zoneRepo.count());
    }

    @Test
    void zoneCoverUsesFirstTrackOrOneTimeCustomImageAndNeverFollowsPlayback() throws Exception {
        songs.get(0).setCoverUrl("https://example.test/first.jpg");
        songs.get(1).setCoverUrl("https://example.test/second.jpg");
        tracks.saveAllAndFlush(songs);
        var ids = songs.subList(0, 3).stream().map(Track::getId).toList();

        var automatic =
                zones.createZone(
                        request("PUBLIC", null, "BAN", Set.of("电子"), ids), host.getId());
        assertEquals("https://example.test/first.jpg", automatic.coverUrl());

        clock.advance(11);
        var advanced = zones.getDetail(automatic.id(), host.getId());
        assertEquals(songs.get(1).getId(), advanced.nowPlaying().trackId());
        assertEquals("https://example.test/first.jpg", advanced.coverUrl());

        var renamed =
                zones.update(
                        automatic.id(),
                        host.getId(),
                        new ZoneUpdateRequest("封面保持不变", "深夜", null, null));
        assertEquals("https://example.test/first.jpg", renamed.coverUrl());

        var custom =
                zones.createZone(
                        request("PUBLIC", null, "BAN", Set.of("电子"), ids),
                        listener.getId(),
                        image());
        assertTrue(custom.coverUrl().startsWith("/zone-covers/cover-"));
        String key = custom.coverUrl().substring(custom.coverUrl().lastIndexOf('/') + 1);
        assertTrue(Files.isRegularFile(imageStorage.resolveZoneCover(key)));
    }

    @Test
    void multipartCreateEndpointAcceptsOptionalCustomCover() throws Exception {
        var ids = songs.subList(0, 3).stream().map(Track::getId).toList();
        String payload =
                json.writeValueAsString(
                        request("PUBLIC", null, "BAN", Set.of("电子"), ids));

        var response =
                mvc.perform(
                                multipart("/zones/with-cover")
                                        .file(image())
                                        .param("payload", payload)
                                        .header(
                                                "Authorization",
                                                "Bearer " + tokenFor(host.getId())))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value(0))
                        .andExpect(jsonPath("$.data.coverUrl").isString())
                        .andReturn();

        Zone saved = zoneRepo.findAll().get(0);
        assertTrue(saved.getCoverUrl().startsWith("/zone-covers/cover-"));
        String coverUrl =
                json.readTree(response.getResponse().getContentAsString())
                        .path("data")
                        .path("coverUrl")
                        .asText();
        mvc.perform(get(coverUrl))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void profileCoverCanBeUploadedAndViewedPubliclyButOnlyByItsOwner() throws Exception {
        String ownerToken = tokenFor(host.getId());
        mvc.perform(multipart("/users/me/cover").file(image()))
                .andExpect(status().isUnauthorized());

        var uploaded =
                mvc.perform(
                                multipart("/users/me/cover")
                                        .file(image())
                                        .header("Authorization", "Bearer " + ownerToken))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value(0))
                        .andReturn();
        String coverUrl =
                json.readTree(uploaded.getResponse().getContentAsString())
                        .path("data")
                        .path("coverUrl")
                        .asText();
        assertTrue(coverUrl.startsWith("/profile-covers/profile-"));
        assertEquals(coverUrl, userService.getProfile(host.getId(), listener.getId()).coverUrl());
        assertNull(userService.getProfile(listener.getId(), host.getId()).coverUrl());
        mvc.perform(get(coverUrl))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        mvc.perform(get("/profile-covers/profile-invalid.jpg")).andExpect(status().isNotFound());

        var replacement =
                mvc.perform(
                                multipart("/users/me/cover")
                                        .file(image())
                                        .header("Authorization", "Bearer " + ownerToken))
                        .andExpect(status().isOk())
                        .andReturn();
        String replacementUrl =
                json.readTree(replacement.getResponse().getContentAsString())
                        .path("data")
                        .path("coverUrl")
                        .asText();
        assertNotEquals(coverUrl, replacementUrl);
        mvc.perform(get(coverUrl)).andExpect(status().isNotFound());
        mvc.perform(get(replacementUrl)).andExpect(status().isOk());
    }

    @Test
    void profileIdentityCanUpdatePublicIdAvatarAndRegisteredLoginName() throws Exception {
        String ownerToken = tokenFor(host.getId());
        mvc.perform(
                        put("/users/me/profile")
                                .contentType("application/json")
                                .content("{\"name\":\"New.Listener\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(
                        put("/users/me/profile")
                                .header("Authorization", "Bearer " + ownerToken)
                                .contentType("application/json")
                                .content("{\"name\":\"New.Listener\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("New.Listener"));
        assertEquals("New.Listener", users.findById(host.getId()).orElseThrow().getName());
        mvc.perform(
                        put("/users/me/profile")
                                .header("Authorization", "Bearer " + ownerToken)
                                .contentType("application/json")
                                .content("{\"name\":\"" + listener.getName() + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));

        var uploaded =
                mvc.perform(
                                multipart("/users/me/avatar")
                                        .file(image())
                                        .header("Authorization", "Bearer " + ownerToken))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.avatarUrl").isString())
                        .andReturn();
        String avatarUrl =
                json.readTree(uploaded.getResponse().getContentAsString())
                        .path("data")
                        .path("avatarUrl")
                        .asText();
        assertTrue(avatarUrl.startsWith("/profile-avatars/avatar-"));
        assertEquals(avatarUrl, userService.getProfile(host.getId(), listener.getId()).avatarUrl());
        mvc.perform(get(avatarUrl))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"));
        mvc.perform(get("/profile-avatars/avatar-invalid.jpg")).andExpect(status().isNotFound());

        var replacement =
                mvc.perform(
                                multipart("/users/me/avatar")
                                        .file(image())
                                        .header("Authorization", "Bearer " + ownerToken))
                        .andExpect(status().isOk())
                        .andReturn();
        String replacementUrl =
                json.readTree(replacement.getResponse().getContentAsString())
                        .path("data")
                        .path("avatarUrl")
                        .asText();
        assertNotEquals(avatarUrl, replacementUrl);
        mvc.perform(get(avatarUrl)).andExpect(status().isNotFound());
        mvc.perform(get(replacementUrl)).andExpect(status().isOk());

        userService.follow(listener.getId(), host.getId());
        assertEquals(
                replacementUrl, userService.following(listener.getId()).get(0).get("avatarUrl"));

        var guest = sessions.guest();
        var registered = sessions.register("Profile_User", "secure-pass-123", guest.token());
        userService.updateName(registered.userId(), "Renamed_User");
        sessions.logout(registered.token());
        assertEquals(
                registered.userId(),
                sessions.login("renamed_user", "secure-pass-123").userId());
        assertThrows(
                BizException.class,
                () -> sessions.login("profile_user", "secure-pass-123"));
    }

    @Test
    void localCatalogMediaServesCoverAndRangeAudio() throws Exception {
        var cover =
                java.nio.file.Path.of(
                        System.getProperty("java.io.tmpdir"),
                        "soundzone-test-covers",
                        "local-cover.jpg");
        var audio =
                java.nio.file.Path.of(
                        System.getProperty("java.io.tmpdir"),
                        "soundzone-test-audio",
                        "local-audio.mp3");
        byte[] coverBytes = new byte[] {1, 2, 3, 4};
        byte[] audioBytes = new byte[] {10, 20, 30, 40, 50};
        Files.createDirectories(cover.getParent());
        Files.createDirectories(audio.getParent());
        Files.write(cover, coverBytes);
        Files.write(audio, audioBytes);
        try {
            mvc.perform(get("/media/covers/local-cover.jpg"))
                    .andExpect(status().isOk())
                    .andExpect(content().bytes(coverBytes));
            mvc.perform(get("/media/audio/local-audio.mp3").header("Range", "bytes=1-3"))
                    .andExpect(status().isPartialContent())
                    .andExpect(header().string("Content-Range", "bytes 1-3/5"))
                    .andExpect(content().bytes(new byte[] {20, 30, 40}));
        } finally {
            Files.deleteIfExists(cover);
            Files.deleteIfExists(audio);
        }
    }

    @Test
    void trackSearchFindsAllPublicZonesAndRanksCurrentBeforeQueuedAndPreset() {
        var first = create();
        var second =
                zones.createZone(
                        request(
                                "PUBLIC",
                                null,
                                "BAN",
                                Set.of("电子"),
                                List.of(
                                        songs.get(1).getId(),
                                        songs.get(2).getId(),
                                        songs.get(3).getId())),
                        listener.getId());
        zones.createZone(
                request(
                        "PRIVATE",
                        "0707",
                        "BAN",
                        Set.of("电子"),
                        List.of(
                                songs.get(1).getId(),
                                songs.get(2).getId(),
                                songs.get(3).getId())),
                stranger.getId());

        var songOne = zones.listActive(null, "测试曲1");
        assertEquals(
                List.of(second.id(), first.id()),
                songOne.stream().map(ZoneSummaryDTO::id).toList());
        assertEquals("PLAYING", songOne.get(0).searchMatch().type());
        assertNull(songOne.get(0).searchMatch().position());
        assertEquals("QUEUED", songOne.get(1).searchMatch().type());
        assertEquals(1, songOne.get(1).searchMatch().position());

        QueueItem firstSongTwo =
                queueRepo.findByZoneIdAndStatusOrderByCreatedAtAscIdAsc(
                                first.id(), QueueStatus.QUEUED)
                        .stream()
                        .filter(q -> q.getTrack().getId().equals(songs.get(2).getId()))
                        .findFirst()
                        .orElseThrow();
        firstSongTwo.setStatus(QueueStatus.PRESET);
        queueRepo.saveAndFlush(firstSongTwo);
        var songTwo = zones.listActive(null, "测试曲2");
        assertEquals(
                List.of(second.id(), first.id()),
                songTwo.stream().map(ZoneSummaryDTO::id).toList());
        assertEquals("QUEUED", songTwo.get(0).searchMatch().type());
        assertEquals("PRESET", songTwo.get(1).searchMatch().type());

        QueueItem firstSongOne =
                queueRepo.findByZoneIdAndStatusOrderByCreatedAtAscIdAsc(
                                first.id(), QueueStatus.QUEUED)
                        .stream()
                        .filter(q -> q.getTrack().getId().equals(songs.get(1).getId()))
                        .findFirst()
                        .orElseThrow();
        firstSongOne.setStatus(QueueStatus.PLAYED);
        queueRepo.saveAndFlush(firstSongOne);
        assertEquals(
                List.of(second.id()),
                zones.listActive(null, "测试曲1").stream().map(ZoneSummaryDTO::id).toList());

        assertFalse(zones.listActive(null, "考研自习").isEmpty());
        assertTrue(
                zones.listActive(null, "考研自习").stream()
                        .allMatch(z -> z.searchMatch() == null));
    }

    @Test
    void overlongTracksAreHiddenRejectedAndRemovedFromLegacyQueues() {
        Track overlong = new Track();
        overlong.setTitle("超长混音");
        overlong.setArtist("测试作者");
        overlong.setDurationSec(601);
        overlong.getTags().add("舒缓");
        overlong = tracks.saveAndFlush(overlong);

        assertTrue(trackService.search("超长混音").isEmpty());
        Long overlongId = overlong.getId();
        var createError =
                assertThrows(
                        BizException.class,
                        () ->
                                zones.createZone(
                                        request(
                                                "PUBLIC",
                                                null,
                                                "BAN",
                                                Set.of("电子"),
                                                List.of(
                                                        overlongId,
                                                        songs.get(0).getId(),
                                                        songs.get(1).getId())),
                                        host.getId()));
        assertEquals(ResultCode.SONG_TOO_LONG, createError.getResultCode());
        assertEquals(0, zoneRepo.count());

        var zone = create();
        join(zone.id(), listener.getId());
        var uploadError =
                assertThrows(
                        BizException.class,
                        () ->
                                queue.requestSong(
                                        zone.id(),
                                        new SongRequest(overlongId, null),
                                        listener.getId()));
        assertEquals(ResultCode.SONG_TOO_LONG, uploadError.getResultCode());

        QueueItem legacy = queueRepo.findById(zone.nowPlaying().itemId()).orElseThrow();
        legacy.getTrack().setDurationSec(601);
        tracks.saveAndFlush(legacy.getTrack());
        playback.tick(zone.id());
        assertEquals(
                QueueStatus.REMOVED,
                queueRepo.findById(legacy.getId()).orElseThrow().getStatus());
        assertEquals(
                songs.get(1).getId(),
                zones.getDetail(zone.id(), host.getId()).nowPlaying().trackId());
    }

    @Test
    void oneTagSelectionControlsAllowModeAndEditingKeepsItsRule() {
        var ids = songs.subList(0, 3).stream().map(Track::getId).toList();
        var zone = zones.createZone(
                request("PUBLIC", null, "ALLOW", Set.of("舒缓"), ids), host.getId());
        assertEquals(Set.of("舒缓"), zone.tags());
        assertEquals(Set.of("舒缓"), zone.filterTags());
        assertEquals(1, zones.listActive(null, "舒缓").size());
        var edited = zones.update(zone.id(), host.getId(),
                new ZoneUpdateRequest("新域名", "自习", null, null));
        assertEquals(Set.of("舒缓"), edited.tags());
        assertEquals(Set.of("舒缓"), edited.filterTags());
        assertEquals("ALLOW", edited.filterMode());
    }

    @Test
    void concurrentUploadsOnlyAcceptOne() throws Exception {
        var z = create();
        join(z.id(), listener.getId());
        ExecutorService pool = Executors.newFixedThreadPool(4);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < 4; i++)
                results.add(
                        pool.submit(
                                () -> {
                                    start.await();
                                    try {
                                        queue.requestSong(
                                                z.id(),
                                                new SongRequest(songs.get(3).getId(), host.getId()),
                                                listener.getId());
                                        return 0;
                                    } catch (BizException e) {
                                        return e.getResultCode().getCode();
                                    }
                                }));
            start.countDown();
            List<Integer> codes = new ArrayList<>();
            for (var result : results) codes.add(result.get(15, TimeUnit.SECONDS));
            assertEquals(1, codes.stream().filter(c -> c == 0).count());
            assertEquals(3, codes.stream().filter(c -> c == 3005).count());
            assertEquals(4, queueRepo.count());
            assertEquals(300, queue.cooldownRemainSeconds(z.id(), listener.getId()));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void playbackAdvancesWaitsAndRestartsWithoutClientSkipping() {
        var z = create();
        join(z.id(), listener.getId());
        clock.advance(10);
        playback.tick(z.id());
        assertEquals(
                songs.get(1).getId(), zones.getDetail(z.id(), host.getId()).nowPlaying().trackId());
        clock.advance(20);
        playback.tick(z.id());
        assertNull(zones.getDetail(z.id(), host.getId()).nowPlaying());
        assertEquals(ZoneStatus.ACTIVE, zoneRepo.findById(z.id()).orElseThrow().getStatus());
        var added =
                queue.requestSong(
                        z.id(), new SongRequest(songs.get(3).getId(), null), listener.getId());
        assertEquals("PLAYING", added.status());
        zones.leaveZone(z.id(), host.getId());
        zones.leaveZone(z.id(), listener.getId());
        assertEquals(ZoneStatus.ENDED, zoneRepo.findById(z.id()).orElseThrow().getStatus());
        assertEquals(
                QueueStatus.STOPPED, queueRepo.findById(added.itemId()).orElseThrow().getStatus());
    }

    @Test
    void endingZoneStopsEveryUnplayedQueueItem() {
        var z = create();
        join(z.id(), listener.getId());

        zones.leaveZone(z.id(), host.getId());
        zones.leaveZone(z.id(), listener.getId());

        assertEquals(ZoneStatus.ENDED, zoneRepo.findById(z.id()).orElseThrow().getStatus());
        assertEquals(
                Set.of(QueueStatus.STOPPED),
                queueRepo.findAll().stream().map(QueueItem::getStatus).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void queuedSongCanOnlyBeWithdrawnByItsUploader() throws Exception {
        var z = create();
        join(z.id(), listener.getId());
        var added =
                queue.requestSong(
                        z.id(), new SongRequest(songs.get(3).getId(), null), listener.getId());
        assertEquals("QUEUED", added.status());

        var ownershipError =
                assertThrows(
                        BizException.class,
                        () -> queue.withdraw(z.id(), added.itemId(), host.getId()));
        assertEquals(ResultCode.NOT_RESOURCE_OWNER, ownershipError.getResultCode());
        assertThrows(
                BizException.class,
                () -> queue.withdraw(z.id(), z.nowPlaying().itemId(), host.getId()));

        mvc.perform(
                        delete("/queue/" + added.itemId())
                                .header(
                                        "Authorization",
                                        "Bearer " + tokenFor(listener.getId())))
                .andExpect(status().isOk());
        assertEquals(
                QueueStatus.REMOVED,
                queueRepo.findById(added.itemId()).orElseThrow().getStatus());
        assertTrue(
                zones.getDetail(z.id(), listener.getId()).queue().stream()
                        .noneMatch(item -> item.itemId().equals(added.itemId())));
    }

    @Test
    void sharedPomodoroPromotesPresetSongsWithoutInterruptingCurrentTrack() {
        var ids = songs.subList(0, 3).stream().map(Track::getId).toList();
        var request =
                new ZoneCreateRequest(
                        "同频专注室",
                        "自习",
                        null,
                        ids,
                        "PUBLIC",
                        null,
                        "NONE",
                        Set.of(),
                        Set.of(),
                        null,
                        classicPeriods(Set.of("舒缓"), Set.of("流行")));
        var created = zones.createZone(request, host.getId());
        assertTrue(created.pomodoro().enabled());
        assertEquals("CLASSIC", created.pomodoro().preset());
        assertEquals("FOCUS", created.pomodoro().phase());
        assertTrue(zones.listActive(null, null).get(0).pomodoro().enabled());

        join(created.id(), listener.getId());
        clock.advance(30);
        playback.tick(created.id());
        assertNull(zones.getDetail(created.id(), host.getId()).nowPlaying());

        clock.advance(1460); // 创建后 24:50，留十秒跨越首个阶段边界。
        zones.heartbeat(created.id(), host.getId(), null, false);
        zones.heartbeat(created.id(), listener.getId(), null, false);
        Track focusTrack = songs.get(3);
        focusTrack.setDurationSec(600);
        tracks.saveAndFlush(focusTrack);
        var current =
                queue.requestSong(
                        created.id(), new SongRequest(focusTrack.getId(), null), host.getId());
        assertEquals("PLAYING", current.status());

        Track breakTrack = new Track();
        breakTrack.setTitle("休息段歌曲");
        breakTrack.setArtist("测试作者");
        breakTrack.setDurationSec(120);
        breakTrack.getTags().add("流行");
        breakTrack = tracks.saveAndFlush(breakTrack);
        var saved =
                queue.requestSong(
                        created.id(), new SongRequest(breakTrack.getId(), null), listener.getId());
        assertEquals("PRESET", saved.status());

        clock.advance(20);
        playback.tick(created.id());
        var detail = zones.getDetail(created.id(), host.getId());
        assertEquals("BREAK", detail.pomodoro().phase());
        assertEquals(current.itemId(), detail.nowPlaying().itemId());
        assertEquals(List.of(saved.itemId()), detail.queue().stream().map(QueueItemDTO::itemId).toList());
        assertTrue(detail.presetQueue().isEmpty());
    }

    @Test
    void listeningSummaryCountsOnlyServerConfirmedPlaybackAndIsPrivateToCurrentUser()
            throws Exception {
        songs.forEach(song -> song.setDurationSec(200));
        tracks.saveAllAndFlush(songs);
        var zone = create();
        join(zone.id(), listener.getId());

        clock.advance(20);
        zones.heartbeat(zone.id(), listener.getId(), zone.nowPlaying().itemId(), true);
        clock.advance(20);
        zones.heartbeat(zone.id(), listener.getId(), zone.nowPlaying().itemId(), false);

        var summary = userService.listeningSummary(listener.getId());
        assertEquals(20, summary.totalSeconds());
        assertEquals(20, summary.todaySeconds());
        assertEquals(20, summary.last7DaysSeconds());
        assertEquals(7, summary.daily().size());
        assertEquals(20, summary.daily().get(6).seconds());
        assertEquals(zone.id(), summary.topZones().get(0).zoneId());
        assertEquals(zone.name(), summary.topZones().get(0).name());

        mvc.perform(
                        get("/users/me/listening-summary")
                                .header(
                                        "Authorization",
                                        "Bearer " + tokenFor(listener.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalSeconds").value(20))
                .andExpect(jsonPath("$.data.daily.length()").value(7))
                .andExpect(jsonPath("$.data.topZones[0].zoneId").value(zone.id()));
        mvc.perform(get("/users/me/listening-summary")).andExpect(status().isUnauthorized());
    }

    @Test
    void emptyQueueImmediatelyWaivesRequesterCooldown() {
        var z = create();
        join(z.id(), listener.getId());
        queue.requestSong(z.id(), new SongRequest(songs.get(3).getId(), null), listener.getId());
        assertEquals(300, queue.cooldownRemainSeconds(z.id(), listener.getId()));

        clock.advance(40);
        playback.tick(z.id());
        assertNull(zones.getDetail(z.id(), host.getId()).nowPlaying());
        assertEquals(0, queue.cooldownRemainSeconds(z.id(), listener.getId()));

        var refill =
                queue.requestSong(
                        z.id(), new SongRequest(songs.get(0).getId(), null), listener.getId());
        assertEquals("PLAYING", refill.status());
    }

    @Test
    void guestCanUpgradeInPlaceAndLogBackIntoTheSameAccount() {
        var guest = sessions.guest();
        var registered = sessions.register("Demo_User", "secure-pass-123", guest.token());
        assertEquals(guest.userId(), registered.userId());
        assertFalse(registered.guest());
        assertFalse(sessions.current(registered.token()).guest());

        sessions.logout(registered.token());
        assertThrows(BizException.class, () -> sessions.authenticate(registered.token()));
        var loggedIn = sessions.login("demo_user", "secure-pass-123");
        assertEquals(guest.userId(), loggedIn.userId());
        assertFalse(loggedIn.guest());
        assertThrows(
                BizException.class, () -> sessions.login("demo_user", "wrong-password"));
    }

    @Test
    void expiredPresenceClosesDomainAndLegacyMembersReceiveGrace() {
        var z = create();
        clock.advance(91);
        playback.tick(z.id());
        assertEquals(ZoneStatus.ENDED, zoneRepo.findById(z.id()).orElseThrow().getStatus());
        var another = create();
        jdbc.update("UPDATE sz_zone_member SET last_seen_at=NULL WHERE zone_id=?", another.id());
        clock.advance(91);
        playback.tick(another.id());
        assertEquals(ZoneStatus.ACTIVE, zoneRepo.findById(another.id()).orElseThrow().getStatus());
    }

    @Test
    void residentDemoZoneKeepsMembersAndLoopsItsPlaylist() {
        var created = create();
        var zone = zoneRepo.findById(created.id()).orElseThrow();
        zone.setDemoResident(true);
        zoneRepo.saveAndFlush(zone);
        clock.advance(91);
        playback.tick(zone.getId());
        assertEquals(ZoneStatus.ACTIVE, zoneRepo.findById(zone.getId()).orElseThrow().getStatus());
        assertTrue(members.existsByZoneIdAndUserId(zone.getId(), host.getId()));
        clock.advance(40);
        playback.tick(zone.getId());
        playback.tick(zone.getId());
        assertNotNull(zones.getDetail(zone.getId(), host.getId()).nowPlaying());
    }

    @Test
    void residentDemoZoneAlsoEndsWhenMemberCountReachesZero() {
        var created = create();
        var zone = zoneRepo.findById(created.id()).orElseThrow();
        zone.setDemoResident(true);
        zoneRepo.saveAndFlush(zone);

        members.deleteAll(members.findByZoneId(zone.getId()));
        members.flush();
        playback.tick(zone.getId());

        assertEquals(ZoneStatus.ENDED, zoneRepo.findById(zone.getId()).orElseThrow().getStatus());
        assertEquals(0, zoneRepo.findById(zone.getId()).orElseThrow().getListenerCount());
    }

    @Test
    void privateDomainAndIdentityCannotBeBypassed() throws Exception {
        var z =
                zones.createZone(
                        request(
                                "PRIVATE",
                                "0707",
                                "BAN",
                                Set.of("电子"),
                                songs.subList(0, 3).stream().map(Track::getId).toList()),
                        host.getId());
        String guest = tokenFor(listener.getId()), owner = tokenFor(host.getId());
        mvc.perform(get("/tracks/search").param("keyword", "测试"))
                .andExpect(status().isUnauthorized());
        mvc.perform(
                        get("/tracks/search")
                                .param("keyword", "测试")
                                .header("Authorization", "Bearer " + guest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4));
        mvc.perform(get("/zones/" + z.id())).andExpect(status().isUnauthorized());
        mvc.perform(get("/zones/" + z.id()).header("Authorization", "Bearer " + guest))
                .andExpect(status().isForbidden());
        mvc.perform(get("/zones/" + z.id() + "/invite").header("Authorization", "Bearer " + guest))
                .andExpect(status().isForbidden());
        mvc.perform(get("/zones/" + z.id()).header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inviteCode").doesNotExist());
        mvc.perform(
                        post("/zones/" + z.id() + "/join")
                                .header("Authorization", "Bearer " + guest)
                                .contentType("application/json")
                                .content("{\"userId\":" + host.getId() + ",\"password\":\"bad\"}"))
                .andExpect(jsonPath("$.code").value(3007));
        String invite = zones.invite(z.id(), host.getId());
        zones.joinZone(z.id(), new JoinZoneRequest(host.getId(), null, invite), listener.getId());
        assertTrue(members.existsByZoneIdAndUserId(z.id(), listener.getId()));
        assertTrue(zones.listActive(null, null).isEmpty());
        assertThrows(
                BizException.class,
                () ->
                        queue.requestSong(
                                z.id(),
                                new SongRequest(songs.get(3).getId(), host.getId()),
                                stranger.getId()));
        assertNull(zoneRepo.findById(z.id()).orElseThrow().getPassword());
        assertNotNull(zoneRepo.findById(z.id()).orElseThrow().getPasswordHash());
    }

    @Test
    void picturesAppearImmediatelyAndWithdrawalRevokesExports() throws Exception {
        var z = create();
        join(z.id(), listener.getId());
        var req = new MomentCreateRequest(z.imageCandidate().itemId(), null, true);
        assertThrows(
                BizException.class, () -> moments.create(z.id(), listener.getId(), req, image()));
        var shared = share(z, true);
        assertEquals("APPROVED", shared.moderationStatus());
        assertEquals(z.id(), shared.zoneId());
        assertEquals(songs.get(2).getId(), shared.music().id());
        assertEquals(songs.get(2).getArtist(), shared.music().artist());
        assertEquals(shared.id(), moments.feed(z.id(), listener.getId()).get(0).id());
        assertEquals(shared.id(), moments.detail(shared.id(), listener.getId()).id());
        assertTrue(zones.getDetail(z.id(), listener.getId()).moments().isEmpty());
        clock.advance(10);
        playback.tick(z.id());
        assertTrue(zones.getDetail(z.id(), listener.getId()).moments().isEmpty());
        clock.advance(10);
        playback.tick(z.id());
        assertEquals(shared.queueItemId(), zones.getDetail(z.id(), listener.getId()).nowPlaying().itemId());
        assertEquals(shared.id(), zones.getDetail(z.id(), listener.getId()).moments().get(0).id());
        assertTrue(java.nio.file.Files.exists(moments.image(shared.id(), listener.getId())));
        assertThrows(BizException.class, () -> moments.create(z.id(), host.getId(), req, image()));
        assertEquals(
                songs.get(2).getTitle(), moments.feed(z.id(), listener.getId()).get(0).track());
        var batch = training.export();
        assertEquals(1, ((List<?>) batch.get("items")).size());
        var heart = moments.react(z.id(), shared.id(), listener.getId(), "HEART");
        assertEquals("HEART", heart.reaction());
        assertEquals(1, heart.heartCount());
        assertEquals(1, moments.feed(z.id(), host.getId()).get(0).heartCount());
        var unhearted = moments.react(z.id(), shared.id(), listener.getId(), null);
        assertNull(unhearted.reaction());
        assertEquals(0, unhearted.heartCount());
        assertThrows(
                BizException.class, () -> moments.withdraw(z.id(), shared.id(), listener.getId()));
        moments.withdraw(z.id(), shared.id(), host.getId());
        assertTrue(moments.feed(z.id(), listener.getId()).isEmpty());
        assertEquals(List.of(shared.id()), training.revocations((String) batch.get("batchId")));
        assertThrows(BizException.class, () -> training.image(shared.id()));
        assertThrows(BizException.class, () -> moments.image(shared.id(), host.getId()));
    }

    @Test
    void oldPendingPicturesBecomeVisibleWithoutReapprovingRejectedPictures() throws Exception {
        var z = create();
        join(z.id(), listener.getId());
        var shared = share(z, false);
        var record = momentRepo.findById(shared.id()).orElseThrow();
        record.setModerationStatus(ModerationStatus.PENDING);
        momentRepo.saveAndFlush(record);
        assertEquals(shared.id(), moments.feed(z.id(), listener.getId()).get(0).id());
        assertTrue(java.nio.file.Files.exists(moments.image(shared.id(), listener.getId())));
        assertEquals(
                "HEART", moments.react(z.id(), shared.id(), listener.getId(), "HEART").reaction());
        record.setModerationStatus(ModerationStatus.REJECTED);
        momentRepo.saveAndFlush(record);
        assertTrue(moments.feed(z.id(), listener.getId()).isEmpty());
        assertThrows(BizException.class, () -> moments.image(shared.id(), listener.getId()));
    }

    @Test
    void thirtyMinuteWindowConsentAndCollectionStatesPersist() throws Exception {
        var z = create();
        var resident = zoneRepo.findById(z.id()).orElseThrow();
        resident.setDemoResident(true);
        zoneRepo.saveAndFlush(resident);
        join(z.id(), listener.getId());
        var shared = share(z, false);
        assertTrue(((List<?>) training.export().get("items")).isEmpty());
        Long playing = z.nowPlaying().itemId();
        queue.like(z.id(), playing, listener.getId(), true);
        queue.like(z.id(), playing, listener.getId(), true);
        assertEquals(1, queueRepo.findById(playing).orElseThrow().getLikes());
        assertEquals(1, likeRepo.count());
        feedback.collect(z.id(), playing, listener.getId(), true);
        feedback.collect(z.id(), playing, listener.getId(), true);
        assertEquals(1, collections.count());
        assertTrue(zones.getDetail(z.id(), listener.getId()).nowPlaying().collected());
        feedback.collect(z.id(), playing, listener.getId(), false);
        assertEquals(0, collections.count());
        clock.advance(1801);
        assertTrue(moments.feed(z.id(), listener.getId()).isEmpty());
    }

    @Test
    void residentDemoPhotosRemainVisibleWithoutExtendingOrdinaryMoments() throws Exception {
        var z = create();
        join(z.id(), listener.getId());
        var resident = zoneRepo.findById(z.id()).orElseThrow();
        resident.setDemoResident(true);
        zoneRepo.saveAndFlush(resident);
        host.setHostSubject("demo:test-host");
        users.saveAndFlush(host);
        var shared = share(z, false);
        clock.advance(1801);
        assertEquals(shared.id(), moments.feed(z.id(), listener.getId()).get(0).id());
        assertTrue(Files.isRegularFile(imageStorage.resolve(
                imageStorage.ensureDemoMomentImage("night-host"))));
    }

    @Test
    void aNewLikeNotifiesOnlyTheSongUploaderOnce() {
        var z = create();
        join(z.id(), listener.getId());
        Long playing = z.nowPlaying().itemId();

        queue.like(z.id(), playing, listener.getId(), true);
        queue.like(z.id(), playing, listener.getId(), true);

        var glowEvents =
                applicationEvents.stream(ZoneEvent.class)
                        .filter(event -> "GLOW".equals(event.type()))
                        .toList();
        assertEquals(1, glowEvents.size());
        assertEquals(host.getId(), glowEvents.get(0).toUserId());
        assertEquals(playing, glowEvents.get(0).itemId());

        queue.like(z.id(), playing, listener.getId(), false);
        queue.like(z.id(), playing, host.getId(), true);
        assertEquals(
                1,
                applicationEvents.stream(ZoneEvent.class)
                        .filter(event -> "GLOW".equals(event.type()))
                        .count());
    }

    @Test
    void myUploadsAreRankedByLikesThenRecency() {
        var z = create();
        join(z.id(), listener.getId());
        join(z.id(), stranger.getId());
        Long first = z.nowPlaying().itemId();
        Long second = z.queue().get(0).itemId();
        Long third = z.queue().get(1).itemId();

        queue.like(z.id(), first, listener.getId(), true);
        queue.like(z.id(), second, listener.getId(), true);
        queue.like(z.id(), second, stranger.getId(), true);

        var uploads = userService.uploads(host.getId());
        var ranked =
                uploads.stream()
                        .map(row -> (QueueItemDTO) row.get("item"))
                        .toList();
        assertEquals(
                List.of(second, first, third),
                ranked.stream().map(QueueItemDTO::itemId).toList());
        assertEquals(List.of(2, 1, 0), ranked.stream().map(QueueItemDTO::likes).toList());
    }

    @Test
    void multipartFollowAndWithdrawalUseSessionIdentity() throws Exception {
        var z = create();
        String owner = tokenFor(host.getId());
        String result =
                mvc.perform(
                                multipart("/zones/" + z.id() + "/moments")
                                        .file(image())
                                        .param(
                                                "queueItemId",
                                                z.imageCandidate().itemId().toString())
                                        .param("trainingConsent", "false")
                                        .header("Authorization", "Bearer " + owner))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        long id = json.readTree(result).path("data").path("id").asLong();
        assertTrue(id > 0);
        mvc.perform(
                        post("/users/" + listener.getId() + "/follow")
                                .header("Authorization", "Bearer " + owner)
                                .contentType("application/json")
                                .content("{\"fromUserId\":" + stranger.getId() + "}"))
                .andExpect(status().isOk());
        assertEquals(1, userService.following(host.getId()).size());
        assertEquals(0, userService.following(stranger.getId()).size());
        mvc.perform(delete("/moments/" + id).header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk());
        mvc.perform(get("/internal/moments/pending")).andExpect(status().isNotFound());
        mvc.perform(post("/internal/moments/" + id + "/review"))
                .andExpect(status().isNotFound());
    }

    @Test
    void websocketGlowIsDeliveredOnlyToUploader() throws Exception {
        var z = create();
        join(z.id(), listener.getId());
        String owner = tokenFor(host.getId()), other = tokenFor(listener.getId());
        var ownerMessages = new LinkedBlockingQueue<String>();
        var otherMessages = new LinkedBlockingQueue<String>();
        WebSocket ws1 = connect(owner, z.id(), ownerMessages),
                ws2 = connect(other, z.id(), otherMessages);
        try {
            assertTrue(ownerMessages.poll(5, TimeUnit.SECONDS).contains("READY"));
            assertTrue(otherMessages.poll(5, TimeUnit.SECONDS).contains("READY"));
            feedback.collect(z.id(), z.nowPlaying().itemId(), listener.getId(), true);
            boolean glow = false;
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (System.nanoTime() < deadline && !glow) {
                String msg = ownerMessages.poll(200, TimeUnit.MILLISECONDS);
                glow = msg != null && msg.contains("GLOW");
            }
            assertTrue(glow);
            assertTrue(otherMessages.stream().noneMatch(m -> m.contains("GLOW")));
        } finally {
            ws1.sendClose(WebSocket.NORMAL_CLOSURE, "done").join();
            ws2.sendClose(WebSocket.NORMAL_CLOSURE, "done").join();
        }
    }

    @Test
    void directMessagesRequireAFollowRelationshipAndPersistForBothUsers() {
        assertThrows(
                BizException.class,
                () ->
                        directMessages.send(
                                listener.getId(),
                                host.getId(),
                                new DirectMessageRequest("hello")));

        userService.follow(listener.getId(), host.getId());
        var first =
                directMessages.send(
                        listener.getId(),
                        host.getId(),
                        new DirectMessageRequest("  在听同一首歌吗？  "));
        assertEquals(LocalDateTime.now(clock), first.createdAt());
        var reply =
                directMessages.send(
                        host.getId(),
                        listener.getId(),
                        new DirectMessageRequest("是的"));

        assertEquals("在听同一首歌吗？", first.body());
        assertEquals(host.getId(), reply.fromUserId());
        assertEquals(
                List.of(first.id(), reply.id()),
                directMessages.conversation(listener.getId(), host.getId()).stream()
                        .map(DirectMessageDTO::id)
                        .toList());

        userService.unfollow(listener.getId(), host.getId());
        assertEquals(2, directMessages.conversation(host.getId(), listener.getId()).size());
        assertThrows(
                BizException.class,
                () ->
                        directMessages.send(
                                host.getId(),
                                listener.getId(),
                                new DirectMessageRequest("不能继续发送")));
        assertThrows(
                BizException.class,
                () -> directMessages.conversation(host.getId(), stranger.getId()));
        assertEquals(2, directMessageRepo.count());
    }

    @Test
    void inAppNotificationsAggregateAndOpenFromTheirBusinessTargets() throws Exception {
        var z = create();
        join(z.id(), listener.getId());
        userService.follow(host.getId(), listener.getId());
        userService.follow(listener.getId(), host.getId());

        notificationService.invite(z.id(), host.getId(), listener.getId());
        directMessages.send(
                listener.getId(), host.getId(), new DirectMessageRequest("第一条提醒"));
        directMessages.send(
                listener.getId(), host.getId(), new DirectMessageRequest("第二条提醒"));
        queue.like(z.id(), z.nowPlaying().itemId(), listener.getId(), true);

        var hostNotifications = notificationService.list(host.getId());
        var message =
                hostNotifications.stream()
                        .filter(n -> n.type().equals(NotificationType.DIRECT_MESSAGE.name()))
                        .findFirst()
                        .orElseThrow();
        assertEquals(2, message.eventCount());
        assertEquals(listener.getId(), message.actorId());
        assertTrue(
                hostNotifications.stream()
                        .anyMatch(n -> n.type().equals(NotificationType.TRACK_STARTED.name())));
        assertTrue(
                hostNotifications.stream()
                        .anyMatch(n -> n.type().equals(NotificationType.TRACK_LIKE.name())));

        var invite = notificationService.list(listener.getId()).stream()
                .filter(n -> n.type().equals(NotificationType.ZONE_INVITE.name()))
                .findFirst()
                .orElseThrow();
        assertEquals(z.id(), invite.zoneId());

        String ownerToken = tokenFor(host.getId());
        mvc.perform(
                        get("/notifications/unread-count")
                                .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(3));
        mvc.perform(
                        put("/notifications/read-all")
                                .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());
        assertEquals(0, notificationService.unreadCount(host.getId()));
        assertTrue(notificationRepo.count() >= 4);
    }

    @Test
    void directMessageRestValidationAndRealtimeNotificationWork() throws Exception {
        userService.follow(listener.getId(), host.getId());
        String senderToken = tokenFor(listener.getId()), recipientToken = tokenFor(host.getId());
        var senderEvents = new LinkedBlockingQueue<String>();
        var recipientEvents = new LinkedBlockingQueue<String>();
        WebSocket senderSocket = connectMessages(senderToken, senderEvents);
        WebSocket recipientSocket = connectMessages(recipientToken, recipientEvents);
        try {
            assertTrue(senderEvents.poll(5, TimeUnit.SECONDS).contains("READY"));
            assertTrue(recipientEvents.poll(5, TimeUnit.SECONDS).contains("READY"));
            mvc.perform(
                            post("/messages/users/" + host.getId())
                                    .header("Authorization", "Bearer " + senderToken)
                                    .contentType("application/json")
                                    .content("{\"body\":\"\"}"))
                    .andExpect(jsonPath("$.code").value(1001));
            mvc.perform(
                            post("/messages/users/" + host.getId())
                                    .header("Authorization", "Bearer " + senderToken)
                                    .contentType("application/json")
                                    .content("{\"body\":\"demo message\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.body").value("demo message"));
            assertTrue(awaitEvent(senderEvents, "MESSAGE"));
            assertTrue(awaitEvent(recipientEvents, "MESSAGE"));
            mvc.perform(
                            get("/messages/users/" + listener.getId())
                                    .header("Authorization", "Bearer " + recipientToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].body").value("demo message"));
        } finally {
            senderSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done").join();
            recipientSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done").join();
        }
    }

    private boolean awaitEvent(BlockingQueue<String> events, String type) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            String message = events.poll(200, TimeUnit.MILLISECONDS);
            if (message != null && message.contains(type)) return true;
        }
        return false;
    }

    private WebSocket connectMessages(String token, BlockingQueue<String> messages)
            throws Exception {
        WebSocket socket = connectSocket("/api/ws/messages", messages);
        socket.sendText(json.writeValueAsString(Map.of("token", token)), true).join();
        return socket;
    }

    private WebSocket connect(String token, Long zoneId, BlockingQueue<String> messages)
            throws Exception {
        var ws = connectSocket("/api/ws/zones", messages);
        ws.sendText(json.writeValueAsString(Map.of("token", token, "zoneId", zoneId)), true).join();
        return ws;
    }

    private WebSocket connectSocket(String path, BlockingQueue<String> messages) throws Exception {
        return HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(
                        URI.create("ws://localhost:" + port + path),
                        new WebSocket.Listener() {
                            private final StringBuilder buffer = new StringBuilder();

                            public void onOpen(WebSocket socket) {
                                socket.request(1);
                            }

                            public CompletionStage<?> onText(
                                    WebSocket socket, CharSequence data, boolean last) {
                                buffer.append(data);
                                if (last) {
                                    messages.add(buffer.toString());
                                    buffer.setLength(0);
                                }
                                socket.request(1);
                                return null;
                            }
                        })
                .get(5, TimeUnit.SECONDS);
    }
}
