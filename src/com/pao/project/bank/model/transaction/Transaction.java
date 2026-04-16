package com.pao.project.bank.model.transaction;

import com.pao.project.bank.exception.InvalidOperationException;
import com.pao.project.bank.model.enums.TransactionType;

import java.time.LocalDateTime;
import java.util.Objects;

public abstract class Transaction {
    protected final int id;
    protected final TransactionType type;
    protected final double amount;
    protected final LocalDateTime timestamp;
    protected final String description;

    public Transaction(int id, TransactionType type, double amount, LocalDateTime timestamp, String description) {
        if (type == null) {
            throw new InvalidOperationException("Transaction type cannot be null.");
        }
        if (amount <= 0) {
            throw new InvalidOperationException("Transaction amount must be greater than 0.");
        }

        this.id = id;
        this.type = type;
        this.amount = amount;
        this.timestamp = timestamp;
        this.description = description == null ? "" : description;
    }


    public int getId() {
        return id;
    }

    public TransactionType getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getDescription() {
        return description;
    }

    public abstract String getSummary();
    public abstract boolean involvesAccount(String iban);

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Transaction that)) return false;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", type=" + type +
                ", amount=" + amount +
                ", timestamp=" + timestamp +
                ", description='" + description + '\'' +
                '}';
    }
}
