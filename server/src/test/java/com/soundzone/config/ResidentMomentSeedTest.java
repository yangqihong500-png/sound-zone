package com.soundzone.config;

import static org.junit.jupiter.api.Assertions.*;

import com.soundzone.moment.repository.MomentRepository;
import com.soundzone.moment.service.ImageStorage;
import com.soundzone.queue.entity.QueueItem;
import com.soundzone.queue.entity.QueueStatus;
import com.soundzone.queue.repository.QueueItemRepository;
import com.soundzone.zone.entity.Zone;
import com.soundzone.zone.repository.ZoneRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;

@SpringBootTest(properties = "soundzone.demo-data-enabled=true")
@ActiveProfiles("test")
class ResidentMomentSeedTest {
    @Autowired DataInitializer initializer;
    @Autowired ZoneRepository zones;
    @Autowired MomentRepository moments;
    @Autowired ImageStorage images;
    @Autowired QueueItemRepository queue;

    @Test
    @Transactional
    void seedsFiveSongBoundPhotosAcrossFourResidentZonesAndCanRunAgain() {
        assertEquals(4, zones.findAll().stream().filter(Zone::isDemoResident).count());
        assertEquals(5, moments.count());
        for (var moment : moments.findAll()) {
            assertNotNull(moment.getImageUrl());
            assertTrue(Files.isRegularFile(images.resolve(moment.getImageUrl())));
            assertEquals(moment.getUser().getId(), moment.getQueueItem().getRequester().getId());
            assertEquals(moment.getTrack().getId(), moment.getQueueItem().getTrack().getId());
        }
        var night = zones.findAll().stream()
                .filter(z -> "下班后的客厅".equals(z.getName()))
                .findFirst().orElseThrow();
        var guestMoment = moments.findAll().stream()
                .filter(m -> "我也到家了，今晚一起慢慢听。".equals(m.getText()))
                .findFirst().orElseThrow();
        var playlist = queue.findByZoneIdAndStatusIn(night.getId(),
                        List.of(QueueStatus.PLAYING, QueueStatus.QUEUED, QueueStatus.PLAYED))
                .stream()
                .sorted(Comparator.comparing(QueueItem::getCreatedAt)
                        .thenComparing(q -> q.getId()))
                .toList();
        assertEquals(guestMoment.getQueueItem().getId(), playlist.get(1).getId());
        assertEquals(3, playlist.subList(0, 3).stream()
                .map(q -> q.getRequester().getId()).distinct().count());
        initializer.run();
        assertEquals(5, moments.count());
        assertEquals(playlist.size(), queue.findByZoneIdAndStatusIn(night.getId(),
                List.of(QueueStatus.PLAYING, QueueStatus.QUEUED, QueueStatus.PLAYED)).size());
    }

    @Test
    @Transactional
    void migratesAnOlderHostUploadAndRemovesTheExtraDemoCopy() {
        var night = zones.findAll().stream()
                .filter(z -> "下班后的客厅".equals(z.getName()))
                .findFirst().orElseThrow();
        var guestMoment = moments.findFirstByZoneIdAndText(night.getId(),
                "我也到家了，今晚一起慢慢听。").orElseThrow();
        QueueItem original = guestMoment.getQueueItem();
        original.setRequester(night.getHost());
        queue.saveAndFlush(original);
        QueueItem extra = new QueueItem();
        extra.setZone(night);
        extra.setTrack(guestMoment.getTrack());
        extra.setRequester(guestMoment.getUser());
        extra.setStatus(QueueStatus.QUEUED);
        extra = queue.saveAndFlush(extra);

        initializer.run();

        assertEquals(guestMoment.getUser().getId(), original.getRequester().getId());
        assertEquals(QueueStatus.REMOVED, extra.getStatus());
        assertEquals(original.getId(), guestMoment.getQueueItem().getId());
        assertEquals(5, moments.count());
    }
}
