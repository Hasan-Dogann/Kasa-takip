package com.isletmefinans.backend.dto;

import com.isletmefinans.backend.entity.CreditCustomer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreditCustomerSummaryResponse(
        Long id,
        String fullName,
        String phoneNumber,
        BigDecimal totalDebt,
        BigDecimal totalPaid,
        BigDecimal remainingBalance,
        boolean archived,
        LocalDateTime updatedAt,
        LocalDateTime archivedAt
) {

    public static CreditCustomerSummaryResponse from(CreditCustomer customer) {
        return new CreditCustomerSummaryResponse(
                customer.getId(),
                customer.getFullName(),
                customer.getPhoneNumber(),
                customer.getTotalDebt(),
                customer.getTotalPaid(),
                customer.getRemainingBalance(),
                customer.isArchived(),
                customer.getUpdatedAt(),
                customer.getArchivedAt()
        );
    }
}
