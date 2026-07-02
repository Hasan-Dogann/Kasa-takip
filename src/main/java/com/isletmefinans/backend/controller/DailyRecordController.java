package com.isletmefinans.backend.controller;

import com.isletmefinans.backend.dto.DailyRecordResponse;
import com.isletmefinans.backend.dto.DailyRecordUpsertRequest;
import com.isletmefinans.backend.service.DailyRecordService;
import com.isletmefinans.backend.util.YearMonthParser;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/daily-records")
public class DailyRecordController {

    private final DailyRecordService dailyRecordService;

    public DailyRecordController(DailyRecordService dailyRecordService) {
        this.dailyRecordService = dailyRecordService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DailyRecordResponse createRecord(@Valid @RequestBody DailyRecordUpsertRequest request) {
        return dailyRecordService.createRecord(request);
    }

    @PutMapping("/{id}")
    public DailyRecordResponse updateRecord(@PathVariable Long id, @Valid @RequestBody DailyRecordUpsertRequest request) {
        return dailyRecordService.updateRecord(id, request);
    }

    @GetMapping("/{id}")
    public DailyRecordResponse getRecordById(@PathVariable Long id) {
        return dailyRecordService.getRecordById(id);
    }

    @GetMapping("/by-date")
    public DailyRecordResponse getRecordByDate(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        return dailyRecordService.getRecordByDate(date);
    }

    @GetMapping("/by-month")
    public List<DailyRecordResponse> getRecordsByMonth(@RequestParam String month) {
        return dailyRecordService.listRecordsByMonth(YearMonthParser.parse(month));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRecord(@PathVariable Long id) {
        dailyRecordService.deleteRecord(id);
    }
}

