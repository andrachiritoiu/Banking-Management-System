package com.pao.project.bank.service;

import com.pao.project.bank.model.AccountStatement;
import com.pao.project.bank.model.Credit;
import com.pao.project.bank.model.account.Account;
import com.pao.project.bank.model.enums.CreditStatus;
import com.pao.project.bank.model.enums.TransactionType;
import com.pao.project.bank.model.person.Client;
import com.pao.project.bank.model.transaction.Deposit;
import com.pao.project.bank.model.transaction.Exchange;
import com.pao.project.bank.model.transaction.Transaction;
import com.pao.project.bank.model.transaction.Transfer;
import com.pao.project.bank.model.transaction.Withdrawal;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class ReportService {
    private static final ReportService INSTANCE = new ReportService();

    private final TransactionService transactionService = TransactionService.getInstance();
    private final AccountService accountService = AccountService.getInstance();
    private final CreditService creditService = CreditService.getInstance();

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

    public AccountStatement generateMonthlyAccountStatement(Account account, YearMonth month) {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null.");
        }

        if (month == null) {
            throw new IllegalArgumentException("Month cannot be null.");
        }

        List<Transaction> monthlyTransactions = getTransactionsForAccountInMonth(account, month);
        double inflows = calculateTotalInflows(account, monthlyTransactions);
        double outflows = calculateTotalOutflows(account, monthlyTransactions);

        return new AccountStatement(
                account,
                monthlyTransactions,
                inflows,
                outflows,
                account.getBalance()
        );
    }

    public Map<YearMonth, Double> calculateTotalIncomingByMonth(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null.");
        }

        Map<YearMonth, Double> totals = new TreeMap<>();

        for (Transaction transaction : transactionService.getTransactionsForAccount(account.getIban().getCode())) {
            double amount = getIncomingAmount(account, transaction);

            if (amount > 0) {
                YearMonth month = YearMonth.from(transaction.getTimestamp());
                totals.put(month, totals.getOrDefault(month, 0.0) + amount);
            }
        }

        return totals;
    }

    public Map<YearMonth, Double> calculateTotalOutgoingByMonth(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null.");
        }

        Map<YearMonth, Double> totals = new TreeMap<>();

        for (Transaction transaction : transactionService.getTransactionsForAccount(account.getIban().getCode())) {
            double amount = getOutgoingAmount(account, transaction);

            if (amount > 0) {
                YearMonth month = YearMonth.from(transaction.getTimestamp());
                totals.put(month, totals.getOrDefault(month, 0.0) + amount);
            }
        }

        return totals;
    }

    public Map<Client, Double> getTopClientsByBalance(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive.");
        }

        Map<Client, Double> balancesByClient = new LinkedHashMap<>();

        for (Account account : accountService.getAllAccounts()) {
            Client owner = account.getOwner();
            balancesByClient.put(owner, balancesByClient.getOrDefault(owner, 0.0) + account.getBalance());
        }

        return balancesByClient.entrySet()
                .stream()
                .sorted(Map.Entry.<Client, Double>comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new
                ));
    }

    public Map<TransactionType, List<Transaction>> getTransactionsGroupedByType() {
        Map<TransactionType, List<Transaction>> groupedTransactions = new LinkedHashMap<>();

        for (Transaction transaction : transactionService.getAllTransactions()) {
            groupedTransactions
                    .computeIfAbsent(transaction.getType(), key -> new ArrayList<>())
                    .add(transaction);
        }

        return groupedTransactions;
    }

    public Map<String, List<Account>> getAccountsGroupedByCurrency() {
        Map<String, List<Account>> accountsByCurrency = new TreeMap<>();

        for (Account account : accountService.getAllAccounts()) {
            accountsByCurrency
                    .computeIfAbsent(account.getCurrency(), key -> new ArrayList<>())
                    .add(account);
        }

        return accountsByCurrency;
    }

    public Map<CreditStatus, List<Credit>> getCreditsGroupedByStatus() {
        Map<CreditStatus, List<Credit>> creditsByStatus = new LinkedHashMap<>();

        for (Credit credit : creditService.getAllCredits()) {
            creditsByStatus
                    .computeIfAbsent(credit.getStatus(), key -> new ArrayList<>())
                    .add(credit);
        }

        return creditsByStatus;
    }

    private List<Transaction> getTransactionsForAccountInMonth(Account account, YearMonth month) {
        List<Transaction> result = new ArrayList<>();

        for (Transaction transaction : transactionService.getTransactionsForAccount(account.getIban().getCode())) {
            if (YearMonth.from(transaction.getTimestamp()).equals(month)) {
                result.add(transaction);
            }
        }

        return result;
    }

    private double calculateTotalInflows(Account account, List<Transaction> transactions) {
        double total = 0;

        for (Transaction transaction : transactions) {
            total += getIncomingAmount(account, transaction);
        }

        return total;
    }

    private double calculateTotalOutflows(Account account, List<Transaction> transactions) {
        double total = 0;

        for (Transaction transaction : transactions) {
            total += getOutgoingAmount(account, transaction);
        }

        return total;
    }

    private double getIncomingAmount(Account account, Transaction transaction) {
        if (transaction instanceof Deposit deposit) {
            return deposit.getDestinationAccount().equals(account) ? deposit.getAmount() : 0;
        }

        if (transaction instanceof Transfer transfer) {
            return transfer.getDestinationAccount().equals(account) ? transfer.getAmount() : 0;
        }

        if (transaction instanceof Exchange exchange) {
            return exchange.getDestinationAccount().equals(account) ? exchange.getDestinationAmount() : 0;
        }

        return 0;
    }

    private double getOutgoingAmount(Account account, Transaction transaction) {
        if (transaction instanceof Withdrawal withdrawal) {
            return withdrawal.getSourceAccount().equals(account) ? withdrawal.getAmount() : 0;
        }

        if (transaction instanceof Transfer transfer) {
            return transfer.getSourceAccount().equals(account) ? transfer.getAmount() : 0;
        }

        if (transaction instanceof Exchange exchange) {
            return exchange.getSourceAccount().equals(account) ? exchange.getSourceAmount() : 0;
        }

        return 0;
    }
}
