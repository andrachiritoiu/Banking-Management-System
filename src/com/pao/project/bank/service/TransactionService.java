package com.pao.project.bank.service;

import com.pao.project.bank.model.transaction.Transaction;

import java.util.ArrayList;
import java.util.List;

public class TransactionService {
    private static final TransactionService INSTANCE = new TransactionService();

    private final List<Transaction> transactions = new ArrayList<>();

    private TransactionService() {}

    public static TransactionService getInstance() {
        return INSTANCE;
    }


    public void recordTransaction(Transaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null.");
        }

        transactions.add(transaction);
    }
}
