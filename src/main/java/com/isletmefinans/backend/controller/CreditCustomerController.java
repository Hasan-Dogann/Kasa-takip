package com.isletmefinans.backend.controller;

import com.isletmefinans.backend.dto.CreditCustomerCreateRequest;
import com.isletmefinans.backend.dto.CreditCustomerDetailResponse;
import com.isletmefinans.backend.dto.CreditCustomerSummaryResponse;
import com.isletmefinans.backend.dto.CreditCustomerUpdateRequest;
import com.isletmefinans.backend.dto.CreditDebtRequest;
import com.isletmefinans.backend.dto.CreditPaymentRequest;
import com.isletmefinans.backend.dto.CreditTransactionUpdateRequest;
import com.isletmefinans.backend.service.CreditCustomerService;
import jakarta.validation.Valid;
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

import java.util.List;

@RestController
@RequestMapping("/api/credits")
public class CreditCustomerController {

    private final CreditCustomerService creditCustomerService;

    public CreditCustomerController(CreditCustomerService creditCustomerService) {
        this.creditCustomerService = creditCustomerService;
    }

    @GetMapping("/customers")
    public List<CreditCustomerSummaryResponse> listCustomers(@RequestParam(defaultValue = "active") String status) {
        return "archive".equalsIgnoreCase(status) || "archived".equalsIgnoreCase(status)
                ? creditCustomerService.listArchivedCustomers()
                : creditCustomerService.listActiveCustomers();
    }

    @GetMapping("/customers/{customerId}")
    public CreditCustomerDetailResponse getCustomerDetail(@PathVariable Long customerId) {
        return creditCustomerService.getCustomerDetail(customerId);
    }

    @PostMapping("/customers")
    @ResponseStatus(HttpStatus.CREATED)
    public CreditCustomerDetailResponse createCustomer(@Valid @RequestBody CreditCustomerCreateRequest request) {
        return creditCustomerService.createCustomerWithDebt(request);
    }

    @PutMapping("/customers/{customerId}")
    public CreditCustomerDetailResponse updateCustomer(
            @PathVariable Long customerId,
            @Valid @RequestBody CreditCustomerUpdateRequest request
    ) {
        return creditCustomerService.updateCustomer(customerId, request);
    }

    @DeleteMapping("/customers/{customerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCustomer(@PathVariable Long customerId) {
        creditCustomerService.deleteCustomer(customerId);
    }

    @PostMapping("/customers/{customerId}/debts")
    public CreditCustomerDetailResponse addDebt(
            @PathVariable Long customerId,
            @Valid @RequestBody CreditDebtRequest request
    ) {
        return creditCustomerService.addDebt(customerId, request);
    }

    @PostMapping("/customers/{customerId}/payments")
    public CreditCustomerDetailResponse recordPayment(
            @PathVariable Long customerId,
            @Valid @RequestBody CreditPaymentRequest request
    ) {
        return creditCustomerService.recordPayment(customerId, request);
    }

    @PutMapping("/transactions/{transactionId}")
    public CreditCustomerDetailResponse updateTransaction(
            @PathVariable Long transactionId,
            @Valid @RequestBody CreditTransactionUpdateRequest request
    ) {
        return creditCustomerService.updateTransaction(transactionId, request);
    }

    @DeleteMapping("/transactions/{transactionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTransaction(@PathVariable Long transactionId) {
        creditCustomerService.deleteTransaction(transactionId);
    }
}
