package com.isletmefinans.backend.service;

import com.isletmefinans.backend.dto.DailyRecordResponse;
import com.isletmefinans.backend.dto.DailyRecordUpsertRequest;
import com.isletmefinans.backend.dto.ExpenseEntryRequest;
import com.isletmefinans.backend.dto.IncomeEntryRequest;
import com.isletmefinans.backend.entity.DailyRecord;
import com.isletmefinans.backend.entity.ExpenseCategory;
import com.isletmefinans.backend.repository.DailyRecordRepository;
import com.isletmefinans.backend.repository.IncomeEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyRecordServiceTest {

    @Mock
    private DailyRecordRepository dailyRecordRepository;

    @Mock
    private IncomeEntryRepository incomeEntryRepository;

    private DailyRecordService dailyRecordService;

    @BeforeEach
    void setUp() {
        dailyRecordService = new DailyRecordService(dailyRecordRepository, incomeEntryRepository);
    }

    @Test
    void shouldCreateDailyRecordWithCategoryExpenses() {
        LocalDate recordDate = LocalDate.of(2026, 6, 29);
        DailyRecordUpsertRequest request = new DailyRecordUpsertRequest(
                recordDate,
                java.util.List.of(
                        new IncomeEntryRequest(new BigDecimal("15000"), "Nakit satis"),
                        new IncomeEntryRequest(new BigDecimal("5000"), "Paket servis")
                ),
                java.util.List.of(
                        new ExpenseEntryRequest(ExpenseCategory.TAVUK, new BigDecimal("4000"), "Gunluk alim"),
                        new ExpenseEntryRequest(ExpenseCategory.PERSONEL, new BigDecimal("3000"), "Vardiya")
                )
        );

        AtomicReference<DailyRecord> savedRecordRef = new AtomicReference<>();

        when(dailyRecordRepository.existsByRecordDate(recordDate)).thenReturn(false);
        when(dailyRecordRepository.save(any(DailyRecord.class))).thenAnswer(invocation -> {
            DailyRecord record = invocation.getArgument(0);
            record.setId(15L);
            savedRecordRef.set(record);
            return record;
        });
        when(dailyRecordRepository.findById(15L)).thenAnswer(invocation -> Optional.of(savedRecordRef.get()));

        DailyRecordResponse response = dailyRecordService.createRecord(request);

        assertThat(response.totalIncome()).isEqualByComparingTo("20000.00");
        assertThat(response.totalExpense()).isEqualByComparingTo("7000.00");
        assertThat(response.remainingBalance()).isEqualByComparingTo("13000.00");
        assertThat(response.incomeItems()).hasSize(2);
        assertThat(response.expenseItems()).hasSize(2);
        assertThat(response.expenseItems())
                .extracting(item -> item.category())
                .containsExactly("tavuk", "personel");
    }
}
