package com.pao.project.bank.model.transaction;

import com.pao.project.bank.exception.InvalidOperationException;
import com.pao.project.bank.model.account.Account;
import com.pao.project.bank.model.enums.TransactionType;

import java.time.LocalDateTime;

public class Transfer extends Transaction {
    private final Account destinationAccount;
    private final Account sourceAccount;

    public Transfer(int id, TransactionType type, double amount, LocalDateTime timestamp, String description, Account destinationAccount, Account sourceAccount) {
        super(id, type, amount, timestamp, description);

        if (sourceAccount == null || destinationAccount == null) {
            throw new InvalidOperationException("Source and destination accounts cannot be null.");
        }

        if (sourceAccount.equals(destinationAccount)) {
            throw new InvalidOperationException("Source and destination accounts must be different.");
        }

        this.destinationAccount = destinationAccount;
        this.sourceAccount = sourceAccount;
    }

    public Account getDestinationAccount() {
        return destinationAccount;
    }

    public Account getSourceAccount() {
        return sourceAccount;
    }

    @Override
    public String getSummary() {
        return "Transfer of " + amount +
                " from account " + sourceAccount.getIban() +
                " to account " + destinationAccount.getIban();
    }

    @Override
    public boolean involvesAccount(String iban) {
        return iban != null &&
                (iban.equals(sourceAccount.getIban().getCode()) ||
                        iban.equals(destinationAccount.getIban().getCode()));
    }

    @Override
    public String toString() {
        return "Transfer{" +
                "id=" + id +
                ", destinationAccount=" + destinationAccount +
                ", sourceAccount=" + sourceAccount +
                ", type=" + type +
                ", amount=" + amount +
                ", timestamp=" + timestamp +
                ", description='" + description + '\'' +
                '}';
    }
}
