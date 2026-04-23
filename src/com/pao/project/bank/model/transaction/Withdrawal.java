package com.pao.project.bank.model.transaction;

import com.pao.project.bank.exception.InvalidOperationException;
import com.pao.project.bank.model.account.Account;
import com.pao.project.bank.model.enums.TransactionType;

import java.time.LocalDateTime;

public class Withdrawal extends Transaction{
    private final Account sourceAccount;

    public Withdrawal(int id, TransactionType type, double amount, LocalDateTime timestamp, String description, Account sourceAccount) {
        super(id, type, amount, timestamp, description);

        if (sourceAccount == null) {
            throw new InvalidOperationException("Source account cannot be null.");
        }

        this.sourceAccount = sourceAccount;
    }

    public Account getSourceAccount() {
        return sourceAccount;
    }

    @Override
    public String getSummary() {
        return "Withdrawal of " + amount +
                " from account " + sourceAccount.getIban();
    }

    @Override
    public boolean involvesAccount(String iban) {
        return iban != null &&
                iban.equals(sourceAccount.getIban().getCode());
    }


    @Override
    public String toString() {
        return "Withdrawal{" +
                "id=" + id +
                ", sourceAccount=" + sourceAccount +
                ", type=" + type +
                ", amount=" + amount +
                ", timestamp=" + timestamp +
                ", description='" + description + '\'' +
                '}';
    }
}
