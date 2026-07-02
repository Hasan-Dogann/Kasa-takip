package com.isletmefinans.backend.service;

import com.isletmefinans.backend.dto.CreditCustomerCreateRequest;
import com.isletmefinans.backend.dto.CreditCustomerDetailResponse;
import com.isletmefinans.backend.dto.CreditCustomerSummaryResponse;
import com.isletmefinans.backend.dto.CreditCustomerUpdateRequest;
import com.isletmefinans.backend.dto.CreditDebtRequest;
import com.isletmefinans.backend.dto.CreditPaymentRequest;
import com.isletmefinans.backend.dto.CreditTransactionResponse;
import com.isletmefinans.backend.dto.CreditTransactionUpdateRequest;
import com.isletmefinans.backend.entity.CreditCustomer;
import com.isletmefinans.backend.entity.CreditTransaction;
import com.isletmefinans.backend.entity.CreditTransactionType;
import com.isletmefinans.backend.exception.BusinessValidationException;
import com.isletmefinans.backend.exception.ResourceNotFoundException;
import com.isletmefinans.backend.repository.CreditCustomerRepository;
import com.isletmefinans.backend.repository.CreditTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.isletmefinans.backend.util.AmountUtils.ZERO;
import static com.isletmefinans.backend.util.AmountUtils.normalize;

@Service
public class CreditCustomerService {

    private final CreditCustomerRepository creditCustomerRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final DailyRecordService dailyRecordService;

    public CreditCustomerService(
            CreditCustomerRepository creditCustomerRepository,
            CreditTransactionRepository creditTransactionRepository,
            DailyRecordService dailyRecordService
    ) {
        this.creditCustomerRepository = creditCustomerRepository;
        this.creditTransactionRepository = creditTransactionRepository;
        this.dailyRecordService = dailyRecordService;
    }

