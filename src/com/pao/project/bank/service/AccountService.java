package com.pao.project.bank.service;

import com.pao.project.bank.model.account.Account;
import com.pao.project.bank.model.enums.Currency;
import com.pao.project.bank.model.enums.TransactionType;
import com.pao.project.bank.model.person.Client;
import com.pao.project.bank.model.transaction.Deposit;
import com.pao.project.bank.model.transaction.Exchange;
import com.pao.project.bank.model.transaction.Transfer;
import com.pao.project.bank.model.transaction.Withdrawal;
import com.pao.project.bank.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountService {
    private static final AccountService INSTANCE = new AccountService();

    private final TransactionService transactionService = TransactionService.getInstance();

    // CONEXIUNE JDBC PENTRU ETAPA 2
    private final Connection connection = DatabaseConnection.getInstance().getConnection();

    private int transactionIdCounter = 1;
    private final List<Account> accounts = new ArrayList<>();
    private final Map<String, Account> accountsByIban = new HashMap<>();
    private final Map<String, String> ibanAliases = new HashMap<>();

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

    public Account findByIban(String iban) {
        if (iban == null) {
            throw new IllegalArgumentException("IBAN cannot be null.");
        }

        return accountsByIban.get(iban);
    }

    public void setAlias(String alias, String iban) {
        if (alias == null || alias.isBlank()) {
            throw new IllegalArgumentException("Alias cannot be null or blank.");
        }

        if (iban == null) {
            throw new IllegalArgumentException("IBAN cannot be null.");
        }

        if (!accountsByIban.containsKey(iban)) {
            throw new IllegalArgumentException("Account not found.");
        }

        ibanAliases.put(normalizeAlias(alias), iban);
    }

    public Account findByAlias(String alias) {
        if (alias == null || alias.isBlank()) {
            throw new IllegalArgumentException("Alias cannot be null or blank.");
        }

        String iban = ibanAliases.get(normalizeAlias(alias));

        if (iban == null) {
            return null;
        }

        return accountsByIban.get(iban);
    }

    public void transferByAlias(String sourceIban, String alias, double amount) {
        Account destinationAccount = findByAlias(alias);

        if (destinationAccount == null) {
            throw new IllegalArgumentException("Alias not found.");
        }

        transfer(sourceIban, destinationAccount.getIban().getCode(), amount);
    }

    public Map<String, String> getIbanAliases() {
        return new HashMap<>(ibanAliases);
    }

    public void closeAccount(String iban) {
        if (iban == null) {
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

    public void deposit(String iban, double amount) {
        if (iban == null) {
            throw new IllegalArgumentException("IBAN cannot be null.");
        }

        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive.");
        }

        Account account = accountsByIban.get(iban);

        if (account == null) {
            throw new IllegalArgumentException("Account not found.");
        }

        account.deposit(amount);

        Deposit deposit = new Deposit(
                generateTransactionId(),
                TransactionType.DEPOSIT,
                amount,
                LocalDateTime.now(),
                "Deposit operation",
                account
        );

        transactionService.recordTransaction(deposit);
    }

    // etapa 2
    public void depositJdbc(String iban, double amount) {
        if (iban == null) {
            throw new IllegalArgumentException("IBAN cannot be null.");
        }

        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive.");
        }

        try {
            connection.setAutoCommit(false);

            AccountDbData account = getAccountForUpdate(iban);

            updateAccountBalance(account.id, amount);

            int transactionId = insertTransaction(
                    TransactionType.DEPOSIT,
                    amount,
                    "Deposit operation"
            );

            insertDepositDetails(transactionId, account.id);

            connection.commit();
            System.out.println("Deposit JDBC completed successfully.");

        } catch (SQLException e) {
            try {
                connection.rollback();
                System.out.println("Deposit JDBC failed. Rollback executed.");
            } catch (SQLException rollbackException) {
                throw new RuntimeException("Rollback failed.", rollbackException);
            }

            throw new RuntimeException("Deposit JDBC failed: " + e.getMessage(), e);

        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                throw new RuntimeException("Could not reset autoCommit.", e);
            }
        }
    }

    public void withdraw(String iban, double amount) {
        if (iban == null) {
            throw new IllegalArgumentException("IBAN cannot be null.");
        }

        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive.");
        }

        Account account = accountsByIban.get(iban);

        if (account == null) {
            throw new IllegalArgumentException("Account not found.");
        }

        account.withdraw(amount);

        Withdrawal withdrawal = new Withdrawal(
                generateTransactionId(),
                TransactionType.WITHDRAWAL,
                amount,
                LocalDateTime.now(),
                "Withdraw operation",
                account
        );

        transactionService.recordTransaction(withdrawal);
    }

    // etapa 2
    public void withdrawJdbc(String iban, double amount) {
        if (iban == null) {
            throw new IllegalArgumentException("IBAN cannot be null.");
        }

        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive.");
        }

        try {
            connection.setAutoCommit(false);

            AccountDbData account = getAccountForUpdate(iban);

            if (account.balance < amount) {
                throw new SQLException("Insufficient funds.");
            }

            updateAccountBalance(account.id, -amount);

            int transactionId = insertTransaction(
                    TransactionType.WITHDRAWAL,
                    amount,
                    "Withdraw operation"
            );

            insertWithdrawalDetails(transactionId, account.id);

            connection.commit();
            System.out.println("Withdrawal JDBC completed successfully.");

        } catch (SQLException e) {
            try {
                connection.rollback();
                System.out.println("Withdrawal JDBC failed. Rollback executed.");
            } catch (SQLException rollbackException) {
                throw new RuntimeException("Rollback failed.", rollbackException);
            }

            throw new RuntimeException("Withdrawal JDBC failed: " + e.getMessage(), e);

        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                throw new RuntimeException("Could not reset autoCommit.", e);
            }
        }
    }

    // etapa 1
    public void transfer(String ibanSource, String ibanDestination, double amount) {
        if (ibanDestination == null || ibanSource == null) {
            throw new IllegalArgumentException("IBAN cannot be null.");
        }

        if (amount <= 0) {
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

        accountSource.withdraw(amount);
        accountDestination.deposit(amount);

        Transfer transfer = new Transfer(
                generateTransactionId(),
                TransactionType.TRANSFER,
                amount,
                LocalDateTime.now(),
                "Transfer operation",
                accountDestination,
                accountSource
        );

        transactionService.recordTransaction(transfer);
    }

    // etapa 2
    public void transferJdbc(String ibanSource, String ibanDestination, double amount) {
        if (ibanSource == null || ibanDestination == null) {
            throw new IllegalArgumentException("IBAN cannot be null.");
        }

        if (amount <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive.");
        }

        if (ibanSource.equals(ibanDestination)) {
            throw new IllegalArgumentException("Source and destination accounts must be different.");
        }

        try {
            connection.setAutoCommit(false);

            AccountDbData sourceAccount = getAccountForUpdate(ibanSource);
            AccountDbData destinationAccount = getAccountForUpdate(ibanDestination);

            if (sourceAccount.balance < amount) {
                throw new SQLException("Insufficient funds.");
            }

            updateAccountBalance(sourceAccount.id, -amount);
            updateAccountBalance(destinationAccount.id, amount);

            int transactionId = insertTransaction(
                    TransactionType.TRANSFER,
                    amount,
                    "Transfer operation"
            );

            insertTransferDetails(
                    transactionId,
                    sourceAccount.id,
                    destinationAccount.id
            );

            connection.commit();
            System.out.println("Transfer JDBC completed successfully.");

        } catch (SQLException e) {
            try {
                connection.rollback();
                System.out.println("Transfer JDBC failed. Rollback executed.");
            } catch (SQLException rollbackException) {
                throw new RuntimeException("Rollback failed.", rollbackException);
            }

            throw new RuntimeException("Transfer JDBC failed: " + e.getMessage(), e);

        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                throw new RuntimeException("Could not reset autoCommit.", e);
            }
        }
    }

    private AccountDbData getAccountForUpdate(String iban) throws SQLException {
        String sql = """
                SELECT id, balance
                FROM accounts
                WHERE iban = ?
                FOR UPDATE
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, iban);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new AccountDbData(
                            resultSet.getInt("id"),
                            resultSet.getDouble("balance")
                    );
                }
            }
        }

        throw new SQLException("Account not found for IBAN: " + iban);
    }

    private void updateAccountBalance(int accountId, double amountChange) throws SQLException {
        String sql = """
                UPDATE accounts
                SET balance = balance + ?
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDouble(1, amountChange);
            statement.setInt(2, accountId);

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Could not update account with id: " + accountId);
            }
        }
    }

    private int insertTransaction(TransactionType transactionType, double amount, String description) throws SQLException {
        String sql = """
                INSERT INTO transactions (
                    transaction_type,
                    amount,
                    `timestamp`,
                    description
                )
                VALUES (?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, transactionType.name());
            statement.setDouble(2, amount);
            statement.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            statement.setString(4, description);

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Could not insert transaction.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }

        throw new SQLException("Could not get generated transaction id.");
    }

    private void insertDepositDetails(int transactionId, int destinationAccountId) throws SQLException {
        String sql = """
                INSERT INTO deposit_transactions (
                    transaction_id,
                    destination_account_id
                )
                VALUES (?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, transactionId);
            statement.setInt(2, destinationAccountId);

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Could not insert deposit details.");
            }
        }
    }

    private void insertWithdrawalDetails(int transactionId, int sourceAccountId) throws SQLException {
        String sql = """
                INSERT INTO withdrawal_transactions (
                    transaction_id,
                    source_account_id
                )
                VALUES (?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, transactionId);
            statement.setInt(2, sourceAccountId);

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Could not insert withdrawal details.");
            }
        }
    }

    private void insertTransferDetails(
            int transactionId,
            int sourceAccountId,
            int destinationAccountId
    ) throws SQLException {
        String sql = """
                INSERT INTO transfer_transactions (
                    transaction_id,
                    source_account_id,
                    destination_account_id
                )
                VALUES (?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, transactionId);
            statement.setInt(2, sourceAccountId);
            statement.setInt(3, destinationAccountId);

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Could not insert transfer details.");
            }
        }
    }

    private static class AccountDbData {
        private final int id;
        private final double balance;

        private AccountDbData(int id, double balance) {
            this.id = id;
            this.balance = balance;
        }
    }

    public Exchange exchange(String ibanSource, String ibanDestination, double sourceAmount, double exchangeRate) {
        if (ibanSource == null || ibanDestination == null) {
            throw new IllegalArgumentException("IBAN cannot be null.");
        }

        if (sourceAmount <= 0) {
            throw new IllegalArgumentException("Exchange amount must be positive.");
        }

        if (exchangeRate <= 0) {
            throw new IllegalArgumentException("Exchange rate must be positive.");
        }

        Account accountSource = accountsByIban.get(ibanSource);
        Account accountDestination = accountsByIban.get(ibanDestination);

        if (accountSource == null || accountDestination == null) {
            throw new IllegalArgumentException("Account not found.");
        }

        if (ibanSource.equals(ibanDestination)) {
            throw new IllegalArgumentException("Source and destination accounts must be different.");
        }

        Currency fromCurrency = parseCurrency(accountSource.getCurrency());
        Currency toCurrency = parseCurrency(accountDestination.getCurrency());

        if (fromCurrency == toCurrency) {
            throw new IllegalArgumentException("Exchange must be made between accounts with different currencies.");
        }

        double destinationAmount = sourceAmount * exchangeRate;

        accountSource.withdraw(sourceAmount);
        accountDestination.deposit(destinationAmount);

        Exchange exchange = new Exchange(
                generateTransactionId(),
                accountSource,
                accountDestination,
                sourceAmount,
                destinationAmount,
                fromCurrency,
                toCurrency,
                exchangeRate,
                LocalDateTime.now(),
                "Exchange operation"
        );

        transactionService.recordTransaction(exchange);

        return exchange;
    }

    private Currency parseCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency cannot be null.");
        }

        try {
            return Currency.valueOf(currency.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported currency: " + currency);
        }
    }

    private String normalizeAlias(String alias) {
        return alias.trim().toLowerCase();
    }
}
