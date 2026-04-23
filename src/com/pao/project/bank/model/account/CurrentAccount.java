package com.pao.project.bank.model.account;

import com.pao.project.bank.exception.InsufficientFundsException;
import com.pao.project.bank.exception.InvalidOperationException;
import com.pao.project.bank.model.IBAN;
import com.pao.project.bank.model.enums.AccountType;
import com.pao.project.bank.model.person.Client;

import java.time.LocalDate;

public class CurrentAccount extends Account{
    private double monthlyFee;

    public CurrentAccount(int id, IBAN iban, double balance, String currency, Client owner, double monthlyFee) {
        super(id, iban, balance, currency, owner);

        if (monthlyFee < 0) {
            throw new InvalidOperationException("Monthly fee cannot be negative.");
        }

        this.monthlyFee = monthlyFee;
    }

    public CurrentAccount(int id, IBAN iban, double balance, String currency, Client owner, boolean active, LocalDate openingDate, double monthlyFee) {
        super(id, iban, balance, currency, owner, active, openingDate);

        if (monthlyFee < 0) {
            throw new InvalidOperationException("Monthly fee cannot be negative.");
        }

        this.monthlyFee = monthlyFee;
    }

    public double getMonthlyFee() {
        return monthlyFee;
    }

    public void setMonthlyFee(double monthlyFee) {
        if (monthlyFee < 0) {
            throw new InvalidOperationException("Monthly fee cannot be negative.");
        }
        this.monthlyFee = monthlyFee;
    }



    public void applyMonthlyFee() {
        validateActiveAccount();

        if (monthlyFee > 0) {
            if (monthlyFee > balance) {
                throw new InsufficientFundsException("Not enough money for monthly fee.");
            }
            setBalance(getBalance() - monthlyFee);
        }
    }

    @Override
    public AccountType getAccountType() {
        return AccountType.CURRENT;
    }

    @Override
    public void withdraw(double amount) {
        validateActiveAccount();
        validateAmount(amount);

        if (amount > balance)
            throw new InsufficientFundsException("Not enough money.");

        setBalance(getBalance() - amount);
    }

    @Override
    public String toString() {
        return "CurrentAccount{" +
                "id=" + id +
                ", monthlyFee=" + monthlyFee +
                ", iban=" + iban +
                ", balance=" + balance +
                ", currency='" + currency + '\'' +
                ", owner=" + owner +
                ", active=" + active +
                ", openingDate=" + openingDate +
                '}';
    }
}
