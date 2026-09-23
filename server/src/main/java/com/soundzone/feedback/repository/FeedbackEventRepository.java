package com.soundzone.feedback.repository;

import com.soundzone.feedback.entity.FeedbackEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackEventRepository extends JpaRepository<FeedbackEvent, Long> {

    /** 域后战报聚合的原始事件（docs/02 决议 D2） */
    List<FeedbackEvent> findByZoneId(Long zoneId);

    /** 某用户在域内收到的反馈数（特效次数） */
    long countByZoneIdAndToUserId(Long zoneId, Long toUserId);
}
