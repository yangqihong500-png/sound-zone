package com.soundzone.config;

import com.soundzone.moment.entity.Moment;
import com.soundzone.moment.repository.MomentRepository;
import com.soundzone.queue.entity.QueueItem;
import com.soundzone.queue.entity.QueueStatus;
import com.soundzone.queue.repository.QueueItemRepository;
import com.soundzone.track.entity.Track;
import com.soundzone.track.repository.TrackRepository;
import com.soundzone.user.entity.User;
import com.soundzone.user.repository.UserRepository;
import com.soundzone.zone.entity.FilterMode;
import com.soundzone.zone.entity.PeriodType;
import com.soundzone.zone.entity.Zone;
import com.soundzone.zone.entity.ZoneMember;
import com.soundzone.zone.entity.ZonePeriod;
import com.soundzone.zone.entity.ZoneStatus;
import com.soundzone.zone.entity.ZoneVisibility;
import com.soundzone.zone.repository.ZoneMemberRepository;
import com.soundzone.zone.repository.ZonePeriodRepository;
import com.soundzone.zone.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Demo 种子数据（v2：2026-09-24 会议后同步）
 * 覆盖：公开域 × 2（黑名单/白名单两种过滤模式）+ 私密域 × 1（含密码与邀请码）
 * 番茄钟、图片分享（绑定上传者歌曲）、成员与同频人数
 * 仅当数据库为空时初始化（H2 每次启动都是空库）
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TrackRepository trackRepository;
    private final ZoneRepository zoneRepository;
    private final ZonePeriodRepository periodRepository;
    private final ZoneMemberRepository memberRepository;
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

        // ---------- 曲目（标签覆盖五类：语言/年代/风格/场景/情绪，决议 D5）----------
        Track lemon = track("Lemon", "米津玄師", "#A8B8C8", Set.of("日语", "10s", "流行", "舒缓"));
        Track kiseki = track("キセキ", "GReeeeN", "#B5C4D4", Set.of("日语", "00s", "流行", "治愈"));
        Track yoru = track("夜に駆ける", "YOASOBI", "#C3B8D9", Set.of("日语", "20s", "流行", "抖音热曲"));
        Track river = track("River Flows in You", "Yiruma", "#C9D4C5", Set.of("纯音乐", "00s", "古典", "舒缓", "自习"));
        Track midnightCity = track("Midnight City", "M83", "#D9C3B8", Set.of("英语", "10s", "电子", "亢奋", "健身"));
        Track blinding = track("Blinding Lights", "The Weeknd", "#E3C9D4", Set.of("英语", "20s", "电子", "流行", "健身"));
        Track plasticLove = track("Plastic Love", "竹内まりや", "#B8CDD9", Set.of("日语", "80s", "City Pop", "旅行"));
        Track stayWithMe = track("真夜中のドア", "松原みき", "#D9CFB8", Set.of("日语", "70s", "City Pop", "深夜"));

        // ---------- 域 1：考研自习室（公开 + BAN 黑名单 + 番茄钟，完整机制演示）----------
        Zone study = zone("考研自习室", "自习", host1, "#A8B8C8", 87,
                ZoneVisibility.PUBLIC, FilterMode.BAN,
                Set.of("舒缓", "专注"), Set.of("抖音热曲", "亢奋"));
        period(study, 0, 40, PeriodType.FOCUS, Set.of("舒缓", "专注", "纯音乐"));
        period(study, 1, 15, PeriodType.BREAK, Set.of("流行", "舒缓"));
        member(study, host1);
        member(study, demo);
        QueueItem playing1 = queueItem(study, lemon, demo, QueueStatus.PLAYING, 0);
        playing1.setStartedAt(LocalDateTime.now().minusMinutes(3));
        queueItemRepository.save(playing1);
        queueItem(study, kiseki, host1, QueueStatus.QUEUED, 24);
        queueItem(study, yoru, demo, QueueStatus.QUEUED, 19);
        queueItem(study, river, host1, QueueStatus.QUEUED, 15);
        // 图片分享：demo 绑定自己上传的 Lemon（决议 D6 绑定规则）
        moment(study, demo, null, "#B5C4D4", lemon, LocalDateTime.now().minusMinutes(12));
        moment(study, host1, "今天也是满座", "#C9D4C5", kiseki, LocalDateTime.now().minusMinutes(5));

        // ---------- 域 2：夜跑俱乐部（公开 + ALLOW 白名单模式演示）----------
        Zone run = zone("夜跑俱乐部", "健身", host2, "#D9C3B8", 45,
                ZoneVisibility.PUBLIC, FilterMode.ALLOW,
                Set.of("电子", "亢奋"), Set.of("电子", "亢奋", "健身"));
        member(run, host2);
        QueueItem playing2 = queueItem(run, midnightCity, host2, QueueStatus.PLAYING, 0);
        playing2.setStartedAt(LocalDateTime.now().minusMinutes(1));
        queueItemRepository.save(playing2);
        queueItem(run, blinding, host2, QueueStatus.QUEUED, 21);
        moment(run, host2, "珠江边 5km 打卡", "#D9C3B8", midnightCity, LocalDateTime.now().minusMinutes(8));

        // ---------- 域 3：京都深夜（私密域：密码 0707 + 邀请码 kyoto88）----------
        Zone trip = zone("京都深夜", "旅行", host3, "#B8CDD9", 12,
                ZoneVisibility.PRIVATE, FilterMode.BAN,
                Set.of("City Pop", "日语"), Set.of("抖音热曲"));
        trip.setPassword("0707");
        trip.setInviteCode("kyoto88");
        zoneRepository.save(trip);
        member(trip, host3);
        QueueItem playing3 = queueItem(trip, plasticLove, host3, QueueStatus.PLAYING, 0);
        playing3.setStartedAt(LocalDateTime.now().minusMinutes(2));
        queueItemRepository.save(playing3);
        queueItem(trip, stayWithMe, host3, QueueStatus.QUEUED, 28);
        moment(trip, host3, "鸭川的黄昏", "#B8CDD9", plasticLove, LocalDateTime.now().minusMinutes(3));
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

    private Zone zone(String name, String scene, User host, String color, int listeners,
                      ZoneVisibility visibility, FilterMode filterMode,
                      Set<String> tags, Set<String> filterTags) {
        Zone z = new Zone();
        z.setName(name);
        z.setScene(scene);
        z.setHost(host);
        z.setCoverColor(color);
        z.setListenerCount(listeners);
        z.setVisibility(visibility);
        z.setFilterMode(filterMode);
        z.setTags(tags);
        z.setFilterTags(filterTags);
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

    private void member(Zone zone, User user) {
        ZoneMember m = new ZoneMember();
        m.setZone(zone);
        m.setUser(user);
        memberRepository.save(m);
    }

    private QueueItem queueItem(Zone zone, Track track, User requester, QueueStatus status, int likes) {
        QueueItem q = new QueueItem();
        q.setZone(zone);
        q.setTrack(track);
        q.setRequester(requester);
        q.setStatus(status);
        q.setLikes(likes);
        return queueItemRepository.save(q);
    }

    private void moment(Zone zone, User user, String text, String color, Track track, LocalDateTime createdAt) {
        Moment m = new Moment();
        m.setZone(zone);
        m.setUser(user);
        m.setText(text);
        m.setColor(color);
        m.setTrack(track);
        m.setCreatedAt(createdAt);
        momentRepository.save(m);
    }
}
