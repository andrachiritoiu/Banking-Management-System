package com.pao.project.bank.model.account;

import com.pao.project.bank.exception.AccountClosedException;
import com.pao.project.bank.exception.InvalidOperationException;
import com.pao.project.bank.model.IBAN;
import com.pao.project.bank.model.enums.AccountType;
import com.pao.project.bank.model.person.Client;

import java.time.LocalDate;
import java.util.Objects;

public abstract class Account implements Transactable, Comparable<Account>{
    protected int id;
    protected IBAN iban;
    protected double balance;
    protected String currency;
    protected Client owner;
    protected boolean active;
    protected LocalDate openingDate;

    //new account
    public Account(int id, IBAN iban, double balance, String currency, Client owner) {
        this(id, iban, balance, currency, owner, true, LocalDate.now());
    }

    //DB account
    public Account(int id, IBAN iban, double balance, String currency, Client owner, boolean active, LocalDate openingDate) {
        if (iban == null)
            throw new InvalidOperationException("IBAN cannot be null.");
        if (currency == null || currency.isBlank())
            throw new InvalidOperationException("Currency cannot be null.");
        if (owner == null)
            throw new InvalidOperationException("Owner cannot be null.");
        if (balance < 0)
            throw new InvalidOperationException("Balance cannot be negative.");
        if (openingDate == null)
            throw new InvalidOperationException("Opening date cannot be null.");

        this.id = id;
        this.iban = iban;
        this.balance = balance;
        this.currency = currency;
        this.owner = owner;
        this.active = active;
        this.openingDate = openingDate;
    }


    public int getId() {
        return id;
    }

    public IBAN getIban() {
        return iban;
    }

    public double getBalance() {
        return balance;
    }

    protected void setBalance(double balance) {
        this.balance = balance;
    }

    public String getCurrency() {
        return currency;
    }

    public Client getOwner() {
        return owner;
    }

    public void setOwner(Client owner) {
        if (owner == null) {
            throw new InvalidOperationException("Owner cannot be null.");
        }
        this.owner = owner;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDate getOpeningDate() {
        return openingDate;
    }


    public abstract AccountType getAccountType();
    public abstract void withdraw(double amount);

    public void closeAccount(){
        this.active = false;
    }

    //internal
    protected void validateActiveAccount() {
        if (!active)
            throw new AccountClosedException("Account is closed.");
    }

    protected void validateAmount(double amount) {
        if (amount <= 0)
            throw new InvalidOperationException("Amount must be positive.");
    }

    @Override
    public void deposit(double amount) {
        validateActiveAccount();
        validateAmount(amount);

        balance+=amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account account)) return false;
        return Objects.equals(iban, account.iban);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(iban);
    }

    @Override
    public int compareTo(Account o) {
        if (o == null) {
            throw new NullPointerException("Compared account cannot be null.");
        }
        return this.iban.getCode().compareTo(o.iban.getCode());
    }

    @Override
    public String toString() {
        return "Account{" +
                "id=" + id +
                ", iban=" + iban +
                ", balance=" + balance +
                ", currency='" + currency + '\'' +
                ", owner=" + owner +
                ", active=" + active +
                ", openingDate=" + openingDate +
                '}';
    }
}
