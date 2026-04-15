package com.pao.project.bank.model;

import com.pao.project.bank.model.account.Account;
import com.pao.project.bank.model.enums.TransactionType;
import com.pao.project.bank.model.transaction.Transaction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AccountStatement {
    private final Account account;
    private final LocalDate generatedAt;
    private final List<Transaction> transactions;
    private final double totalInflows;
    private final double totalOutflows;
    private final double initialBalance;
    private final double finalBalance;

    public AccountStatement(Account account, List<Transaction> transactions) {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null.");
        }

        this.account = account;
        this.generatedAt = LocalDate.now();
        this.transactions = new ArrayList<>(transactions != null ? transactions : List.of());

        double inflows = 0;
        double outflows = 0;

        for (Transaction transaction : this.transactions) {
            if (transaction == null) {
                continue;
            }

            if (transaction.getType() == TransactionType.DEPOSIT) {
                inflows += transaction.getAmount();
            } else if (transaction.getType() == TransactionType.WITHDRAWAL) {
                outflows += transaction.getAmount();
            }

        }

        this.totalInflows = inflows;
        this.totalOutflows = outflows;
        this.finalBalance = account.getBalance();
        this.initialBalance = finalBalance - totalInflows + totalOutflows;
    }


    public Account getAccount() {
        return account;
    }

    public LocalDate getGeneratedAt() {
        return generatedAt;
    }

    public List<Transaction> getTransactions() {
        return new ArrayList<>(transactions);
    }

    public double getTotalInflows() {
        return totalInflows;
    }

    public double getTotalOutflows() {
        return totalOutflows;
    }

    public double getInitialBalance() {
        return initialBalance;
    }

    public double getFinalBalance() {
        return finalBalance;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AccountStatement that)) return false;
        return Double.compare(totalInflows, that.totalInflows) == 0 && Double.compare(totalOutflows, that.totalOutflows) == 0 && Double.compare(initialBalance, that.initialBalance) == 0 && Double.compare(finalBalance, that.finalBalance) == 0 && Objects.equals(account, that.account) && Objects.equals(generatedAt, that.generatedAt) && Objects.equals(transactions, that.transactions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(account, generatedAt, transactions, totalInflows, totalOutflows, initialBalance, finalBalance);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("---- ACCOUNT STATEMENT ----\n");
        sb.append("IBAN: ").append(account.getIban()).append("\n");
        sb.append("Generated at: ").append(generatedAt).append("\n");

        sb.append("- BALANCES\n");
        sb.append("Initial balance: ").append(initialBalance).append("\n");
        sb.append("Final balance: ").append(finalBalance).append("\n\n");

        sb.append("- SUMMARY\n");
        sb.append("Total inflows: ").append(totalInflows).append("\n");
        sb.append("Total outflows: ").append(totalOutflows).append("\n\n");

        sb.append("- TRANSACTIONS\n");
        if (transactions.isEmpty()) {
            sb.append("No transactions available.\n");
        } else {
            for (Transaction transaction : transactions) {
                sb.append(transaction).append("\n");
            }
        }

        return sb.toString();
    }
}
