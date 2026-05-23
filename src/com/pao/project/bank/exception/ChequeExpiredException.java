package com.pao.project.bank.exception;

public class ChequeExpiredException extends RuntimeException {
    public ChequeExpiredException(String message) {
        super(message);
    }
}
