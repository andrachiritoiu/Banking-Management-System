package com.pao.project.bank.model;

import com.pao.project.bank.exception.InvalidOperationException;
import com.pao.project.bank.model.account.Account;
import com.pao.project.bank.model.enums.CreditStatus;
import com.pao.project.bank.model.enums.CreditType;
import com.pao.project.bank.model.person.Client;

import java.time.LocalDate;
import java.util.Objects;

public class Credit {
    private final int id;
    private final Client borrower;
    private final Account targetAccount;
    private final CreditType type;
    private final double principalAmount;
    private final double annualInterestRate;
    private final int durationInMonths;
    private final LocalDate startDate;

    private double remainingAmount;
    private CreditStatus status;

    public Credit(int id, Client borrower, Account targetAccount, CreditType type, double principalAmount, double annualInterestRate, int durationInMonths, LocalDate startDate) {
        if (borrower == null) {
            throw new InvalidOperationException("Borrower cannot be null.");
        }

        if (targetAccount == null) {
            throw new InvalidOperationException("Target account cannot be null.");
        }

        if (type == null) {
            throw new InvalidOperationException("Credit type cannot be null.");
        }

        if (principalAmount <= 0) {
            throw new InvalidOperationException("Principal amount must be greater than 0.");
        }

        if (annualInterestRate < 0) {
            throw new InvalidOperationException("Annual interest rate cannot be negative.");
        }

        if (durationInMonths <= 0) {
            throw new InvalidOperationException("Duration must be greater than 0.");
        }

        if (startDate == null) {
            throw new InvalidOperationException("Start date cannot be null.");
        }

        this.id = id;
        this.borrower = borrower;
        this.targetAccount = targetAccount;
        this.type = type;
        this.principalAmount = principalAmount;
        this.annualInterestRate = annualInterestRate;
        this.durationInMonths = durationInMonths;
        this.startDate = startDate;
        this.remainingAmount = calculateTotalAmountToPay();
        this.status = CreditStatus.PENDING;
    }

    public int getId() {
        return id;
    }

    public Client getBorrower() {
        return borrower;
    }

    public Account getTargetAccount() {
        return targetAccount;
    }

    public CreditType getType() {
        return type;
    }

    public double getPrincipalAmount() {
        return principalAmount;
    }

    public double getAnnualInterestRate() {
        return annualInterestRate;
    }

    public int getDurationInMonths() {
        return durationInMonths;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public double getRemainingAmount() {
        return remainingAmount;
    }

    public CreditStatus getStatus() {
        return status;
    }

    public double calculateTotalAmountToPay() {
        double years = durationInMonths / 12.0;
        return principalAmount + principalAmount * annualInterestRate * years / 100.0;
    }

    public double calculateMonthlyInstallment() {
        return calculateTotalAmountToPay() / durationInMonths;
    }

    public void approve() {
        if (status != CreditStatus.PENDING) {
            throw new InvalidOperationException("Only pending credits can be approved.");
        }

        status = CreditStatus.ACTIVE;
    }

    public void reject() {
        if (status != CreditStatus.PENDING) {
            throw new InvalidOperationException("Only pending credits can be rejected.");
        }

        status = CreditStatus.REJECTED;
    }

    public void payInstallment(double amount) {
        if (status != CreditStatus.ACTIVE) {
            throw new InvalidOperationException("Only active credits can be paid.");
        }

        if (amount <= 0) {
            throw new InvalidOperationException("Payment amount must be greater than 0.");
        }

        remainingAmount -= amount;

        if (remainingAmount <= 0) {
            remainingAmount = 0;
            status = CreditStatus.PAID;
        }
    }

    public boolean isActive() {
        return status == CreditStatus.ACTIVE;
    }

    public boolean isPaid() {
        return status == CreditStatus.PAID;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Credit credit)) {
            return false;
        }

        return id == credit.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Credit{" +
                "id=" + id +
                ", borrower=" + borrower.getFullName() +
                ", targetAccount=" + targetAccount.getIban().getCode() +
                ", type=" + type +
                ", principalAmount=" + principalAmount +
                ", annualInterestRate=" + annualInterestRate +
                ", durationInMonths=" + durationInMonths +
                ", startDate=" + startDate +
                ", remainingAmount=" + remainingAmount +
                ", status=" + status +
                '}';
    }
}