    @Transactional(readOnly = true)
    public List<CreditCustomerSummaryResponse> listActiveCustomers() {
        return creditCustomerRepository.findByArchivedFalseOrderByUpdatedAtDesc().stream()
                .map(CreditCustomerSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CreditCustomerSummaryResponse> listArchivedCustomers() {
        return creditCustomerRepository.findByArchivedTrueOrderByArchivedAtDesc().stream()
                .map(CreditCustomerSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CreditCustomerDetailResponse getCustomerDetail(Long customerId) {
        return toDetail(getExistingCustomer(customerId));
    }

    @Transactional
    public CreditCustomerDetailResponse createCustomerWithDebt(CreditCustomerCreateRequest request) {
        CreditCustomer customer = creditCustomerRepository.findByPhoneNumberIgnoreCase(normalizePhone(request.phoneNumber()))
                .orElseGet(CreditCustomer::new);

        customer.setFullName(trimToNull(request.fullName()));
        customer.setPhoneNumber(normalizePhone(request.phoneNumber()));
        if (customer.getTotalDebt() == null) {
            customer.setTotalDebt(ZERO);
            customer.setTotalPaid(ZERO);
            customer.setRemainingBalance(ZERO);
            customer.setArchived(false);
            customer.setArchivedAt(null);
        }

        customer.addTransaction(buildTransaction(CreditTransactionType.DEBT, request.amount(), request.description(), request.occurredAt()));
        refreshCustomerTotals(customer);

        return toDetail(creditCustomerRepository.saveAndFlush(customer));
    }

    @Transactional
    public CreditCustomerDetailResponse updateCustomer(Long customerId, CreditCustomerUpdateRequest request) {
        CreditCustomer customer = getExistingCustomer(customerId);
        String normalizedPhone = normalizePhone(request.phoneNumber());
        creditCustomerRepository.findByPhoneNumberIgnoreCase(normalizedPhone)
                .filter(other -> !other.getId().equals(customerId))
                .ifPresent(other -> {
                    throw new BusinessValidationException("Bu telefon numarasi baska bir veresiye kaydinda kullaniliyor");
                });

        customer.setFullName(trimToNull(request.fullName()));
        customer.setPhoneNumber(normalizedPhone);
        return toDetail(creditCustomerRepository.save(customer));
    }

    @Transactional
    public CreditCustomerDetailResponse addDebt(Long customerId, CreditDebtRequest request) {
        CreditCustomer customer = getExistingCustomer(customerId);
        customer.addTransaction(buildTransaction(CreditTransactionType.DEBT, request.amount(), request.description(), request.occurredAt()));
        refreshCustomerTotals(customer);
        return toDetail(creditCustomerRepository.saveAndFlush(customer));
    }

    @Transactional
    public CreditCustomerDetailResponse recordPayment(Long customerId, CreditPaymentRequest request) {
        CreditCustomer customer = getExistingCustomer(customerId);
        BigDecimal amount = normalize(request.amount());

        if (amount.compareTo(customer.getRemainingBalance()) > 0) {
            throw new BusinessValidationException("Tahsilat kalan borctan fazla olamaz");
        }

        CreditTransaction payment = buildTransaction(
                CreditTransactionType.PAYMENT,
                amount,
                request.description(),
                request.occurredAt()
        );
        customer.addTransaction(payment);
        creditCustomerRepository.saveAndFlush(customer);

        if (payment.getId() == null) {
            throw new IllegalStateException("Odeme hareketi kimligi olusturulamadi");
        }

        dailyRecordService.addOrUpdateCreditPaymentIncome(
                payment.getId(),
                customer.getFullName(),
                amount,
                payment.getDescription(),
                payment.getOccurredAt().toLocalDate()
        );

        refreshCustomerTotals(customer);
        return toDetail(creditCustomerRepository.saveAndFlush(customer));
    }

    @Transactional
    public CreditCustomerDetailResponse updateTransaction(Long transactionId, CreditTransactionUpdateRequest request) {
        CreditTransaction transaction = creditTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Hareket bulunamadi: " + transactionId));
        CreditCustomer customer = transaction.getCustomer();

        transaction.setAmount(normalize(request.amount()));
        transaction.setDescription(trimToNull(request.description()));
        transaction.setOccurredAt(resolveOccurredAt(request.occurredAt()));

        if (transaction.getTransactionType() == CreditTransactionType.PAYMENT) {
            BigDecimal maxAllowed = calculateMaximumPaymentFor(customer, transaction.getId());
            if (transaction.getAmount().compareTo(maxAllowed) > 0) {
                throw new BusinessValidationException("Odeme tutari kalan borctan fazla olamaz");
            }

            dailyRecordService.addOrUpdateCreditPaymentIncome(
                    transaction.getId(),
                    customer.getFullName(),
                    transaction.getAmount(),
                    transaction.getDescription(),
                    transaction.getOccurredAt().toLocalDate()
            );
        }

        refreshCustomerTotals(customer);
        return toDetail(creditCustomerRepository.saveAndFlush(customer));
    }

    @Transactional
    public void deleteCustomer(Long customerId) {
        CreditCustomer customer = getExistingCustomer(customerId);

        customer.getTransactions().stream()
                .filter(transaction -> transaction.getTransactionType() == CreditTransactionType.PAYMENT)
                .map(CreditTransaction::getId)
                .filter(transactionId -> transactionId != null)
                .forEach(dailyRecordService::removeCreditPaymentIncome);

        creditCustomerRepository.delete(customer);
    }

    @Transactional
    public void deleteTransaction(Long transactionId) {
        CreditTransaction transaction = creditTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Hareket bulunamadi: " + transactionId));
        CreditCustomer customer = transaction.getCustomer();

        if (transaction.getTransactionType() == CreditTransactionType.PAYMENT) {
            dailyRecordService.removeCreditPaymentIncome(transaction.getId());
        }

        customer.getTransactions().removeIf(item -> item.getId() != null && item.getId().equals(transactionId));

        if (customer.getTransactions().isEmpty()) {
            creditCustomerRepository.delete(customer);
            return;
        }

        refreshCustomerTotals(customer);
        creditCustomerRepository.saveAndFlush(customer);
    }

    private BigDecimal calculateMaximumPaymentFor(CreditCustomer customer, Long editedTransactionId) {
        BigDecimal debtTotal = customer.getTransactions().stream()
                .filter(transaction -> transaction.getTransactionType() == CreditTransactionType.DEBT)
                .map(CreditTransaction::getAmount)
                .reduce(ZERO, BigDecimal::add);
        BigDecimal paidExceptCurrent = customer.getTransactions().stream()
                .filter(transaction -> transaction.getTransactionType() == CreditTransactionType.PAYMENT)
                .filter(transaction -> !transaction.getId().equals(editedTransactionId))
                .map(CreditTransaction::getAmount)
                .reduce(ZERO, BigDecimal::add);

        return normalize(debtTotal.subtract(paidExceptCurrent));
    }

    private CreditTransaction buildTransaction(
            CreditTransactionType type,
            BigDecimal amount,
            String description,
            LocalDateTime occurredAt
    ) {
        CreditTransaction transaction = new CreditTransaction();
        transaction.setTransactionType(type);
        transaction.setAmount(normalize(amount));
        transaction.setDescription(trimToNull(description));
        transaction.setOccurredAt(resolveOccurredAt(occurredAt));
        return transaction;
    }

    private void refreshCustomerTotals(CreditCustomer customer) {
        BigDecimal totalDebt = customer.getTransactions().stream()
                .filter(transaction -> transaction.getTransactionType() == CreditTransactionType.DEBT)
                .map(CreditTransaction::getAmount)
                .reduce(ZERO, BigDecimal::add);
        BigDecimal totalPaid = customer.getTransactions().stream()
                .filter(transaction -> transaction.getTransactionType() == CreditTransactionType.PAYMENT)
                .map(CreditTransaction::getAmount)
                .reduce(ZERO, BigDecimal::add);
        BigDecimal remainingBalance = normalize(totalDebt.subtract(totalPaid));

        if (remainingBalance.compareTo(ZERO) < 0) {
            throw new BusinessValidationException("Tahsilatlar toplam borctan fazla olamaz");
        }

        customer.setTotalDebt(normalize(totalDebt));
        customer.setTotalPaid(normalize(totalPaid));
        customer.setRemainingBalance(remainingBalance);

        if (remainingBalance.compareTo(ZERO) == 0 && totalDebt.compareTo(ZERO) > 0) {
            customer.setArchived(true);
            if (customer.getArchivedAt() == null) {
                customer.setArchivedAt(LocalDateTime.now());
            }
        } else {
            customer.setArchived(false);
            customer.setArchivedAt(null);
        }
    }

    private CreditCustomerDetailResponse toDetail(CreditCustomer customer) {
        List<CreditTransaction> orderedTransactions = customer.getTransactions().stream()
                .sorted(Comparator.comparing(CreditTransaction::getOccurredAt).thenComparing(CreditTransaction::getId))
                .toList();

        BigDecimal runningBalance = ZERO;
        List<CreditTransactionResponse> transactionResponses = new ArrayList<>();

        for (CreditTransaction transaction : orderedTransactions) {
            if (transaction.getTransactionType() == CreditTransactionType.DEBT) {
                runningBalance = normalize(runningBalance.add(transaction.getAmount()));
            } else {
                runningBalance = normalize(runningBalance.subtract(transaction.getAmount()));
            }

            transactionResponses.add(CreditTransactionResponse.from(transaction, runningBalance));
        }

        return new CreditCustomerDetailResponse(
                customer.getId(),
                customer.getFullName(),
                customer.getPhoneNumber(),
                customer.getTotalDebt(),
                customer.getTotalPaid(),
                customer.getRemainingBalance(),
                customer.isArchived(),
                customer.getArchivedAt(),
                List.copyOf(transactionResponses)
        );
    }

    private CreditCustomer getExistingCustomer(Long customerId) {
        return creditCustomerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Veresiye kaydi bulunamadi: " + customerId));
    }

    private LocalDateTime resolveOccurredAt(LocalDateTime occurredAt) {
        return occurredAt != null ? occurredAt : LocalDateTime.now();
    }

    private String normalizePhone(String phoneNumber) {
        String normalizedPhone = trimToNull(phoneNumber);

        if (normalizedPhone == null) {
            throw new BusinessValidationException("Telefon numarasi zorunludur");
        }

        return normalizedPhone;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
