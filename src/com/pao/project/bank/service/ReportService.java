package com.pao.project.bank.service;

import com.pao.project.bank.model.AccountStatement;
import com.pao.project.bank.model.account.Account;
import com.pao.project.bank.model.transaction.Deposit;
import com.pao.project.bank.model.transaction.Exchange;
import com.pao.project.bank.model.transaction.Transaction;
import com.pao.project.bank.model.transaction.Transfer;
import com.pao.project.bank.model.transaction.Withdrawal;

import java.util.List;

public class ReportService {
    private static final ReportService INSTANCE = new ReportService();

    private final TransactionService transactionService = TransactionService.getInstance();

    private ReportService() {}

    public static ReportService getInstance() {
        return INSTANCE;
    }

    public double calculateTotalInflows(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null.");
        }

        double total = 0;
        String iban = account.getIban().getCode();

        List<Transaction> transactions = transactionService.getTransactionsForAccount(iban);

        for(Transaction transaction : transactions){
            if(transaction instanceof Deposit deposit){
                total+=deposit.getAmount();
            }

            if(transaction instanceof Transfer transfer){
                if(transfer.getDestinationAccount().equals(account)){
                    total+=transfer.getAmount();
                }
            }

            if(transaction instanceof Exchange exchange){
                if(exchange.getDestinationAccount().equals(account)){
                    total+=exchange.getDestinationAmount();
                }
            }
        }
        return total;
    }

    public double calculateTotalOutflows(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null.");
        }

        double total = 0;
        String iban = account.getIban().getCode();

        List<Transaction> transactions = transactionService.getTransactionsForAccount(iban);

        for(Transaction transaction : transactions){
            if(transaction instanceof Withdrawal withdrawal){
                total+=withdrawal.getAmount();
            }

            if(transaction instanceof Transfer transfer){
                if(transfer.getSourceAccount().equals(account)){
                    total+=transfer.getAmount();
                }
            }

            if(transaction instanceof Exchange exchange){
                if(exchange.getSourceAccount().equals(account)){
                    total+=exchange.getSourceAmount();
                }
            }
        }
        return total;
    }

    public List<Transaction> getTransactionHistory(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null.");
        }

        return transactionService.getTransactionsForAccount(account.getIban().getCode());
    }

    public AccountStatement generateAccountStatement(Account account){
        if(account == null){
            throw new IllegalArgumentException("Account cannot be null.");
        }

        String iban = account.getIban().getCode();
        List<Transaction> transactionsHistory = transactionService.getTransactionsForAccount(iban);

        double inflows = calculateTotalInflows(account);
        double outflows = calculateTotalOutflows(account);

        return new AccountStatement(
                account,
                transactionsHistory,
                inflows,
                outflows,
                account.getBalance()
        );
    }
}
