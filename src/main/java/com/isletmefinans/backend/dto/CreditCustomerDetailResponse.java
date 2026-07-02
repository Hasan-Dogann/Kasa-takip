package com.isletmefinans.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CreditCustomerDetailResponse(
        Long id,
        String fullName,
        String phoneNumber,
        BigDecimal totalDebt,
        BigDecimal totalPaid,
        BigDecimal remainingBalance,
        boolean archived,
        LocalDateTime archivedAt,
        List<CreditTransactionResponse> transactions
) {
}
