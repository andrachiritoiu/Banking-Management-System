package com.pao.project.bank.service;

import com.pao.project.bank.model.enums.TransactionType;
import com.pao.project.bank.model.transaction.Transaction;
import com.pao.project.bank.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class TransactionService {
    private static final TransactionService INSTANCE = new TransactionService();

    // etapa 1 - tranzactii tinute in memorie
    private final List<Transaction> transactions = new ArrayList<>();

    // etapa 2 - conexiune JDBC pentru interogari in baza de date
    private final Connection connection = DatabaseConnection.getInstance().getConnection();

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

    public List<Transaction> getTransactionsForAccount(String iban) {
        List<Transaction> result = new ArrayList<>();

        if (iban == null) {
            return result;
        }

        for (Transaction transaction : transactions) {
            if (transaction.involvesAccount(iban)) {
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

    // JOIN 1: toate transferurile cu IBAN sursa si IBAN destinatie
    public List<TransferReport> getAllTransfersWithAccountsJdbc() {
        List<TransferReport> result = new ArrayList<>();

        String sql = """
                SELECT
                    t.id,
                    t.amount,
                    t.`timestamp`,
                    t.description,
                    source_acc.iban AS source_iban,
                    destination_acc.iban AS destination_iban
                FROM transactions t
                JOIN transfer_transactions tt
                    ON t.id = tt.transaction_id
                JOIN accounts source_acc
                    ON tt.source_account_id = source_acc.id
                JOIN accounts destination_acc
                    ON tt.destination_account_id = destination_acc.id
                WHERE t.transaction_type = ?
                ORDER BY t.`timestamp` DESC
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, TransactionType.TRANSFER.name());

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    TransferReport transferReport = new TransferReport(
                            resultSet.getInt("id"),
                            resultSet.getDouble("amount"),
                            resultSet.getTimestamp("timestamp"),
                            resultSet.getString("description"),
                            resultSet.getString("source_iban"),
                            resultSet.getString("destination_iban")
                    );

                    result.add(transferReport);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Could not load transfers with account details.", e);
        }

        return result;
    }

    // JOIN 2: toate transferurile in care apare un anumit IBAN
    public List<TransferReport> getTransfersForAccountJdbc(String iban) {
        if (iban == null || iban.isBlank()) {
            throw new IllegalArgumentException("IBAN cannot be null or blank.");
        }

        List<TransferReport> result = new ArrayList<>();

        String sql = """
                SELECT
                    t.id,
                    t.amount,
                    t.`timestamp`,
                    t.description,
                    source_acc.iban AS source_iban,
                    destination_acc.iban AS destination_iban
                FROM transactions t
                JOIN transfer_transactions tt
                    ON t.id = tt.transaction_id
                JOIN accounts source_acc
                    ON tt.source_account_id = source_acc.id
                JOIN accounts destination_acc
                    ON tt.destination_account_id = destination_acc.id
                WHERE t.transaction_type = ?
                  AND (source_acc.iban = ? OR destination_acc.iban = ?)
                ORDER BY t.`timestamp` DESC
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, TransactionType.TRANSFER.name());
            statement.setString(2, iban);
            statement.setString(3, iban);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    TransferReport transferReport = new TransferReport(
                            resultSet.getInt("id"),
                            resultSet.getDouble("amount"),
                            resultSet.getTimestamp("timestamp"),
                            resultSet.getString("description"),
                            resultSet.getString("source_iban"),
                            resultSet.getString("destination_iban")
                    );

                    result.add(transferReport);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Could not load transfers for account.", e);
        }

        return result;
    }

    // JOIN 3: top conturi dupa numarul de transferuri trimise
    public List<AccountTransferSummary> getTopAccountsBySentTransfersJdbc(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive.");
        }

        List<AccountTransferSummary> result = new ArrayList<>();

        String sql = """
                SELECT
                    a.id AS account_id,
                    a.iban,
                    COUNT(tt.transaction_id) AS transfer_count,
                    COALESCE(SUM(t.amount), 0) AS total_sent
                FROM accounts a
                JOIN transfer_transactions tt
                    ON a.id = tt.source_account_id
                JOIN transactions t
                    ON tt.transaction_id = t.id
                WHERE t.transaction_type = ?
                GROUP BY a.id, a.iban
                ORDER BY transfer_count DESC, total_sent DESC
                LIMIT ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, TransactionType.TRANSFER.name());
            statement.setInt(2, limit);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    AccountTransferSummary summary = new AccountTransferSummary(
                            resultSet.getInt("account_id"),
                            resultSet.getString("iban"),
                            resultSet.getInt("transfer_count"),
                            resultSet.getDouble("total_sent")
                    );

                    result.add(summary);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Could not load top accounts by sent transfers.", e);
        }

        return result;
    }

    public static class TransferReport {
        private final int transactionId;
        private final double amount;
        private final Timestamp timestamp;
        private final String description;
        private final String sourceIban;
        private final String destinationIban;

        public TransferReport(
                int transactionId,
                double amount,
                Timestamp timestamp,
                String description,
                String sourceIban,
                String destinationIban
        ) {
            this.transactionId = transactionId;
            this.amount = amount;
            this.timestamp = timestamp;
            this.description = description;
            this.sourceIban = sourceIban;
            this.destinationIban = destinationIban;
        }

        public int getTransactionId() {
            return transactionId;
        }

        public double getAmount() {
            return amount;
        }

        public Timestamp getTimestamp() {
            return timestamp;
        }

        public String getDescription() {
            return description;
        }

        public String getSourceIban() {
            return sourceIban;
        }

        public String getDestinationIban() {
            return destinationIban;
        }

        @Override
        public String toString() {
            return "TransferReport{" +
                    "transactionId=" + transactionId +
                    ", amount=" + amount +
                    ", timestamp=" + timestamp +
                    ", description='" + description + '\'' +
                    ", sourceIban='" + sourceIban + '\'' +
                    ", destinationIban='" + destinationIban + '\'' +
                    '}';
        }
    }

    public static class AccountTransferSummary {
        private final int accountId;
        private final String iban;
        private final int transferCount;
        private final double totalSent;

        public AccountTransferSummary(
                int accountId,
                String iban,
                int transferCount,
                double totalSent
        ) {
            this.accountId = accountId;
            this.iban = iban;
            this.transferCount = transferCount;
            this.totalSent = totalSent;
        }

        public int getAccountId() {
            return accountId;
        }

        public String getIban() {
            return iban;
        }

        public int getTransferCount() {
            return transferCount;
        }

        public double getTotalSent() {
            return totalSent;
        }

        @Override
        public String toString() {
            return "AccountTransferSummary{" +
                    "accountId=" + accountId +
                    ", iban='" + iban + '\'' +
                    ", transferCount=" + transferCount +
                    ", totalSent=" + totalSent +
                    '}';
        }
    }
}