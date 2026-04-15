package com.pao.project.bank.model;

import java.util.Objects;

public final class IBAN {
    private final String code;

    public IBAN(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("IBAN cannot be null or empty.");
        }

        String normalized = code.replaceAll("\\s+", "").toUpperCase();

        if (!normalized.matches("RO\\d{2}[A-Z]{4}[A-Z0-9]{16}")) {
            throw new IllegalArgumentException("Invalid IBAN");
        }
        this.code = normalized;
    }

    public String getCode() {
        return code;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof IBAN iban)) return false;
        return Objects.equals(code, iban.code);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(code);
    }

    @Override
    public String toString() {
        return code;
    }
}
