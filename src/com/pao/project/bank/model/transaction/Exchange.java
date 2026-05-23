package com.pao.project.bank.model.transaction;

import com.pao.project.bank.model.account.Account;
import com.pao.project.bank.model.enums.Currency;
import com.pao.project.bank.model.enums.TransactionType;

import java.time.LocalDateTime;
import java.util.Objects;

public final class Exchange extends Transaction {

    private final Account sourceAccount;
    private final Account destinationAccount;
    private final double sourceAmount;
    private final double destinationAmount;
    private final Currency fromCurrency;
    private final Currency toCurrency;
    private final double exchangeRate;

    public Exchange(int id, Account sourceAccount, Account destinationAccount, double sourceAmount, double destinationAmount, Currency fromCurrency, Currency toCurrency, double exchangeRate, LocalDateTime dateTime, String description) {
        super(id, TransactionType.EXCHANGE, sourceAmount, dateTime, description);

        if (sourceAccount == null) {
            throw new IllegalArgumentException("Source account cannot be null.");
        }

        if (destinationAccount == null) {
            throw new IllegalArgumentException("Destination account cannot be null.");
        }

        if (sourceAmount <= 0) {
            throw new IllegalArgumentException("Source amount must be positive.");
        }

        if (destinationAmount <= 0) {
            throw new IllegalArgumentException("Destination amount must be positive.");
        }

        if (exchangeRate <= 0) {
            throw new IllegalArgumentException("Exchange rate must be positive.");
        }

        if (fromCurrency == null || toCurrency == null) {
            throw new IllegalArgumentException("Currencies cannot be null.");
        }

        if (fromCurrency == toCurrency) {
            throw new IllegalArgumentException("Exchange must be between different currencies.");
        }

        this.sourceAccount = sourceAccount;
        this.destinationAccount = destinationAccount;
        this.sourceAmount = sourceAmount;
        this.destinationAmount = destinationAmount;
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.exchangeRate = exchangeRate;
    }

    public Account getSourceAccount() {
        return sourceAccount;
    }

    public Account getDestinationAccount() {
        return destinationAccount;
    }

    public double getSourceAmount() {
        return sourceAmount;
    }

    public double getDestinationAmount() {
        return destinationAmount;
    }

    public Currency getFromCurrency() {
        return fromCurrency;
    }

    public Currency getToCurrency() {
        return toCurrency;
    }

    public double getExchangeRate() {
        return exchangeRate;
    }

    @Override
    public String getSummary() {
        return "Exchange: " +
                sourceAmount + " " + fromCurrency +
                " -> " +
                destinationAmount + " " + toCurrency +
                " | rate: " + exchangeRate +
                " | from " + sourceAccount.getIban() +
                " to " + destinationAccount.getIban();
    }

    @Override
    public boolean involvesAccount(String iban) {
        return iban != null &&
                (iban.equals(sourceAccount.getIban().getCode()) ||
                        iban.equals(destinationAccount.getIban().getCode()));
    }

    @Override
    public String toString() {
        return "Exchange{" +
                "id=" + getId() +
                ", sourceAmount=" + sourceAmount +
                ", destinationAmount=" + destinationAmount +
                ", fromCurrency=" + fromCurrency +
                ", toCurrency=" + toCurrency +
                ", exchangeRate=" + exchangeRate +
                ", sourceAccount=" + sourceAccount.getIban() +
                ", destinationAccount=" + destinationAccount.getIban() +
                ", timestamp=" + getTimestamp() +
                ", description='" + getDescription() + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof Exchange exchange)) {
            return false;
        }

        return getId() == exchange.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
