package com.soundzone.config;

import com.soundzone.queue.entity.QueueItem;
import com.soundzone.queue.entity.QueueStatus;
import com.soundzone.queue.repository.QueueItemRepository;
import com.soundzone.moment.entity.Moment;
import com.soundzone.moment.repository.MomentRepository;
import com.soundzone.track.entity.Track;
import com.soundzone.track.repository.TrackRepository;
import com.soundzone.user.entity.User;
import com.soundzone.user.repository.UserRepository;
import com.soundzone.zone.entity.PeriodType;
import com.soundzone.zone.entity.Zone;
import com.soundzone.zone.entity.ZonePeriod;
import com.soundzone.zone.entity.ZoneStatus;
import com.soundzone.zone.repository.ZonePeriodRepository;
import com.soundzone.zone.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Demo 种子数据：与前端 app/src/api/mock.js 保持一致
 * 仅当数据库为空时初始化（H2 每次启动都是空库）
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TrackRepository trackRepository;
    private final ZoneRepository zoneRepository;
    private final ZonePeriodRepository periodRepository;
    private final QueueItemRepository queueItemRepository;
    private final MomentRepository momentRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) return;

        // ---------- 用户 ----------
        User host1 = user("白桃乌龙");
        User host2 = user("配速430");
        User host3 = user("京都慢一点");
        User demo = user("octave"); // Demo 当前用户
        demo.setTasteScore(86.0);
        userRepository.save(demo);

        // ---------- 曲目（含曲风标签，docs/02 决议 D1 的判定基础）----------
        Track lemon = track("Lemon", "米津玄師", "#9FE1CB", Set.of("流行", "舒缓"));
        Track kiseki = track("キセキ", "GReeeeN", "#B5D4F4", Set.of("流行", "日系"));
        Track yoru = track("夜に駆ける", "YOASOBI", "#AFA9EC", Set.of("流行", "日系", "抖音热曲"));
        Track river = track("River Flows in You", "Yiruma", "#C0DD97", Set.of("舒缓", "纯音乐"));
        Track midnightCity = track("Midnight City", "M83", "#F0997B", Set.of("电子", "节奏"));
        Track blinding = track("Blinding Lights", "The Weeknd", "#F4C0D1", Set.of("电子", "流行"));
        Track plasticLove = track("Plastic Love", "竹内まりや", "#85B7EB", Set.of("City Pop", "日系"));
        Track stayWithMe = track("真夜中のドア", "松原みき", "#FAC775", Set.of("City Pop", "日系"));

        // ---------- 域 1：考研自习室（含黑名单 + 番茄钟，完整机制演示）----------
        Zone study = zone("考研自习室", "自习", host1, "#9FE1CB", 87,
                Set.of("舒缓", "纯音乐"), Set.of("抖音热曲", "情歌"));
        // 番茄钟：40 分钟专注（仅舒缓/纯音乐）+ 15 分钟休息（流行可放）—— docs/02 决议 D3
        period(study, 0, 40, PeriodType.FOCUS, Set.of("舒缓", "纯音乐"));
        period(study, 1, 15, PeriodType.BREAK, Set.of("流行", "舒缓", "纯音乐"));
        QueueItem playing1 = queueItem(study, lemon, demo, QueueStatus.PLAYING, 0, 0.0);
        playing1.setStartedAt(LocalDateTime.now().minusMinutes(3));
        queueItemRepository.save(playing1);
        queueItem(study, kiseki, host1, QueueStatus.QUEUED, 24, 24 * 2 + 3);
        queueItem(study, yoru, demo, QueueStatus.QUEUED, 19, 19 * 2);
        queueItem(study, river, host1, QueueStatus.QUEUED, 15, 15 * 2 + 3);
        moment(study, host1, "图书馆 19:20", null, "#B5D4F4", lemon);
        moment(study, demo, "刷完这套题就睡", null, "#F5C4B3", kiseki);

        // ---------- 域 2：夜跑俱乐部 ----------
        Zone run = zone("夜跑俱乐部", "健身", host2, "#F0997B", 45,
                Set.of("电子", "节奏"), Set.of("舒缓"));
        QueueItem playing2 = queueItem(run, midnightCity, host2, QueueStatus.PLAYING, 0, 0.0);
        playing2.setStartedAt(LocalDateTime.now().minusMinutes(1));
        queueItemRepository.save(playing2);
        queueItem(run, blinding, host2, QueueStatus.QUEUED, 21, 21 * 2 + 3);
        moment(run, host2, "珠江边 5km 打卡", null, "#F0997B", midnightCity);

        // ---------- 域 3：日本 solo trip ----------
        Zone trip = zone("日本 solo trip", "旅行", host3, "#85B7EB", 62,
                Set.of("City Pop", "日系"), Set.of("抖音热曲"));
        QueueItem playing3 = queueItem(trip, plasticLove, host3, QueueStatus.PLAYING, 0, 0.0);
        playing3.setStartedAt(LocalDateTime.now().minusMinutes(2));
        queueItemRepository.save(playing3);
        queueItem(trip, stayWithMe, host3, QueueStatus.QUEUED, 28, 28 * 2 + 3);
        moment(trip, host3, "鸭川的黄昏", null, "#85B7EB", plasticLove);
    }

    // ---------- 构造辅助 ----------

    private User user(String name) {
        User u = new User();
        u.setName(name);
        return userRepository.save(u);
    }

    private Track track(String title, String artist, String color, Set<String> tags) {
        Track t = new Track();
        t.setTitle(title);
        t.setArtist(artist);
        t.setCoverColor(color);
        t.setTags(tags);
        return trackRepository.save(t);
    }

    private Zone zone(String name, String scene, User host, String color,
                      int listeners, Set<String> tags, Set<String> banned) {
        Zone z = new Zone();
        z.setName(name);
        z.setScene(scene);
        z.setHost(host);
        z.setCoverColor(color);
        z.setListenerCount(listeners);
        z.setTags(tags);
        z.setBannedTags(banned);
        z.setStatus(ZoneStatus.ACTIVE);
        return zoneRepository.save(z);
    }

    private void period(Zone zone, int order, int duration, PeriodType type, Set<String> allowed) {
        ZonePeriod p = new ZonePeriod();
        p.setZone(zone);
        p.setOrderIndex(order);
        p.setDurationMin(duration);
        p.setType(type);
        p.setAllowedTags(allowed);
        periodRepository.save(p);
    }

    private QueueItem queueItem(Zone zone, Track track, User requester,
                                QueueStatus status, int likes, double score) {
        QueueItem q = new QueueItem();
        q.setZone(zone);
        q.setTrack(track);
        q.setRequester(requester);
        q.setStatus(status);
        q.setLikes(likes);
        q.setScore(score);
        q.setHostBonus(zone.getHost().getId().equals(requester.getId()) ? 1 : 0);
        return queueItemRepository.save(q);
    }

    private void moment(Zone zone, User user, String text, String imageUrl, String color, Track track) {
        Moment m = new Moment();
        m.setZone(zone);
        m.setUser(user);
        m.setText(text);
        m.setImageUrl(imageUrl);
        m.setColor(color);
        m.setTrack(track);
        momentRepository.save(m);
    }
}
