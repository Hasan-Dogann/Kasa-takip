package com.isletmefinans.backend.service;

import com.isletmefinans.backend.dto.MonthlyReportResponse;
import com.isletmefinans.backend.entity.DailyRecord;
import com.isletmefinans.backend.entity.ExpenseCategory;
import com.isletmefinans.backend.entity.ExpenseEntry;
import com.isletmefinans.backend.entity.IncomeEntry;
import com.isletmefinans.backend.entity.IncomeEntrySourceType;
import com.isletmefinans.backend.repository.DailyRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonthlyReportServiceTest {

    @Mock
    private DailyRecordRepository dailyRecordRepository;

    private MonthlyReportService monthlyReportService;

    @BeforeEach
    void setUp() {
        monthlyReportService = new MonthlyReportService(dailyRecordRepository, new MonthlyReportExporter());
    }

    @Test
    void shouldBuildMonthlyReportWithCategoryBreakdown() {
        YearMonth month = YearMonth.of(2026, 6);
        DailyRecord firstDay = createRecord(
                LocalDate.of(2026, 6, 1),
                List.of(new BigDecimal("10000.00")),
                List.of(
                        expense(ExpenseCategory.TAVUK, "3000.00"),
                        expense(ExpenseCategory.MANAV, "2000.00")
                )
        );
        DailyRecord thirdDay = createRecord(
                LocalDate.of(2026, 6, 3),
                List.of(new BigDecimal("4000.00")),
                List.of(expense(ExpenseCategory.PERSONEL, "1000.00"))
        );

        when(dailyRecordRepository.findByRecordDateBetweenOrderByRecordDateAsc(month.atDay(1), month.atEndOfMonth()))
                .thenReturn(List.of(firstDay, thirdDay));

        MonthlyReportResponse report = monthlyReportService.getMonthlyReport(month);

        assertThat(report.summary().totalIncome()).isEqualByComparingTo("14000.00");
        assertThat(report.summary().totalExpense()).isEqualByComparingTo("6000.00");
        assertThat(report.summary().totalRemaining()).isEqualByComparingTo("8000.00");
        assertThat(report.summary().recordCount()).isEqualTo(2);
        assertThat(report.days()).hasSize(30);
        assertThat(report.days().get(0).hasRecord()).isTrue();
        assertThat(report.days().get(1).hasRecord()).isFalse();
        assertThat(report.expenseByCategory())
                .filteredOn(item -> item.category().equals("tavuk"))
                .singleElement()
                .extracting(item -> item.totalExpense())
                .isEqualTo(new BigDecimal("3000.00"));
        assertThat(report.expenseByCategory())
                .filteredOn(item -> item.category().equals("manav"))
                .singleElement()
                .extracting(item -> item.totalExpense())
                .isEqualTo(new BigDecimal("2000.00"));
        assertThat(report.expenseByCategory())
                .filteredOn(item -> item.category().equals("personel"))
                .singleElement()
                .extracting(item -> item.totalExpense())
                .isEqualTo(new BigDecimal("1000.00"));
    }

    private DailyRecord createRecord(LocalDate date, List<BigDecimal> incomeValues, List<ExpenseEntry> expenseEntries) {
        DailyRecord record = new DailyRecord();
        record.setId(Math.abs(date.hashCode()) + 0L);
        record.setRecordDate(date);
        record.replaceIncomeEntries(incomeValues.stream().map(this::income).toList());
        record.replaceExpenseEntries(expenseEntries);

        BigDecimal totalIncome = incomeValues.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalExpense = expenseEntries.stream()
                .map(ExpenseEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        record.setTotalIncome(totalIncome);
        record.setTotalExpense(totalExpense);
        record.setRemainingBalance(totalIncome.subtract(totalExpense).setScale(2, RoundingMode.HALF_UP));
        return record;
    }

    private IncomeEntry income(BigDecimal amount) {
        IncomeEntry entry = new IncomeEntry();
        entry.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        entry.setDescription(null);
        entry.setSourceType(IncomeEntrySourceType.MANUAL);
        return entry;
    }

    private ExpenseEntry expense(ExpenseCategory category, String amount) {
        ExpenseEntry entry = new ExpenseEntry();
        entry.setCategory(category);
        entry.setAmount(new BigDecimal(amount));
        entry.setDescription(null);
        return entry;
    }
}
