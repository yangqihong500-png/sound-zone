package com.soundzone;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.soundzone.auth.service.*;
import com.soundzone.common.*;
import com.soundzone.feedback.repository.*;
import com.soundzone.feedback.service.FeedbackService;
import com.soundzone.moment.dto.*;
import com.soundzone.moment.entity.*;
import com.soundzone.moment.repository.*;
import com.soundzone.moment.service.*;
import com.soundzone.queue.dto.*;
import com.soundzone.queue.entity.*;
import com.soundzone.queue.repository.*;
import com.soundzone.queue.service.*;
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
import org.springframework.test.web.servlet.MockMvc;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.*;
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
class CoreWorkflowTest {
    @Autowired ZoneService zones;
    @Autowired ZoneRepository zoneRepo;
    @Autowired ZoneMemberRepository members;
    @Autowired QueueService queue;
    @Autowired QueueItemRepository queueRepo;
    @Autowired QueueLikeRepository likeRepo;
    @Autowired PlaybackService playback;
    @Autowired MomentService moments;
    @Autowired MomentRepository momentRepo;
    @Autowired TrainingExportService training;
    @Autowired FeedbackService feedback;
    @Autowired TrackCollectionRepository collections;
    @Autowired TrackRepository tracks;
    @Autowired TrackService trackService;
    @Autowired UserRepository users;
    @Autowired UserService userService;
    @Autowired SessionService sessions;
    @Autowired MutableClock clock;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
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
                        "sz_follow",
                        "sz_auth_session",
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
        var ids = List.of(songs.get(2).getId(), songs.get(0).getId(), songs.get(1).getId());
        var zone = zones.createZone(request("PUBLIC", null, "BAN", Set.of("电子"), ids), host.getId());
        assertEquals(ids.get(0), zone.nowPlaying().trackId());
        assertEquals(ids.subList(1, 3), zone.queue().stream().map(QueueItemDTO::trackId).toList());
        assertEquals(host.getId(), zone.hostId());
        assertEquals(600, queue.cooldownRemainSeconds(zone.id(), host.getId()));
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
            assertEquals(600, queue.cooldownRemainSeconds(z.id(), listener.getId()));
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
        assertEquals(shared.id(), moments.feed(z.id(), listener.getId()).get(0).id());
        assertEquals(shared.id(), zones.getDetail(z.id(), listener.getId()).moments().get(0).id());
        assertTrue(java.nio.file.Files.exists(moments.image(shared.id(), listener.getId())));
        assertThrows(BizException.class, () -> moments.create(z.id(), host.getId(), req, image()));
        assertEquals(
                songs.get(2).getTitle(), moments.feed(z.id(), listener.getId()).get(0).track());
        var batch = training.export();
        assertEquals(1, ((List<?>) batch.get("items")).size());
        assertEquals("HEART", moments.react(z.id(), shared.id(), listener.getId(), "HEART"));
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
        assertEquals("HEART", moments.react(z.id(), shared.id(), listener.getId(), "HEART"));
        record.setModerationStatus(ModerationStatus.REJECTED);
        momentRepo.saveAndFlush(record);
        assertTrue(moments.feed(z.id(), listener.getId()).isEmpty());
        assertThrows(BizException.class, () -> moments.image(shared.id(), listener.getId()));
    }

    @Test
    void thirtyMinuteWindowConsentAndCollectionStatesPersist() throws Exception {
        var z = create();
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

    private WebSocket connect(String token, Long zoneId, BlockingQueue<String> messages)
            throws Exception {
        var ws =
                HttpClient.newHttpClient()
                        .newWebSocketBuilder()
                        .buildAsync(
                                URI.create("ws://localhost:" + port + "/api/ws/zones"),
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
        ws.sendText(json.writeValueAsString(Map.of("token", token, "zoneId", zoneId)), true).join();
        return ws;
    }
}
