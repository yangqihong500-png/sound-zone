package com.soundzone.track.repository;

import com.soundzone.track.entity.Track;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrackRepository extends JpaRepository<Track, Long> {

    /** 点歌搜曲：歌名或歌手模糊匹配（忽略大小写） */
    List<Track> findTop20ByTitleContainingIgnoreCaseOrArtistContainingIgnoreCase(
            String title, String artist);

    List<Track> findTop50ByOrderByIdAsc();

    Optional<Track> findFirstBySourceAndExternalId(String source, String externalId);

    List<Track> findTop50BySourceOrderByIdAsc(String source);
}
