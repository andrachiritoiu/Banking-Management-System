package com.pao.project.bank.exception;

public class CreditNotApprovedException extends RuntimeException {
    public CreditNotApprovedException(String message) {
        super(message);
    }
}
