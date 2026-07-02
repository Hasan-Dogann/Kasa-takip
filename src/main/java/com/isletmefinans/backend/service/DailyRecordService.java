package com.isletmefinans.backend.service;

import com.isletmefinans.backend.dto.DailyRecordResponse;
import com.isletmefinans.backend.dto.DailyRecordUpsertRequest;
import com.isletmefinans.backend.dto.ExpenseEntryRequest;
import com.isletmefinans.backend.dto.IncomeEntryRequest;
import com.isletmefinans.backend.entity.DailyRecord;
import com.isletmefinans.backend.entity.ExpenseEntry;
import com.isletmefinans.backend.entity.IncomeEntry;
import com.isletmefinans.backend.entity.IncomeEntrySourceType;
import com.isletmefinans.backend.exception.BusinessValidationException;
import com.isletmefinans.backend.exception.ResourceNotFoundException;
import com.isletmefinans.backend.repository.DailyRecordRepository;
import com.isletmefinans.backend.repository.IncomeEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static com.isletmefinans.backend.util.AmountUtils.ZERO;
import static com.isletmefinans.backend.util.AmountUtils.normalize;

@Service
public class DailyRecordService {

    private final DailyRecordRepository dailyRecordRepository;
    private final IncomeEntryRepository incomeEntryRepository;

    public DailyRecordService(DailyRecordRepository dailyRecordRepository, IncomeEntryRepository incomeEntryRepository) {
        this.dailyRecordRepository = dailyRecordRepository;
        this.incomeEntryRepository = incomeEntryRepository;
    }

    @Transactional
    public DailyRecordResponse createRecord(DailyRecordUpsertRequest request) {
        validateCreateRequest(request);
        validateDuplicateDateForCreate(request.recordDate());

        DailyRecord record = new DailyRecord();
        applyRequest(record, request);
        DailyRecord savedRecord = dailyRecordRepository.save(record);

        return getRecordById(savedRecord.getId());
    }

    @Transactional
    public DailyRecordResponse updateRecord(Long id, DailyRecordUpsertRequest request) {
        DailyRecord existingRecord = getExistingRecord(id);
        validateDuplicateDateForUpdate(request.recordDate(), id);
        validateDateMove(existingRecord, request.recordDate());

        applyRequest(existingRecord, request);
        validateRecordHasAnyEntry(existingRecord);
        dailyRecordRepository.save(existingRecord);

        return getRecordById(id);
    }

    @Transactional(readOnly = true)
    public DailyRecordResponse getRecordById(Long id) {
        return DailyRecordResponse.from(getExistingRecord(id));
    }

    @Transactional(readOnly = true)
    public DailyRecordResponse getRecordByDate(LocalDate date) {
        return dailyRecordRepository.findByRecordDate(date)
                .map(DailyRecordResponse::from)
                .orElse(DailyRecordResponse.empty(date));
    }

    @Transactional(readOnly = true)
    public List<DailyRecordResponse> listRecordsByMonth(YearMonth month) {
        List<DailyRecord> records = dailyRecordRepository.findByRecordDateBetweenOrderByRecordDateAsc(
                month.atDay(1),
                month.atEndOfMonth()
        );

        return records.stream()
                .map(DailyRecordResponse::from)
                .toList();
    }

    @Transactional
    public void deleteRecord(Long id) {
        DailyRecord record = getExistingRecord(id);

        boolean hasSystemIncome = record.getIncomeEntries().stream()
                .anyMatch(entry -> resolveSourceType(entry) != IncomeEntrySourceType.MANUAL);

        if (hasSystemIncome) {
            throw new BusinessValidationException("Veresiyeden gelen tahsilat bulunan gunler silinemez");
        }

        dailyRecordRepository.delete(record);
    }

