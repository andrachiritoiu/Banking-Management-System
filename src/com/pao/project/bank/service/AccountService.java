package com.pao.project.bank.service;

import com.pao.project.bank.model.AccountStatement;
import com.pao.project.bank.model.IBAN;
import com.pao.project.bank.model.account.CurrentAccount;
import com.pao.project.bank.model.account.SavingsAccount;
import com.pao.project.bank.model.account.Account;
import com.pao.project.bank.model.enums.Currency;
import com.pao.project.bank.model.enums.TransactionType;
import com.pao.project.bank.model.person.Client;
import com.pao.project.bank.model.transaction.Deposit;
import com.pao.project.bank.model.transaction.Exchange;
import com.pao.project.bank.model.transaction.Transaction;
import com.pao.project.bank.model.transaction.Transfer;
import com.pao.project.bank.model.transaction.Withdrawal;
import com.pao.project.bank.repository.person.CorporateClientRepository;
import com.pao.project.bank.repository.person.IndividualClientRepository;
import com.pao.project.bank.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AccountService {
    private static final AccountService INSTANCE = new AccountService();

    private final TransactionService transactionService = TransactionService.getInstance();
    private final IndividualClientRepository individualClientRepository = new IndividualClientRepository();
    private final CorporateClientRepository corporateClientRepository = new CorporateClientRepository();

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

    // etapa 2
    public void openAccountJdbc(Account account) {
        validateAccountForJdbc(account);

        String iban = account.getIban().getCode();

        try {
            if (accountExistsByIban(iban)) {
                throw new SQLException("An account with this IBAN already exists.");
            }

            insertAccountJdbc(account);
            System.out.println("Open account JDBC completed successfully.");
        } catch (SQLException e) {
            throw new RuntimeException("Open account JDBC failed: " + e.getMessage(), e);
        }
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

    // etapa 2
    public void closeAccountJdbc(String iban) {
        if (iban == null || iban.isBlank()) {
            throw new IllegalArgumentException("IBAN cannot be null or blank.");
        }

        try {
            connection.setAutoCommit(false);

            AccountCloseDbData account = getAccountForClose(iban);

            if (account.balance != 0) {
                throw new SQLException("Cannot close account with non-zero balance.");
            }

            if (!account.active) {
                throw new SQLException("Account is already closed.");
            }

            markAccountClosed(account.id);

            connection.commit();
            System.out.println("Close account JDBC completed successfully.");
        } catch (SQLException e) {
            try {
                connection.rollback();
                System.out.println("Close account JDBC failed. Rollback executed.");
            } catch (SQLException rollbackException) {
                throw new RuntimeException("Rollback failed.", rollbackException);
            }

            throw new RuntimeException("Close account JDBC failed: " + e.getMessage(), e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                throw new RuntimeException("Could not reset autoCommit.", e);
            }
        }
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

    // etapa 2
    public AccountStatement getAccountStatementJdbc(String iban, LocalDate startDate, LocalDate endDate) {
        if (iban == null || iban.isBlank()) {
            throw new IllegalArgumentException("IBAN cannot be null or blank.");
        }

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Date interval cannot contain null values.");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date.");
        }

        try {
            Account account = loadAccountByIbanJdbc(iban);
            List<Transaction> transactions = loadAccountTransactionsJdbc(account, startDate, endDate);

            double totalInflows = calculateJdbcTotalInflows(account, transactions);
            double totalOutflows = calculateJdbcTotalOutflows(account, transactions);

            AccountStatement statement = new AccountStatement(
                    account,
                    transactions,
                    totalInflows,
                    totalOutflows,
                    account.getBalance()
            );

            System.out.println(statement);
            return statement;
        } catch (SQLException e) {
            throw new RuntimeException("Account statement JDBC failed: " + e.getMessage(), e);
        }
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

    // etapa 2
    public void exchangeJdbc(String ibanSource, String ibanDestination, double sourceAmount, double exchangeRate) {
        if (ibanSource == null || ibanDestination == null) {
            throw new IllegalArgumentException("IBAN cannot be null.");
        }

        if (sourceAmount <= 0) {
            throw new IllegalArgumentException("Exchange amount must be positive.");
        }

        if (exchangeRate <= 0) {
            throw new IllegalArgumentException("Exchange rate must be positive.");
        }

        if (ibanSource.equals(ibanDestination)) {
            throw new IllegalArgumentException("Source and destination accounts must be different.");
        }

        try {
            connection.setAutoCommit(false);

            AccountDbData sourceAccount = getAccountForUpdate(ibanSource);
            AccountDbData destinationAccount = getAccountForUpdate(ibanDestination);

            Currency fromCurrency = parseCurrency(sourceAccount.currency);
            Currency toCurrency = parseCurrency(destinationAccount.currency);

            if (fromCurrency == toCurrency) {
                throw new SQLException("Exchange must be made between accounts with different currencies.");
            }

            if (sourceAccount.balance < sourceAmount) {
                throw new SQLException("Insufficient funds.");
            }

            double destinationAmount = sourceAmount * exchangeRate;

            updateAccountBalance(sourceAccount.id, -sourceAmount);
            updateAccountBalance(destinationAccount.id, destinationAmount);

            int transactionId = insertTransaction(
                    TransactionType.EXCHANGE,
                    sourceAmount,
                    "Exchange operation"
            );

            insertExchangeDetails(
                    transactionId,
                    sourceAccount.id,
                    destinationAccount.id,
                    destinationAmount,
                    fromCurrency,
                    toCurrency,
                    exchangeRate
            );

            connection.commit();
            System.out.println("Exchange JDBC completed successfully.");

        } catch (SQLException e) {
            try {
                connection.rollback();
                System.out.println("Exchange JDBC failed. Rollback executed.");
            } catch (SQLException rollbackException) {
                throw new RuntimeException("Rollback failed.", rollbackException);
            }

            throw new RuntimeException("Exchange JDBC failed: " + e.getMessage(), e);

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
                SELECT id, balance, currency
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
                            resultSet.getDouble("balance"),
                            resultSet.getString("currency")
                    );
                }
            }
        }

        throw new SQLException("Account not found for IBAN: " + iban);
    }

    private boolean accountExistsByIban(String iban) throws SQLException {
        String sql = """
                SELECT id
                FROM accounts
                WHERE iban = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, iban);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void insertAccountJdbc(Account account) throws SQLException {
        String sql = """
                INSERT INTO accounts (
                    id,
                    iban,
                    account_type,
                    balance,
                    currency,
                    active,
                    opening_date,
                    client_id
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, account.getId());
            statement.setString(2, account.getIban().getCode());
            statement.setString(3, account.getAccountType().name());
            statement.setDouble(4, account.getBalance());
            statement.setString(5, account.getCurrency());
            statement.setBoolean(6, account.isActive());
            statement.setDate(7, java.sql.Date.valueOf(account.getOpeningDate()));
            statement.setInt(8, account.getOwner().getId());

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Could not insert account.");
            }
        }
    }

    private AccountCloseDbData getAccountForClose(String iban) throws SQLException {
        String sql = """
                SELECT id, balance, active
                FROM accounts
                WHERE iban = ?
                FOR UPDATE
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, iban);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new AccountCloseDbData(
                            resultSet.getInt("id"),
                            resultSet.getDouble("balance"),
                            resultSet.getBoolean("active")
                    );
                }
            }
        }

        throw new SQLException("Account not found for IBAN: " + iban);
    }

    private Account loadAccountByIbanJdbc(String iban) throws SQLException {
        String sql = """
                SELECT
                    a.id,
                    a.iban,
                    a.account_type,
                    a.balance,
                    a.currency,
                    a.active,
                    a.opening_date,
                    a.client_id,
                    ca.monthly_fee,
                    sa.interest_rate,
                    sa.withdrawals_this_month
                FROM accounts a
                LEFT JOIN current_accounts ca ON a.id = ca.account_id
                LEFT JOIN savings_accounts sa ON a.id = sa.account_id
                WHERE a.iban = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, iban);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapAccountJdbc(resultSet);
                }
            }
        }

        throw new SQLException("Account not found for IBAN: " + iban);
    }

    private Account loadAccountByIdJdbc(int accountId) throws SQLException {
        String sql = """
                SELECT
                    a.id,
                    a.iban,
                    a.account_type,
                    a.balance,
                    a.currency,
                    a.active,
                    a.opening_date,
                    a.client_id,
                    ca.monthly_fee,
                    sa.interest_rate,
                    sa.withdrawals_this_month
                FROM accounts a
                LEFT JOIN current_accounts ca ON a.id = ca.account_id
                LEFT JOIN savings_accounts sa ON a.id = sa.account_id
                WHERE a.id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, accountId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapAccountJdbc(resultSet);
                }
            }
        }

        throw new SQLException("Account not found for id: " + accountId);
    }

    private Account mapAccountJdbc(ResultSet resultSet) throws SQLException {
        String accountType = resultSet.getString("account_type");
        Client owner = loadOwnerJdbc(resultSet.getInt("client_id"));

        if ("CURRENT".equals(accountType)) {
            return new CurrentAccount(
                    resultSet.getInt("id"),
                    new IBAN(resultSet.getString("iban")),
                    resultSet.getDouble("balance"),
                    resultSet.getString("currency"),
                    owner,
                    resultSet.getBoolean("active"),
                    resultSet.getDate("opening_date").toLocalDate(),
                    resultSet.getDouble("monthly_fee")
            );
        }

        if ("SAVINGS".equals(accountType)) {
            return new SavingsAccount(
                    resultSet.getInt("id"),
                    new IBAN(resultSet.getString("iban")),
                    resultSet.getDouble("balance"),
                    resultSet.getString("currency"),
                    owner,
                    resultSet.getBoolean("active"),
                    resultSet.getDate("opening_date").toLocalDate(),
                    resultSet.getDouble("interest_rate"),
                    resultSet.getInt("withdrawals_this_month")
            );
        }

        throw new SQLException("Unsupported account type: " + accountType);
    }

    private Client loadOwnerJdbc(int clientId) throws SQLException {
        Optional<? extends Client> individualClient = individualClientRepository.findById(clientId);
        if (individualClient.isPresent()) {
            return individualClient.get();
        }

        Optional<? extends Client> corporateClient = corporateClientRepository.findById(clientId);
        if (corporateClient.isPresent()) {
            return corporateClient.get();
        }

        throw new SQLException("Owner client not found for id: " + clientId);
    }

    private List<Transaction> loadAccountTransactionsJdbc(Account account, LocalDate startDate, LocalDate endDate) throws SQLException {
        String sql = """
                SELECT
                    t.id,
                    t.transaction_type,
                    t.amount,
                    t.`timestamp`,
                    t.description,
                    dt.destination_account_id AS deposit_destination_account_id,
                    wt.source_account_id AS withdrawal_source_account_id,
                    tt.source_account_id AS transfer_source_account_id,
                    tt.destination_account_id AS transfer_destination_account_id,
                    et.source_account_id AS exchange_source_account_id,
                    et.destination_account_id AS exchange_destination_account_id,
                    et.destination_amount,
                    et.from_currency,
                    et.to_currency,
                    et.exchange_rate
                FROM transactions t
                LEFT JOIN deposit_transactions dt ON t.id = dt.transaction_id
                LEFT JOIN withdrawal_transactions wt ON t.id = wt.transaction_id
                LEFT JOIN transfer_transactions tt ON t.id = tt.transaction_id
                LEFT JOIN exchange_transactions et ON t.id = et.transaction_id
                WHERE t.`timestamp` >= ?
                  AND t.`timestamp` < ?
                  AND (
                        dt.destination_account_id = ?
                        OR wt.source_account_id = ?
                        OR tt.source_account_id = ?
                        OR tt.destination_account_id = ?
                        OR et.source_account_id = ?
                        OR et.destination_account_id = ?
                  )
                ORDER BY t.`timestamp`, t.id
                """;

        Timestamp startTimestamp = Timestamp.valueOf(startDate.atStartOfDay());
        Timestamp endTimestamp = Timestamp.valueOf(endDate.plusDays(1).atStartOfDay());
        List<Transaction> transactions = new ArrayList<>();
        int accountId = account.getId();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, startTimestamp);
            statement.setTimestamp(2, endTimestamp);
            statement.setInt(3, accountId);
            statement.setInt(4, accountId);
            statement.setInt(5, accountId);
            statement.setInt(6, accountId);
            statement.setInt(7, accountId);
            statement.setInt(8, accountId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    transactions.add(mapTransactionJdbc(resultSet));
                }
            }
        }

        return transactions;
    }

    private Transaction mapTransactionJdbc(ResultSet resultSet) throws SQLException {
        TransactionType transactionType = TransactionType.valueOf(resultSet.getString("transaction_type"));
        int transactionId = resultSet.getInt("id");
        double amount = resultSet.getDouble("amount");
        LocalDateTime timestamp = resultSet.getTimestamp("timestamp").toLocalDateTime();
        String description = resultSet.getString("description");

        return switch (transactionType) {
            case DEPOSIT -> new Deposit(
                    transactionId,
                    transactionType,
                    amount,
                    timestamp,
                    description,
                    loadAccountByIdJdbc(resultSet.getInt("deposit_destination_account_id"))
            );
            case WITHDRAWAL -> new Withdrawal(
                    transactionId,
                    transactionType,
                    amount,
                    timestamp,
                    description,
                    loadAccountByIdJdbc(resultSet.getInt("withdrawal_source_account_id"))
            );
            case TRANSFER -> new Transfer(
                    transactionId,
                    transactionType,
                    amount,
                    timestamp,
                    description,
                    loadAccountByIdJdbc(resultSet.getInt("transfer_destination_account_id")),
                    loadAccountByIdJdbc(resultSet.getInt("transfer_source_account_id"))
            );
            case EXCHANGE -> new Exchange(
                    transactionId,
                    loadAccountByIdJdbc(resultSet.getInt("exchange_source_account_id")),
                    loadAccountByIdJdbc(resultSet.getInt("exchange_destination_account_id")),
                    amount,
                    resultSet.getDouble("destination_amount"),
                    parseCurrency(resultSet.getString("from_currency")),
                    parseCurrency(resultSet.getString("to_currency")),
                    resultSet.getDouble("exchange_rate"),
                    timestamp,
                    description
            );
        };
    }

    private double calculateJdbcTotalInflows(Account account, List<Transaction> transactions) {
        double total = 0;

        for (Transaction transaction : transactions) {
            if (transaction instanceof Deposit deposit && deposit.getDestinationAccount().equals(account)) {
                total += deposit.getAmount();
            }

            if (transaction instanceof Transfer transfer && transfer.getDestinationAccount().equals(account)) {
                total += transfer.getAmount();
            }

            if (transaction instanceof Exchange exchange && exchange.getDestinationAccount().equals(account)) {
                total += exchange.getDestinationAmount();
            }
        }

        return total;
    }

    private double calculateJdbcTotalOutflows(Account account, List<Transaction> transactions) {
        double total = 0;

        for (Transaction transaction : transactions) {
            if (transaction instanceof Withdrawal withdrawal && withdrawal.getSourceAccount().equals(account)) {
                total += withdrawal.getAmount();
            }

            if (transaction instanceof Transfer transfer && transfer.getSourceAccount().equals(account)) {
                total += transfer.getAmount();
            }

            if (transaction instanceof Exchange exchange && exchange.getSourceAccount().equals(account)) {
                total += exchange.getSourceAmount();
            }
        }

        return total;
    }

    private void markAccountClosed(int accountId) throws SQLException {
        String sql = """
                UPDATE accounts
                SET active = false
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, accountId);

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Could not close account with id: " + accountId);
            }
        }
    }

    private void validateAccountForJdbc(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null.");
        }

        if (account.getId() <= 0) {
            throw new IllegalArgumentException("Account id must be positive.");
        }

        if (account.getIban() == null || account.getIban().getCode() == null || account.getIban().getCode().isBlank()) {
            throw new IllegalArgumentException("IBAN cannot be null or blank.");
        }

        if (account.getAccountType() == null) {
            throw new IllegalArgumentException("Account type cannot be null.");
        }

        if (account.getBalance() < 0) {
            throw new IllegalArgumentException("Balance cannot be negative.");
        }

        if (account.getCurrency() == null || account.getCurrency().isBlank()) {
            throw new IllegalArgumentException("Currency cannot be null or blank.");
        }

        if (account.getOpeningDate() == null) {
            throw new IllegalArgumentException("Opening date cannot be null.");
        }

        if (account.getOwner() == null) {
            throw new IllegalArgumentException("Account owner cannot be null.");
        }

        if (account.getOwner().getId() <= 0) {
            throw new IllegalArgumentException("Account owner id must be positive.");
        }
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

    private void insertExchangeDetails(
            int transactionId,
            int sourceAccountId,
            int destinationAccountId,
            double destinationAmount,
            Currency fromCurrency,
            Currency toCurrency,
            double exchangeRate
    ) throws SQLException {
        String sql = """
                INSERT INTO exchange_transactions (
                    transaction_id,
                    source_account_id,
                    destination_account_id,
                    destination_amount,
                    from_currency,
                    to_currency,
                    exchange_rate
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, transactionId);
            statement.setInt(2, sourceAccountId);
            statement.setInt(3, destinationAccountId);
            statement.setDouble(4, destinationAmount);
            statement.setString(5, fromCurrency.name());
            statement.setString(6, toCurrency.name());
            statement.setDouble(7, exchangeRate);

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Could not insert exchange details.");
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
        private final String currency;

        private AccountDbData(int id, double balance, String currency) {
            this.id = id;
            this.balance = balance;
            this.currency = currency;
        }
    }

    private static class AccountCloseDbData {
        private final int id;
        private final double balance;
        private final boolean active;

        private AccountCloseDbData(int id, double balance, boolean active) {
            this.id = id;
            this.balance = balance;
            this.active = active;
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
