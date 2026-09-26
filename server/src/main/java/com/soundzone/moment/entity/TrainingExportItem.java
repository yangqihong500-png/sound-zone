package com.soundzone.moment.entity;

import jakarta.persistence.*;

import lombok.Data;

import java.time.LocalDateTime;

/** 导出清单留痕，使图片撤回后可向已导出的批次发出剔除清单。 */
@Data
@Entity
@Table(
        name = "sz_training_export_item",
        uniqueConstraints = @UniqueConstraint(columnNames = {"batch_id", "moment_id"}))
public class TrainingExportItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36)
    private String batchId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "moment_id")
    private Moment moment;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
