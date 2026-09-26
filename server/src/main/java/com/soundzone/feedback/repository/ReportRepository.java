package com.soundzone.feedback.repository;

import com.soundzone.feedback.entity.Report;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface ReportRepository extends JpaRepository<Report, Long> {}
