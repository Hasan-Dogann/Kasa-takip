package com.isletmefinans.backend.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum ExpenseCategory {
    TAVUK("tavuk", "Tavuk"),
    KIRMIZI_ET("kirmizi_et", "Kirmizi Et"),
    MANAV("manav", "Manav"),
    PERSONEL("personel", "Personel"),
    MARKET("market", "Market"),
    LAVAS("lavas", "Lavas"),
    EKMEK("ekmek", "Ekmek"),
    KELLE("kelle", "Kelle"),
    SU("su", "Su"),
    AMBALAJ("ambalaj", "Ambalaj"),
    CAY("cay", "Cay (Eski)"),
    SOGUK_MESRUBAT("soguk_mesrubat", "Soguk Mesrubat"),
    DIGER("diger", "Diger");

    private final String value;
    private final String label;

    ExpenseCategory(String value, String label) {
        this.value = value;
        this.label = label;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static ExpenseCategory fromValue(String rawValue) {
        if (rawValue == null) {
            return null;
        }

        return Arrays.stream(values())
                .filter(category -> category.value.equalsIgnoreCase(rawValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Gecersiz gider kategorisi: " + rawValue));
    }
}
