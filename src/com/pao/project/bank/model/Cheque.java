package com.pao.project.bank.model;

import com.pao.project.bank.exception.ChequeExpiredException;
import com.pao.project.bank.model.account.Account;
import com.pao.project.bank.model.enums.ChequeStatus;
import com.pao.project.bank.model.person.Client;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Random;

public class Cheque {
    private final String series;
    private final Account issuerAccount;
    private final Client beneficiary;
    private final double amount;
    private final LocalDate issueDate;
    private final LocalDate expiryDate;
    private ChequeStatus status;

    public Cheque(Account issuerAccount, Client beneficiary, double amount, LocalDate expiryDate) {
        if (issuerAccount == null) {
            throw new IllegalArgumentException("Issuer account cannot be null.");
        }
        if (beneficiary == null) {
            throw new IllegalArgumentException("Beneficiary cannot be null.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Cheque amount must be positive.");
        }
        if (expiryDate == null) {
            throw new IllegalArgumentException("Expiry date cannot be null.");
        }
        if (!expiryDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Expiry date must be in the future.");
        }

        this.series = generateSeries();
        this.issuerAccount = issuerAccount;
        this.beneficiary = beneficiary;
        this.amount = amount;
        this.issueDate = LocalDate.now();
        this.expiryDate = expiryDate;
        this.status = ChequeStatus.ISSUED;
    }

    private String generateSeries() {
        Random random = new Random();
        int number = random.nextInt(90000000) + 10000000; //8
        return "CEC-" + number;
    }


    public String getSeries() {
        return series;
    }

    public Account getIssuerAccount() {
        return issuerAccount;
    }

    public Client getBeneficiary() {
        return beneficiary;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public ChequeStatus getStatus() {
        if (status == ChequeStatus.ISSUED && isExpired()) {
            status = ChequeStatus.EXPIRED;
        }
        return status;
    }

    public boolean isExpired() {
        return LocalDate.now().isAfter(expiryDate);
    }



    public void cash() {
        if (getStatus() == ChequeStatus.EXPIRED) {
            throw new ChequeExpiredException("Expired cheque cannot be cashed.");
        }
        if (status == ChequeStatus.CANCELLED) {
            throw new IllegalStateException("Cancelled cheque cannot be cashed.");
        }
        if (status == ChequeStatus.CASHED) {
            throw new IllegalStateException("Cheque is already cashed.");
        }

        status = ChequeStatus.CASHED;
    }

    public void cancel() {
        if (getStatus() == ChequeStatus.EXPIRED) {
            throw new ChequeExpiredException("Expired cheque cannot be cancelled.");
        }
        if (status == ChequeStatus.CASHED) {
            throw new IllegalStateException("Cashed cheque cannot be cancelled.");
        }
        if (status == ChequeStatus.CANCELLED) {
            throw new IllegalStateException("Cheque is already cancelled.");
        }

        status = ChequeStatus.CANCELLED;
    }


    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Cheque cheque)) return false;
        return Objects.equals(series, cheque.series);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(series);
    }

    @Override
    public String toString() {
        return "Cheque{" +
                "series='" + series + '\'' +
                ", issuerAccount=" + issuerAccount +
                ", beneficiary=" + beneficiary +
                ", amount=" + amount +
                ", issueDate=" + issueDate +
                ", expiryDate=" + expiryDate +
                ", status=" + status +
                '}';
    }
}
