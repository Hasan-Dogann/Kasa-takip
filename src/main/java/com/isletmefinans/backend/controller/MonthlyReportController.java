package com.isletmefinans.backend.controller;

import com.isletmefinans.backend.dto.MonthlyReportResponse;
import com.isletmefinans.backend.service.MonthlyReportService;
import com.isletmefinans.backend.util.YearMonthParser;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.YearMonth;

@RestController
@RequestMapping("/api/monthly-reports")
public class MonthlyReportController {

    private final MonthlyReportService monthlyReportService;

    public MonthlyReportController(MonthlyReportService monthlyReportService) {
        this.monthlyReportService = monthlyReportService;
    }

    @GetMapping
    public MonthlyReportResponse getMonthlyReport(@RequestParam String month) {
        return monthlyReportService.getMonthlyReport(YearMonthParser.parse(month));
    }

    @GetMapping(value = "/export", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> exportMonthlyReport(@RequestParam String month) {
        YearMonth parsedMonth = YearMonthParser.parse(month);
        String reportContent = monthlyReportService.exportMonthlyReport(parsedMonth);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("aylik-rapor-" + parsedMonth + ".txt", StandardCharsets.UTF_8)
                        .build()
        );

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .body(reportContent);
    }
}
