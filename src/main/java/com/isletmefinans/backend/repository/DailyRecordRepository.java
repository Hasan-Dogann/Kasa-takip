package com.isletmefinans.backend.repository;

import com.isletmefinans.backend.entity.DailyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyRecordRepository extends JpaRepository<DailyRecord, Long> {

    boolean existsByRecordDate(LocalDate recordDate);

    boolean existsByRecordDateAndIdNot(LocalDate recordDate, Long id);

    Optional<DailyRecord> findById(Long id);

    Optional<DailyRecord> findByRecordDate(LocalDate recordDate);

    List<DailyRecord> findByRecordDateBetweenOrderByRecordDateAsc(LocalDate startDate, LocalDate endDate);
}
