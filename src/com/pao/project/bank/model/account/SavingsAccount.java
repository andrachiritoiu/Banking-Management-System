package com.pao.project.bank.model.account;

import com.pao.project.bank.exception.InsufficientFundsException;
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
        this.interestRate = interestRate;
        this.withdrawalsThisMonth = withdrawalsThisMonth;
    }

    public SavingsAccount(int id, IBAN iban, double balance, String currency, Client owner, boolean active, LocalDate openingDate, double interestRate, int withdrawalsThisMonth) {
        super(id, iban, balance, currency, owner, active, openingDate);
        this.interestRate = interestRate;
        this.withdrawalsThisMonth = withdrawalsThisMonth;
    }


    public void applyInterest() {
        balance += balance * interestRate / 100;
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

        balance -= amount;
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
