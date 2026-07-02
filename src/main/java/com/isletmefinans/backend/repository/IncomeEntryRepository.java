package com.isletmefinans.backend.repository;

import com.isletmefinans.backend.entity.IncomeEntry;
import com.isletmefinans.backend.entity.IncomeEntrySourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IncomeEntryRepository extends JpaRepository<IncomeEntry, Long> {

    Optional<IncomeEntry> findBySourceTypeAndSourceReferenceId(IncomeEntrySourceType sourceType, Long sourceReferenceId);

    List<IncomeEntry> findAllBySourceReferenceId(Long sourceReferenceId);
}
