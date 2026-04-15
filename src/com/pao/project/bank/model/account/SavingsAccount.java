package com.pao.project.bank.model.account;

import com.pao.project.bank.exception.InsufficientFundsException;
import com.pao.project.bank.exception.InvalidOperationException;
import com.pao.project.bank.exception.WithdrawalLimitExceededException;
import com.pao.project.bank.model.IBAN;
import com.pao.project.bank.model.enums.AccountType;
import com.pao.project.bank.model.person.Client;

import java.time.LocalDate;

public class SavingsAccount extends Account{
    private double interestRate;
    private int withdrawalsThisMonth;

    public SavingsAccount(int id, IBAN iban, double balance, String currency, Client owner, double interestRate, int withdrawalsThisMonth) {
        super(id, iban, balance, currency, owner);

        if (interestRate < 0) {
            throw new InvalidOperationException("Interest rate cannot be negative.");
        }
        if (withdrawalsThisMonth < 0) {
            throw new InvalidOperationException("Withdrawals count cannot be negative.");
        }

        this.interestRate = interestRate;
        this.withdrawalsThisMonth = withdrawalsThisMonth;
    }

    public SavingsAccount(int id, IBAN iban, double balance, String currency, Client owner, boolean active, LocalDate openingDate, double interestRate, int withdrawalsThisMonth) {
        super(id, iban, balance, currency, owner, active, openingDate);


        if (interestRate < 0) {
            throw new InvalidOperationException("Interest rate cannot be negative.");
        }
        if (withdrawalsThisMonth < 0) {
            throw new InvalidOperationException("Withdrawals count cannot be negative.");
        }

        this.interestRate = interestRate;
        this.withdrawalsThisMonth = withdrawalsThisMonth;
    }

    public double getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(double interestRate) {
        if (interestRate < 0) {
            throw new InvalidOperationException("Interest rate cannot be negative.");
        }
        this.interestRate = interestRate;
    }

    public int getWithdrawalsThisMonth() {
        return withdrawalsThisMonth;
    }

    public void setWithdrawalsThisMonth(int withdrawalsThisMonth) {
        if (withdrawalsThisMonth < 0) {
            throw new InvalidOperationException("Withdrawals count cannot be negative.");
        }
        this.withdrawalsThisMonth = withdrawalsThisMonth;
    }


    public void applyInterest() {
        validateActiveAccount();
        setBalance(getBalance() + getBalance() * interestRate / 100);
    }

    @Override
    public AccountType getAccountType() {
        return AccountType.SAVINGS;
    }

    @Override
    public void withdraw(double amount) {
        validateActiveAccount();
        validateAmount(amount);

        if (withdrawalsThisMonth >= 2)
            throw new WithdrawalLimitExceededException("Max 2 withdrawals per month.");

        if (amount > balance)
            throw new InsufficientFundsException("Not enough money.");

        setBalance(getBalance() - amount);
        withdrawalsThisMonth++;
    }

    @Override
    public String toString() {
        return "SavingsAccount{" +
                "id=" + id +
                ", interestRate=" + interestRate +
                ", withdrawalsThisMonth=" + withdrawalsThisMonth +
                ", iban=" + iban +
                ", balance=" + balance +
                ", currency='" + currency + '\'' +
                ", owner=" + owner +
                ", active=" + active +
                ", openingDate=" + openingDate +
                '}';
    }
}
