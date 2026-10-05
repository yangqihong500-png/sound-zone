package com.soundzone.config;

import static org.junit.jupiter.api.Assertions.*;

import com.soundzone.moment.repository.MomentRepository;
import com.soundzone.moment.service.ImageStorage;
import com.soundzone.queue.repository.QueueItemRepository;
import com.soundzone.zone.entity.Zone;
import com.soundzone.zone.repository.ZoneRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;

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
        var hostPlaylist = queue.findByZoneIdAndRequesterIdOrderByCreatedAtAscIdAsc(
                night.getId(), night.getHost().getId());
        assertEquals(hostPlaylist.get(1).getTrack().getId(), guestMoment.getTrack().getId());
        initializer.run();
        assertEquals(5, moments.count());
    }
}
