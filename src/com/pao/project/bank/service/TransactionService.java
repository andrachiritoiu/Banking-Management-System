package com.pao.project.bank.service;

import com.pao.project.bank.model.enums.TransactionType;
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

    public List<Transaction> getAllTransactions() {
        return new ArrayList<>(transactions);
    }

    public List<Transaction>  getTransactionsForAccount(String iban) {
        List<Transaction> result = new ArrayList<>();

        if (iban == null) {
            return result;
        }

        for(Transaction transaction : transactions){
            if(transaction.involvesAccount(iban)){
                result.add(transaction);
            }
        }

        return result;
    }

    public List<Transaction> getTransactionsForAccountByType(String iban, TransactionType type) {
        List<Transaction> result = new ArrayList<>();

        if (iban == null || type == null) {
            return result;
        }

        for (Transaction transaction : transactions) {
            if (transaction.involvesAccount(iban) && transaction.getType() == type) {
                result.add(transaction);
            }
        }

        return result;
    }

    public List<Transaction> getTransactionsSortedByDate(String iban) {
        List<Transaction> result = getTransactionsForAccount(iban);
        result.sort((t1, t2) -> t1.getTimestamp().compareTo(t2.getTimestamp()));
        return result;
    }
}
