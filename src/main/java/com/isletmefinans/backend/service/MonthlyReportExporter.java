package com.isletmefinans.backend.service;

import com.isletmefinans.backend.dto.MonthlyExpenseCategoryResponse;
import com.isletmefinans.backend.dto.MonthlyReportDayResponse;
import com.isletmefinans.backend.dto.MonthlyReportResponse;
import com.isletmefinans.backend.dto.MonthlyReportSummaryResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MonthlyReportExporter {

    public String export(MonthlyReportResponse report) {
        StringBuilder builder = new StringBuilder();
        MonthlyReportSummaryResponse summary = report.summary();

        builder.append("AYLIK KASA RAPORU").append(System.lineSeparator());
        builder.append("Ay: ").append(summary.month()).append(System.lineSeparator());
        builder.append("Toplam Gelir: ").append(format(summary.totalIncome())).append(System.lineSeparator());
        builder.append("Toplam Gider: ").append(format(summary.totalExpense())).append(System.lineSeparator());
        builder.append("Aylik Kalan: ").append(format(summary.totalRemaining())).append(System.lineSeparator());
        builder.append("Kayitli Gun: ").append(summary.recordCount()).append(System.lineSeparator());
        builder.append(System.lineSeparator());
        builder.append("GIDER KATEGORI DETAYI").append(System.lineSeparator());

        for (MonthlyExpenseCategoryResponse category : report.expenseByCategory()) {
            builder.append(category.categoryLabel())
                    .append(": ")
                    .append(format(category.totalExpense()))
                    .append(System.lineSeparator());
        }

        builder.append(System.lineSeparator());
        builder.append("GUNLUK DETAYLAR").append(System.lineSeparator());

        for (MonthlyReportDayResponse day : report.days()) {
            builder.append(day.recordDate())
                    .append(" | kayit=")
                    .append(day.hasRecord() ? "VAR" : "YOK")
                    .append(" | gelir=")
                    .append(format(day.totalIncome()))
                    .append(" | gider=")
                    .append(format(day.totalExpense()))
                    .append(" | kalan=")
                    .append(format(day.remainingBalance()))
                    .append(System.lineSeparator());
        }

        return builder.toString();
    }

    private String format(BigDecimal amount) {
        return amount == null ? "0.00 TL" : amount.toPlainString() + " TL";
    }
}