    @Transactional
    public void addOrUpdateCreditPaymentIncome(
            Long transactionId,
            String customerName,
            BigDecimal amount,
            String description,
            LocalDate paymentDate
    ) {
        if (transactionId == null) {
            throw new BusinessValidationException("Veresiye tahsilati icin gecerli hareket kimligi gereklidir");
        }

        IncomeEntry incomeEntry = incomeEntryRepository
                .findBySourceTypeAndSourceReferenceId(IncomeEntrySourceType.CREDIT_PAYMENT, transactionId)
                .orElse(null);

        DailyRecord previousRecord = incomeEntry != null ? incomeEntry.getDailyRecord() : null;
        DailyRecord targetRecord = getOrCreateRecord(paymentDate);

        if (incomeEntry == null) {
            incomeEntry = new IncomeEntry();
            incomeEntry.setSourceType(IncomeEntrySourceType.CREDIT_PAYMENT);
            incomeEntry.setSourceReferenceId(transactionId);
        } else if (previousRecord != null && !previousRecord.getRecordDate().equals(paymentDate)) {
            previousRecord.getIncomeEntries().remove(incomeEntry);
            recalculateTotals(previousRecord);
            cleanupIfEmpty(previousRecord);
        }

        incomeEntry.setAmount(normalize(amount));
        incomeEntry.setDescription(buildCreditPaymentDescription(customerName, description));

        if (!targetRecord.getIncomeEntries().contains(incomeEntry)) {
            targetRecord.addIncomeEntry(incomeEntry);
        }

        recalculateTotals(targetRecord);
        dailyRecordRepository.save(targetRecord);
    }

    @Transactional
    public void removeCreditPaymentIncome(Long transactionId) {
        List<IncomeEntry> incomeEntries = new ArrayList<>(incomeEntryRepository.findAllBySourceReferenceId(transactionId));

        if (incomeEntries.isEmpty()) {
            incomeEntryRepository
                    .findBySourceTypeAndSourceReferenceId(IncomeEntrySourceType.CREDIT_PAYMENT, transactionId)
                    .ifPresent(incomeEntries::add);
        }

        if (incomeEntries.isEmpty()) {
            return;
        }

        for (IncomeEntry incomeEntry : incomeEntries) {
            DailyRecord record = incomeEntry.getDailyRecord();

            if (record == null) {
                incomeEntryRepository.delete(incomeEntry);
                continue;
            }

            record.getIncomeEntries().removeIf(entry -> entry.getId() != null && entry.getId().equals(incomeEntry.getId()));
            recalculateTotals(record);
            cleanupIfEmpty(record);
        }
    }

    private void validateDuplicateDateForCreate(LocalDate recordDate) {
        if (dailyRecordRepository.existsByRecordDate(recordDate)) {
            throw new BusinessValidationException("Bu tarih icin zaten bir kayit var");
        }
    }

    private void validateDuplicateDateForUpdate(LocalDate recordDate, Long id) {
        if (dailyRecordRepository.existsByRecordDateAndIdNot(recordDate, id)) {
            throw new BusinessValidationException("Bu tarih baska bir kayitta kullaniliyor");
        }
    }

    private DailyRecord getExistingRecord(Long id) {
        return dailyRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kayit bulunamadi: " + id));
    }

    private void applyRequest(DailyRecord record, DailyRecordUpsertRequest request) {
        List<IncomeEntry> manualIncomeEntries = request.incomeItems().stream()
                .map(this::toIncomeEntry)
                .toList();
        List<ExpenseEntry> expenseEntries = request.expenseItems().stream()
                .map(this::toExpenseEntry)
                .toList();
        List<IncomeEntry> preservedSystemIncomeEntries = record.getIncomeEntries().stream()
                .filter(entry -> resolveSourceType(entry) != IncomeEntrySourceType.MANUAL)
                .toList();

        record.setRecordDate(request.recordDate());
        record.replaceIncomeEntries(new ArrayList<>(preservedSystemIncomeEntries));

        for (IncomeEntry manualIncomeEntry : manualIncomeEntries) {
            record.addIncomeEntry(manualIncomeEntry);
        }

        record.replaceExpenseEntries(expenseEntries);
        recalculateTotals(record);
    }

