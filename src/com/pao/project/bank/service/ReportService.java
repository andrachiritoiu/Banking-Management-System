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
import com.pao.project.bank.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
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

    private Connection getConnection() {
        return DatabaseConnection.getInstance().getConnection();
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


    public List<ClientAccountReport> getClientsWithAccountsJdbc() {
        String sql = """
                SELECT
                    c.id AS client_id,
                    COALESCE(CONCAT(ic.first_name, ' ', ic.last_name), cc.company_name) AS client_name,
                    a.id AS account_id,
                    a.iban,
                    a.balance,
                    a.currency
                FROM clients c
                LEFT JOIN individual_clients ic ON c.id = ic.client_id
                LEFT JOIN corporate_clients cc ON c.id = cc.client_id
                JOIN accounts a ON c.id = a.client_id
                ORDER BY c.id, a.iban
                """;

        List<ClientAccountReport> result = new ArrayList<>();

        try (
                PreparedStatement statement = getConnection().prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                result.add(new ClientAccountReport(
                        resultSet.getInt("client_id"),
                        resultSet.getString("client_name"),
                        resultSet.getInt("account_id"),
                        resultSet.getString("iban"),
                        resultSet.getDouble("balance"),
                        resultSet.getString("currency")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not load clients with accounts.", e);
        }

        return result;
    }

    public List<ClientTransferReport> getTransferHistoryForClientJdbc(int clientId) {
        if (clientId <= 0) {
            throw new IllegalArgumentException("Client id must be positive.");
        }

        String sql = """
                SELECT DISTINCT
                    c.id AS client_id,
                    COALESCE(CONCAT(ic.first_name, ' ', ic.last_name), cc.company_name) AS client_name,
                    t.id AS transaction_id,
                    t.amount,
                    t.`timestamp`,
                    source_acc.iban AS source_iban,
                    destination_acc.iban AS destination_iban
                FROM clients c
                LEFT JOIN individual_clients ic ON c.id = ic.client_id
                LEFT JOIN corporate_clients cc ON c.id = cc.client_id
                JOIN accounts client_acc ON c.id = client_acc.client_id
                JOIN transfer_transactions tt
                    ON client_acc.id = tt.source_account_id
                    OR client_acc.id = tt.destination_account_id
                JOIN transactions t ON tt.transaction_id = t.id
                JOIN accounts source_acc ON tt.source_account_id = source_acc.id
                JOIN accounts destination_acc ON tt.destination_account_id = destination_acc.id
                WHERE c.id = ?
                ORDER BY t.`timestamp` DESC
                """;

        List<ClientTransferReport> result = new ArrayList<>();

        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, clientId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(new ClientTransferReport(
                            resultSet.getInt("client_id"),
                            resultSet.getString("client_name"),
                            resultSet.getInt("transaction_id"),
                            resultSet.getDouble("amount"),
                            resultSet.getTimestamp("timestamp"),
                            resultSet.getString("source_iban"),
                            resultSet.getString("destination_iban")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not load transfer history for client.", e);
        }

        return result;
    }

    public List<TopClientTransferReport> getTopClientsByTransferredAmountJdbc(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive.");
        }

        String sql = """
                SELECT
                    c.id AS client_id,
                    COALESCE(CONCAT(ic.first_name, ' ', ic.last_name), cc.company_name) AS client_name,
                    COUNT(t.id) AS transfer_count,
                    SUM(t.amount) AS total_sent
                FROM clients c
                LEFT JOIN individual_clients ic ON c.id = ic.client_id
                LEFT JOIN corporate_clients cc ON c.id = cc.client_id
                JOIN accounts a ON c.id = a.client_id
                JOIN transfer_transactions tt ON a.id = tt.source_account_id
                JOIN transactions t ON tt.transaction_id = t.id
                WHERE t.transaction_type = 'TRANSFER'
                GROUP BY c.id, client_name
                ORDER BY total_sent DESC
                LIMIT ?
                """;

        List<TopClientTransferReport> result = new ArrayList<>();

        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, limit);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(new TopClientTransferReport(
                            resultSet.getInt("client_id"),
                            resultSet.getString("client_name"),
                            resultSet.getInt("transfer_count"),
                            resultSet.getDouble("total_sent")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not load top clients by transferred amount.", e);
        }

        return result;
    }

    public List<AccountTransferStatementReport> getAccountStatementJdbc(String iban) {
        if (iban == null || iban.isBlank()) {
            throw new IllegalArgumentException("IBAN cannot be null or blank.");
        }

        String sql = """
                SELECT
                    t.id AS transaction_id,
                    t.transaction_type,
                    t.amount,
                    t.`timestamp`,
                    t.description,
                    source_acc.iban AS source_iban,
                    destination_acc.iban AS destination_iban,
                    CASE
                        WHEN source_acc.iban = ? THEN 'OUTGOING'
                        WHEN destination_acc.iban = ? THEN 'INCOMING'
                    END AS direction
                FROM transactions t
                JOIN transfer_transactions tt ON t.id = tt.transaction_id
                JOIN accounts source_acc ON tt.source_account_id = source_acc.id
                JOIN accounts destination_acc ON tt.destination_account_id = destination_acc.id
                WHERE source_acc.iban = ? OR destination_acc.iban = ?
                ORDER BY t.`timestamp` DESC
                """;

        List<AccountTransferStatementReport> result = new ArrayList<>();

        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, iban);
            statement.setString(2, iban);
            statement.setString(3, iban);
            statement.setString(4, iban);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(new AccountTransferStatementReport(
                            resultSet.getInt("transaction_id"),
                            resultSet.getString("transaction_type"),
                            resultSet.getDouble("amount"),
                            resultSet.getTimestamp("timestamp"),
                            resultSet.getString("description"),
                            resultSet.getString("source_iban"),
                            resultSet.getString("destination_iban"),
                            resultSet.getString("direction")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not load account statement.", e);
        }

        return result;
    }

    public List<AccountWithoutTransferReport> getAccountsWithoutTransfersJdbc() {
        String sql = """
                SELECT
                    a.id,
                    a.iban,
                    a.balance,
                    a.currency
                FROM accounts a
                LEFT JOIN transfer_transactions sent ON a.id = sent.source_account_id
                LEFT JOIN transfer_transactions received ON a.id = received.destination_account_id
                WHERE sent.transaction_id IS NULL
                  AND received.transaction_id IS NULL
                ORDER BY a.iban
                """;

        List<AccountWithoutTransferReport> result = new ArrayList<>();

        try (
                PreparedStatement statement = getConnection().prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                result.add(new AccountWithoutTransferReport(
                        resultSet.getInt("id"),
                        resultSet.getString("iban"),
                        resultSet.getDouble("balance"),
                        resultSet.getString("currency")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not load accounts without transfers.", e);
        }

        return result;
    }

    public List<ClientBalanceSummaryReport> getClientBalanceSummaryJdbc() {
        String sql = """
                SELECT
                    c.id AS client_id,
                    COALESCE(CONCAT(ic.first_name, ' ', ic.last_name), cc.company_name) AS client_name,
                    COUNT(a.id) AS account_count,
                    COALESCE(SUM(a.balance), 0) AS total_balance
                FROM clients c
                LEFT JOIN individual_clients ic ON c.id = ic.client_id
                LEFT JOIN corporate_clients cc ON c.id = cc.client_id
                LEFT JOIN accounts a ON c.id = a.client_id
                GROUP BY c.id, client_name
                ORDER BY total_balance DESC
                """;

        List<ClientBalanceSummaryReport> result = new ArrayList<>();

        try (
                PreparedStatement statement = getConnection().prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                result.add(new ClientBalanceSummaryReport(
                        resultSet.getInt("client_id"),
                        resultSet.getString("client_name"),
                        resultSet.getInt("account_count"),
                        resultSet.getDouble("total_balance")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not load client balance summary.", e);
        }

        return result;
    }

    public List<ActiveCreditReport> getActiveCreditsWithClientDetailsJdbc() {
        String sql = """
                SELECT
                    c.id AS client_id,
                    COALESCE(CONCAT(ic.first_name, ' ', ic.last_name), cc.company_name) AS client_name,
                    cr.id AS credit_id,
                    cr.credit_type,
                    cr.principal_amount,
                    cr.remaining_amount,
                    cr.status,
                    a.iban AS payment_account_iban
                FROM clients c
                LEFT JOIN individual_clients ic ON c.id = ic.client_id
                LEFT JOIN corporate_clients cc ON c.id = cc.client_id
                JOIN credits cr ON c.id = cr.borrower_id
                JOIN accounts a ON cr.target_account_id = a.id
                WHERE cr.status = 'ACTIVE'
                ORDER BY c.id, cr.id
                """;

        List<ActiveCreditReport> result = new ArrayList<>();

        try (
                PreparedStatement statement = getConnection().prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                result.add(new ActiveCreditReport(
                        resultSet.getInt("client_id"),
                        resultSet.getString("client_name"),
                        resultSet.getInt("credit_id"),
                        resultSet.getString("credit_type"),
                        resultSet.getDouble("principal_amount"),
                        resultSet.getDouble("remaining_amount"),
                        resultSet.getString("status"),
                        resultSet.getString("payment_account_iban")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not load active credits with client details.", e);
        }

        return result;
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

    public static class ClientAccountReport {
        private final int clientId;
        private final String clientName;
        private final int accountId;
        private final String iban;
        private final double balance;
        private final String currency;

        public ClientAccountReport(int clientId, String clientName, int accountId, String iban, double balance, String currency) {
            this.clientId = clientId;
            this.clientName = clientName;
            this.accountId = accountId;
            this.iban = iban;
            this.balance = balance;
            this.currency = currency;
        }

        @Override
        public String toString() {
            return "ClientAccountReport{" +
                    "clientId=" + clientId +
                    ", clientName='" + clientName + '\'' +
                    ", accountId=" + accountId +
                    ", iban='" + iban + '\'' +
                    ", balance=" + balance +
                    ", currency='" + currency + '\'' +
                    '}';
        }
    }

    public static class ClientTransferReport {
        private final int clientId;
        private final String clientName;
        private final int transactionId;
        private final double amount;
        private final Timestamp timestamp;
        private final String sourceIban;
        private final String destinationIban;

        public ClientTransferReport(int clientId, String clientName, int transactionId, double amount, Timestamp timestamp, String sourceIban, String destinationIban) {
            this.clientId = clientId;
            this.clientName = clientName;
            this.transactionId = transactionId;
            this.amount = amount;
            this.timestamp = timestamp;
            this.sourceIban = sourceIban;
            this.destinationIban = destinationIban;
        }

        @Override
        public String toString() {
            return "ClientTransferReport{" +
                    "clientId=" + clientId +
                    ", clientName='" + clientName + '\'' +
                    ", transactionId=" + transactionId +
                    ", amount=" + amount +
                    ", timestamp=" + timestamp +
                    ", sourceIban='" + sourceIban + '\'' +
                    ", destinationIban='" + destinationIban + '\'' +
                    '}';
        }
    }

    public static class TopClientTransferReport {
        private final int clientId;
        private final String clientName;
        private final int transferCount;
        private final double totalSent;

        public TopClientTransferReport(int clientId, String clientName, int transferCount, double totalSent) {
            this.clientId = clientId;
            this.clientName = clientName;
            this.transferCount = transferCount;
            this.totalSent = totalSent;
        }

        @Override
        public String toString() {
            return "TopClientTransferReport{" +
                    "clientId=" + clientId +
                    ", clientName='" + clientName + '\'' +
                    ", transferCount=" + transferCount +
                    ", totalSent=" + totalSent +
                    '}';
        }
    }

    public static class AccountTransferStatementReport {
        private final int transactionId;
        private final String transactionType;
        private final double amount;
        private final Timestamp timestamp;
        private final String description;
        private final String sourceIban;
        private final String destinationIban;
        private final String direction;

        public AccountTransferStatementReport(int transactionId, String transactionType, double amount, Timestamp timestamp, String description, String sourceIban, String destinationIban, String direction) {
            this.transactionId = transactionId;
            this.transactionType = transactionType;
            this.amount = amount;
            this.timestamp = timestamp;
            this.description = description;
            this.sourceIban = sourceIban;
            this.destinationIban = destinationIban;
            this.direction = direction;
        }

        @Override
        public String toString() {
            return "AccountTransferStatementReport{" +
                    "transactionId=" + transactionId +
                    ", transactionType='" + transactionType + '\'' +
                    ", amount=" + amount +
                    ", timestamp=" + timestamp +
                    ", description='" + description + '\'' +
                    ", sourceIban='" + sourceIban + '\'' +
                    ", destinationIban='" + destinationIban + '\'' +
                    ", direction='" + direction + '\'' +
                    '}';
        }
    }

    public static class AccountWithoutTransferReport {
        private final int accountId;
        private final String iban;
        private final double balance;
        private final String currency;

        public AccountWithoutTransferReport(int accountId, String iban, double balance, String currency) {
            this.accountId = accountId;
            this.iban = iban;
            this.balance = balance;
            this.currency = currency;
        }

        @Override
        public String toString() {
            return "AccountWithoutTransferReport{" +
                    "accountId=" + accountId +
                    ", iban='" + iban + '\'' +
                    ", balance=" + balance +
                    ", currency='" + currency + '\'' +
                    '}';
        }
    }

    public static class ClientBalanceSummaryReport {
        private final int clientId;
        private final String clientName;
        private final int accountCount;
        private final double totalBalance;

        public ClientBalanceSummaryReport(int clientId, String clientName, int accountCount, double totalBalance) {
            this.clientId = clientId;
            this.clientName = clientName;
            this.accountCount = accountCount;
            this.totalBalance = totalBalance;
        }

        @Override
        public String toString() {
            return "ClientBalanceSummaryReport{" +
                    "clientId=" + clientId +
                    ", clientName='" + clientName + '\'' +
                    ", accountCount=" + accountCount +
                    ", totalBalance=" + totalBalance +
                    '}';
        }
    }

    public static class ActiveCreditReport {
        private final int clientId;
        private final String clientName;
        private final int creditId;
        private final String creditType;
        private final double principalAmount;
        private final double remainingAmount;
        private final String status;
        private final String paymentAccountIban;

        public ActiveCreditReport(int clientId, String clientName, int creditId, String creditType, double principalAmount, double remainingAmount, String status, String paymentAccountIban) {
            this.clientId = clientId;
            this.clientName = clientName;
            this.creditId = creditId;
            this.creditType = creditType;
            this.principalAmount = principalAmount;
            this.remainingAmount = remainingAmount;
            this.status = status;
            this.paymentAccountIban = paymentAccountIban;
        }

        @Override
        public String toString() {
            return "ActiveCreditReport{" +
                    "clientId=" + clientId +
                    ", clientName='" + clientName + '\'' +
                    ", creditId=" + creditId +
                    ", creditType='" + creditType + '\'' +
                    ", principalAmount=" + principalAmount +
                    ", remainingAmount=" + remainingAmount +
                    ", status='" + status + '\'' +
                    ", paymentAccountIban='" + paymentAccountIban + '\'' +
                    '}';
        }
    }
}
