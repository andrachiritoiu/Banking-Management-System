package com.pao.project.bank.service;

import com.pao.project.bank.model.account.Account;
import com.pao.project.bank.model.enums.TransactionType;
import com.pao.project.bank.model.person.Client;
import com.pao.project.bank.model.transaction.Deposit;
import com.pao.project.bank.model.transaction.Transfer;
import com.pao.project.bank.model.transaction.Withdrawal;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountService {
    private static final AccountService INSTANCE = new AccountService();
    private final TransactionService transactionService = TransactionService.getInstance();

    private int transactionIdCounter = 1;
    private final List<Account> accounts = new ArrayList<>();
    private final Map<String, Account> accountsByIban = new HashMap<>();

    private AccountService() {}

    public static AccountService getInstance() {
        return INSTANCE;
    }

    public void openAccount(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null.");
        }

        String iban = account.getIban().getCode();

        if (findByIban(iban) != null) {
            throw new IllegalArgumentException("An account with this IBAN already exists.");
        }

        accounts.add(account);
        accountsByIban.put(iban, account);
    }

    public Account findByIban(String iban){
        if(iban == null){
            throw new IllegalArgumentException("IBAN cannot be null.");
        }

        return accountsByIban.get(iban);
    }

    public void closeAccount(String iban){
        if(iban == null){
            throw new IllegalArgumentException("IBAN cannot be null.");
        }

        Account account = accountsByIban.get(iban);

        if (account == null) {
            throw new IllegalArgumentException("Account not found.");
        }

        account.closeAccount();
    }

    public List<Account> getAllAccounts() {
        return new ArrayList<>(accounts);
    }

    public List<Account> getAccountsForClient(Client client) {
        List<Account> result = new ArrayList<>();

        if (client == null) {
            return result;
        }

        for (Account account : accounts) {
            if (account.getOwner().equals(client)) {
                result.add(account);
            }
        }

        return result;
    }


    private int generateTransactionId() {
        return transactionIdCounter++;
    }

    public void deposit(String iban, double amount){
        if(iban == null){
            throw new IllegalArgumentException("IBAN cannot be null.");
        }

        if(amount <= 0){
            throw new IllegalArgumentException("Deposit amount must be positive.");
        }

        Account account = accountsByIban.get(iban);

        if (account == null) {
            throw new IllegalArgumentException("Account not found.");
        }

        //sold
        account.deposit(amount);

        //new obj deposit
        Deposit deposit = new Deposit(generateTransactionId(),
                TransactionType.DEPOSIT,
                amount,
                LocalDateTime.now(),
                "Deposit operation",
                account);

        //save
        transactionService.recordTransaction(deposit);
    }

    public void withdraw(String iban, double amount){
        if(iban == null){
            throw new IllegalArgumentException("IBAN cannot be null.");
        }

        if(amount <= 0){
            throw new IllegalArgumentException("Withdrawal amount must be positive.");
        }

        Account account = accountsByIban.get(iban);

        if (account == null) {
            throw new IllegalArgumentException("Account not found.");
        }

        //sold
        account.withdraw(amount);

        //new obj withdraw
        Withdrawal withdrawal = new Withdrawal(generateTransactionId(),
                TransactionType.WITHDRAWAL,
                amount,
                LocalDateTime.now(),
                "Withdraw operation",
                account);

        //save
        transactionService.recordTransaction(withdrawal);
    }

    public void transfer(String ibanSource, String ibanDestination, double amount){
        if(ibanDestination == null || ibanSource == null){
            throw new IllegalArgumentException("IBAN cannot be null.");
        }

        if(amount <= 0){
            throw new IllegalArgumentException("Transfer amount must be positive.");
        }

        Account accountDestination = accountsByIban.get(ibanDestination);
        Account accountSource = accountsByIban.get(ibanSource);

        if (accountDestination == null || accountSource == null) {
            throw new IllegalArgumentException("Account not found.");
        }

        if (ibanSource.equals(ibanDestination)) {
            throw new IllegalArgumentException("Source and destination accounts must be different.");
        }

        //sold
        accountSource.withdraw(amount);
        accountDestination.deposit(amount);

        //new obj withdraw
        Transfer transfer = new Transfer(generateTransactionId(),
                TransactionType.TRANSFER,
                amount,
                LocalDateTime.now(),
                "Transfer operation",
                accountDestination,
                accountSource);

        //save
        transactionService.recordTransaction(transfer);
    }
}
