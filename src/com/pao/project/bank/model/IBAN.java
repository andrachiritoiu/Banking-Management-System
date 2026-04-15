package com.pao.project.bank.model;

public class IBAN {
    private final String code;

    public IBAN(String code) {
        if (!code.matches("RO\\d{2}[A-Z]{4}\\d{16}")) {
            throw new IllegalArgumentException("Invalid IBAN");
        }
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
