package com.pao.project.bank.repository.helper;

import com.pao.project.bank.model.transaction.Transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

public final class TransactionRepositoryHelper {
    public void insertTransaction(Connection connection, Transaction transaction) throws SQLException {
        validateTransaction(transaction);

        String sql = """
                INSERT INTO transactions (
                    id,
                    transaction_type,
                    amount,
                    `timestamp`,
                    description
                )
                VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, transaction.getId());
            statement.setString(2, transaction.getType().name());
            statement.setDouble(3, transaction.getAmount());
            statement.setTimestamp(4, Timestamp.valueOf(transaction.getTimestamp()));
            statement.setString(5, transaction.getDescription());

            statement.executeUpdate();
        }
    }

    public void updateTransaction(Connection connection, Transaction transaction) throws SQLException {
        validateTransaction(transaction);

        String sql = """
                UPDATE transactions
                SET transaction_type = ?,
                    amount = ?,
                    `timestamp` = ?,
                    description = ?
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, transaction.getType().name());
            statement.setDouble(2, transaction.getAmount());
            statement.setTimestamp(3, Timestamp.valueOf(transaction.getTimestamp()));
            statement.setString(4, transaction.getDescription());
            statement.setInt(5, transaction.getId());

            statement.executeUpdate();
        }
    }

    public void deleteTransaction(Connection connection, int id) throws SQLException {
        validateTransactionId(id);

        String sql = """
                DELETE FROM transactions
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    public Optional<String> findTransactionTypeById(Connection connection, int id) throws SQLException {
        validateTransactionId(id);

        String sql = """
                SELECT transaction_type
                FROM transactions
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(resultSet.getString("transaction_type"));
                }
            }
        }

        return Optional.empty();
    }

    public boolean existsById(Connection connection, int id) throws SQLException {
        validateTransactionId(id);

        String sql = """
                SELECT id
                FROM transactions
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void validateTransaction(Transaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null.");
        }

        validateTransactionId(transaction.getId());

        if (transaction.getType() == null) {
            throw new IllegalArgumentException("Transaction type cannot be null.");
        }

        if (transaction.getAmount() <= 0) {
            throw new IllegalArgumentException("Transaction amount must be positive.");
        }

        if (transaction.getTimestamp() == null) {
            throw new IllegalArgumentException("Transaction timestamp cannot be null.");
        }
    }

    private void validateTransactionId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Transaction id must be positive.");
        }
    }
}
