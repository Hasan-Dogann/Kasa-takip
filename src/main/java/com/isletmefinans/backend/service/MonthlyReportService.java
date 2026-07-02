package com.isletmefinans.backend.service;

import com.isletmefinans.backend.dto.MonthlyExpenseCategoryResponse;
import com.isletmefinans.backend.dto.MonthlyReportDayResponse;
import com.isletmefinans.backend.dto.MonthlyReportResponse;
import com.isletmefinans.backend.dto.MonthlyReportSummaryResponse;
import com.isletmefinans.backend.entity.DailyRecord;
import com.isletmefinans.backend.entity.ExpenseCategory;
import com.isletmefinans.backend.entity.ExpenseEntry;
import com.isletmefinans.backend.repository.DailyRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.isletmefinans.backend.util.AmountUtils.ZERO;
import static com.isletmefinans.backend.util.AmountUtils.normalize;

@Service
public class MonthlyReportService {

    private final DailyRecordRepository dailyRecordRepository;
    private final MonthlyReportExporter monthlyReportExporter;

    public MonthlyReportService(DailyRecordRepository dailyRecordRepository, MonthlyReportExporter monthlyReportExporter) {
        this.dailyRecordRepository = dailyRecordRepository;
        this.monthlyReportExporter = monthlyReportExporter;
    }

    @Transactional(readOnly = true)
    public MonthlyReportResponse getMonthlyReport(YearMonth month) {
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();

        List<DailyRecord> records = dailyRecordRepository.findByRecordDateBetweenOrderByRecordDateAsc(startDate, endDate);
        Map<LocalDate, DailyRecord> recordsByDate = records.stream()
                .collect(Collectors.toMap(DailyRecord::getRecordDate, Function.identity()));

        BigDecimal totalIncome = ZERO;
        BigDecimal totalExpense = ZERO;
        EnumMap<ExpenseCategory, BigDecimal> expenseTotals = new EnumMap<>(ExpenseCategory.class);

        for (ExpenseCategory category : ExpenseCategory.values()) {
            expenseTotals.put(category, ZERO);
        }

        List<MonthlyReportDayResponse> days = new ArrayList<>();

        for (DailyRecord record : records) {
            totalIncome = normalize(totalIncome.add(record.getTotalIncome()));
            totalExpense = normalize(totalExpense.add(record.getTotalExpense()));

            for (ExpenseEntry entry : record.getExpenseEntries()) {
                BigDecimal nextValue = normalize(expenseTotals.get(entry.getCategory()).add(entry.getAmount()));
                expenseTotals.put(entry.getCategory(), nextValue);
            }
        }

        for (int dayNumber = 1; dayNumber <= month.lengthOfMonth(); dayNumber++) {
            LocalDate currentDate = month.atDay(dayNumber);
            DailyRecord record = recordsByDate.get(currentDate);

            if (record == null) {
                days.add(MonthlyReportDayResponse.empty(currentDate));
                continue;
            }

            days.add(MonthlyReportDayResponse.from(record));
        }

        MonthlyReportSummaryResponse summary = new MonthlyReportSummaryResponse(
                month.toString(),
                totalIncome,
                totalExpense,
                normalize(totalIncome.subtract(totalExpense)),
                records.size()
        );

        List<MonthlyExpenseCategoryResponse> expenseByCategory = Arrays.stream(ExpenseCategory.values())
                .map(category -> new MonthlyExpenseCategoryResponse(
                        category.getValue(),
                        category.getLabel(),
                        expenseTotals.get(category)
                ))
                .toList();

        return new MonthlyReportResponse(summary, expenseByCategory, List.copyOf(days));
    }

    @Transactional(readOnly = true)
    public String exportMonthlyReport(YearMonth month) {
        return monthlyReportExporter.export(getMonthlyReport(month));
    }
}
