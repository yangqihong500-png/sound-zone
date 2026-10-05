package com.soundzone.config;

import static org.junit.jupiter.api.Assertions.*;

import com.soundzone.moment.repository.MomentRepository;
import com.soundzone.moment.service.ImageStorage;
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
        initializer.run();
        assertEquals(5, moments.count());
    }
}
