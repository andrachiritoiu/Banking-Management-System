package com.pao.project.bank.model.transaction;

import com.pao.project.bank.exception.InvalidOperationException;
import com.pao.project.bank.model.account.Account;
import com.pao.project.bank.model.enums.TransactionType;

import java.time.LocalDateTime;

public class Deposit extends Transaction{
    private final Account destinationAccount;

    public Deposit(int id, TransactionType type, double amount, LocalDateTime timestamp, String description, Account destinationAccount) {
        super(id, type, amount, timestamp, description);

        if (destinationAccount == null) {
            throw new InvalidOperationException("Destination account cannot be null.");
        }

        this.destinationAccount = destinationAccount;
    }

    public Account getDestinationAccount() {
        return destinationAccount;
    }

    @Override
    public String getSummary() {
        return "Deposit of " + amount +
                " into account " + destinationAccount.getIban();
    }

    @Override
    public String toString() {
        return "Deposit{" +
                "id=" + id +
                ", destinationAccount=" + destinationAccount +
                ", type=" + type +
                ", amount=" + amount +
                ", timestamp=" + timestamp +
                ", description='" + description + '\'' +
                '}';
    }
}
