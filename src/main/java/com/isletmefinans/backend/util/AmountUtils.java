package com.isletmefinans.backend.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class AmountUtils {

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private AmountUtils() {
    }

    public static BigDecimal normalize(BigDecimal amount) {
        if (amount == null) {
            return ZERO;
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal min(BigDecimal first, BigDecimal second) {
        BigDecimal normalizedFirst = normalize(first);
        BigDecimal normalizedSecond = normalize(second);
        return normalizedFirst.compareTo(normalizedSecond) <= 0 ? normalizedFirst : normalizedSecond;
    }

    public static BigDecimal max(BigDecimal first, BigDecimal second) {
        BigDecimal normalizedFirst = normalize(first);
        BigDecimal normalizedSecond = normalize(second);
        return normalizedFirst.compareTo(normalizedSecond) >= 0 ? normalizedFirst : normalizedSecond;
    }
}