    private void validateCreateRequest(DailyRecordUpsertRequest request) {
        if (request.incomeItems() == null || request.expenseItems() == null) {
            throw new BusinessValidationException("Gelir ve gider listeleri gonderilmelidir");
        }

        if (request.incomeItems().isEmpty() && request.expenseItems().isEmpty()) {
            throw new BusinessValidationException("En az bir gelir veya gider girilmelidir");
        }
    }

    private void validateDateMove(DailyRecord record, LocalDate newDate) {
        boolean hasSystemIncome = record.getIncomeEntries().stream()
                .anyMatch(entry -> resolveSourceType(entry) != IncomeEntrySourceType.MANUAL);

        if (hasSystemIncome && !record.getRecordDate().equals(newDate)) {
            throw new BusinessValidationException("Veresiye tahsilati olan gunun tarihi degistirilemez");
        }
    }

    private void validateRecordHasAnyEntry(DailyRecord record) {
        if (record.getIncomeEntries().isEmpty() && record.getExpenseEntries().isEmpty()) {
            throw new BusinessValidationException("Kayitta en az bir gelir veya gider bulunmalidir");
        }
    }

    private DailyRecord getOrCreateRecord(LocalDate paymentDate) {
        return dailyRecordRepository.findByRecordDate(paymentDate)
                .orElseGet(() -> {
                    DailyRecord record = new DailyRecord();
                    record.setRecordDate(paymentDate);
                    record.setTotalIncome(ZERO);
                    record.setTotalExpense(ZERO);
                    record.setRemainingBalance(ZERO);
                    return record;
                });
    }

    private void recalculateTotals(DailyRecord record) {
        BigDecimal totalIncome = sumIncomeEntries(record.getIncomeEntries());
        BigDecimal totalExpense = sumExpenseEntries(record.getExpenseEntries());
        record.setTotalIncome(totalIncome);
        record.setTotalExpense(totalExpense);
        record.setRemainingBalance(normalize(totalIncome.subtract(totalExpense)));
    }

    private void cleanupIfEmpty(DailyRecord record) {
        if (record.getIncomeEntries().isEmpty() && record.getExpenseEntries().isEmpty()) {
            if (record.getId() != null) {
                dailyRecordRepository.delete(record);
            }
            return;
        }

        dailyRecordRepository.save(record);
    }

    private BigDecimal sumIncomeEntries(List<IncomeEntry> entries) {
        return entries.stream()
                .map(IncomeEntry::getAmount)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumExpenseEntries(List<ExpenseEntry> entries) {
        return entries.stream()
                .map(ExpenseEntry::getAmount)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private IncomeEntry toIncomeEntry(IncomeEntryRequest request) {
        IncomeEntry entry = new IncomeEntry();
        entry.setAmount(normalize(request.amount()));
        entry.setDescription(trimToNull(request.description()));
        entry.setSourceType(IncomeEntrySourceType.MANUAL);
        entry.setSourceReferenceId(null);
        return entry;
    }

    private ExpenseEntry toExpenseEntry(ExpenseEntryRequest request) {
        ExpenseEntry entry = new ExpenseEntry();
        entry.setCategory(request.category());
        entry.setAmount(normalize(request.amount()));
        entry.setDescription(trimToNull(request.description()));
        return entry;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }

    private String buildCreditPaymentDescription(String customerName, String description) {
        String baseDescription = "Veresiyeden tahsilat - " + customerName;
        String paymentDescription = trimToNull(description);
        return paymentDescription == null ? baseDescription : baseDescription + " - " + paymentDescription;
    }

    private IncomeEntrySourceType resolveSourceType(IncomeEntry entry) {
        return entry.getSourceType() != null ? entry.getSourceType() : IncomeEntrySourceType.MANUAL;
    }
}
