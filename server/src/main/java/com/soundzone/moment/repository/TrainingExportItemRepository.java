package com.soundzone.moment.repository;

import com.soundzone.moment.entity.TrainingExportItem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrainingExportItemRepository extends JpaRepository<TrainingExportItem, Long> {
    List<TrainingExportItem> findByBatchId(String batchId);
}
