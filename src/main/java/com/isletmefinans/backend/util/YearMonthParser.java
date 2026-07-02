package com.isletmefinans.backend.util;

import com.isletmefinans.backend.exception.BusinessValidationException;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public final class YearMonthParser {

    private YearMonthParser() {
    }

    public static YearMonth parse(String value) {
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException exception) {
            throw new BusinessValidationException("Ay formati yyyy-MM olmali");
        }
    }
}

