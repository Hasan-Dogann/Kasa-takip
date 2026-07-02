package com.isletmefinans.backend.service;

import com.isletmefinans.backend.dto.CreditCustomerCreateRequest;
import com.isletmefinans.backend.dto.CreditCustomerDetailResponse;
import com.isletmefinans.backend.dto.CreditPaymentRequest;
import com.isletmefinans.backend.entity.CreditCustomer;
import com.isletmefinans.backend.repository.CreditCustomerRepository;
import com.isletmefinans.backend.repository.CreditTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditCustomerServiceTest {

    @Mock
    private CreditCustomerRepository creditCustomerRepository;

    @Mock
    private CreditTransactionRepository creditTransactionRepository;

    @Mock
    private DailyRecordService dailyRecordService;

    private CreditCustomerService creditCustomerService;

    @BeforeEach
    void setUp() {
        creditCustomerService = new CreditCustomerService(
                creditCustomerRepository,
                creditTransactionRepository,
                dailyRecordService
        );
    }

    @Test
    void shouldCreateCustomerAndArchiveAfterFullPayment() {
        CreditCustomerCreateRequest createRequest = new CreditCustomerCreateRequest(
                "Hasan Dogan",
                "05000000000",
                new BigDecimal("10000"),
                "Adana ve corba",
                LocalDateTime.of(2026, 7, 2, 12, 0)
        );

        AtomicReference<CreditCustomer> customerRef = new AtomicReference<>();

        when(creditCustomerRepository.findByPhoneNumberIgnoreCase("05000000000")).thenReturn(Optional.empty());
        when(creditCustomerRepository.saveAndFlush(any(CreditCustomer.class))).thenAnswer(invocation -> {
            CreditCustomer customer = invocation.getArgument(0);
            if (customer.getId() == null) {
                customer.setId(11L);
            }

            long nextId = 100L;
            for (var transaction : customer.getTransactions()) {
                if (transaction.getId() == null) {
                    transaction.setId(++nextId);
                }
            }

            customerRef.set(customer);
            return customer;
        });

        CreditCustomerDetailResponse created = creditCustomerService.createCustomerWithDebt(createRequest);
        when(creditCustomerRepository.findById(11L)).thenReturn(Optional.of(customerRef.get()));
        doNothing().when(dailyRecordService).addOrUpdateCreditPaymentIncome(any(), any(), any(), any(), any());

        CreditCustomerDetailResponse paid = creditCustomerService.recordPayment(
                11L,
                new CreditPaymentRequest(
                        new BigDecimal("10000"),
                        "Tam tahsilat",
                        LocalDateTime.of(2026, 7, 2, 18, 30)
                )
        );

        assertThat(created.remainingBalance()).isEqualByComparingTo("10000.00");
        assertThat(paid.remainingBalance()).isEqualByComparingTo("0.00");
        assertThat(paid.archived()).isTrue();
        assertThat(paid.transactions()).hasSize(2);
        assertThat(paid.transactions().get(1).runningBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldDeleteCustomerAndRemoveCreditPaymentIncome() {
        CreditCustomer customer = new CreditCustomer();
        customer.setId(44L);
        customer.setFullName("Firat Kizil");
        customer.setPhoneNumber("05310000000");
        customer.setTotalDebt(new BigDecimal("1200.00"));
        customer.setTotalPaid(new BigDecimal("700.00"));
        customer.setRemainingBalance(new BigDecimal("500.00"));

        var debt = new com.isletmefinans.backend.entity.CreditTransaction();
        debt.setId(301L);
        debt.setTransactionType(com.isletmefinans.backend.entity.CreditTransactionType.DEBT);
        debt.setAmount(new BigDecimal("1200.00"));
        debt.setOccurredAt(LocalDateTime.of(2026, 7, 1, 20, 0));

        var payment = new com.isletmefinans.backend.entity.CreditTransaction();
        payment.setId(302L);
        payment.setTransactionType(com.isletmefinans.backend.entity.CreditTransactionType.PAYMENT);
        payment.setAmount(new BigDecimal("700.00"));
        payment.setOccurredAt(LocalDateTime.of(2026, 7, 2, 11, 30));

        customer.addTransaction(debt);
        customer.addTransaction(payment);

        when(creditCustomerRepository.findById(44L)).thenReturn(Optional.of(customer));

        creditCustomerService.deleteCustomer(44L);

        verify(dailyRecordService).removeCreditPaymentIncome(302L);
        verify(creditCustomerRepository).delete(customer);
    }

    @Test
    void shouldDeletePaymentTransactionAndRemoveLinkedIncome() {
        CreditCustomer customer = new CreditCustomer();
        customer.setId(55L);
        customer.setFullName("Hasan Dogan");
        customer.setPhoneNumber("05320000000");
        customer.setTotalDebt(new BigDecimal("3000.00"));
        customer.setTotalPaid(new BigDecimal("300.00"));
        customer.setRemainingBalance(new BigDecimal("2700.00"));

        var debt = new com.isletmefinans.backend.entity.CreditTransaction();
        debt.setId(401L);
        debt.setTransactionType(com.isletmefinans.backend.entity.CreditTransactionType.DEBT);
        debt.setAmount(new BigDecimal("3000.00"));
        debt.setOccurredAt(LocalDateTime.of(2026, 7, 2, 18, 0));

        var payment = new com.isletmefinans.backend.entity.CreditTransaction();
        payment.setId(402L);
        payment.setTransactionType(com.isletmefinans.backend.entity.CreditTransactionType.PAYMENT);
        payment.setAmount(new BigDecimal("300.00"));
        payment.setOccurredAt(LocalDateTime.of(2026, 7, 2, 19, 0));

        customer.addTransaction(debt);
        customer.addTransaction(payment);

        when(creditTransactionRepository.findById(402L)).thenReturn(Optional.of(payment));
        when(creditCustomerRepository.saveAndFlush(any(CreditCustomer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        creditCustomerService.deleteTransaction(402L);

        assertThat(customer.getTransactions()).hasSize(1);
        assertThat(customer.getTransactions().get(0).getId()).isEqualTo(401L);
        verify(dailyRecordService).removeCreditPaymentIncome(402L);
        verify(creditCustomerRepository).saveAndFlush(customer);
    }
}
