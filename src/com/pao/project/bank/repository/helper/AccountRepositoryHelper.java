package com.pao.project.bank.repository.helper;

import com.pao.project.bank.model.account.Account;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class AccountRepositoryHelper {
    public static final String ACCOUNT_TYPE_CURRENT = "CURRENT";
    public static final String ACCOUNT_TYPE_SAVINGS = "SAVINGS";

    public AccountRepositoryHelper() {
    }

    public void insertAccount(Connection connection, Account account, String accountType) throws SQLException {
        validateAccountType(accountType);
        validateAccount(account);

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
            statement.setString(3, accountType);
            statement.setDouble(4, account.getBalance());
            statement.setString(5, account.getCurrency());
            statement.setBoolean(6, account.isActive());
            statement.setDate(7, Date.valueOf(account.getOpeningDate()));
            statement.setInt(8, account.getOwner().getId());

            statement.executeUpdate();
        }
    }

    public void updateAccount(Connection connection, Account account) throws SQLException {
        validateAccount(account);

        String sql = """
                UPDATE accounts
                SET iban = ?,
                    balance = ?,
                    currency = ?,
                    active = ?,
                    opening_date = ?,
                    client_id = ?
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, account.getIban().getCode());
            statement.setDouble(2, account.getBalance());
            statement.setString(3, account.getCurrency());
            statement.setBoolean(4, account.isActive());
            statement.setDate(5, Date.valueOf(account.getOpeningDate()));
            statement.setInt(6, account.getOwner().getId());
            statement.setInt(7, account.getId());

            statement.executeUpdate();
        }
    }

    public void deleteAccount(Connection connection, int id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("Account id must be positive.");
        }

        String sql = """
                DELETE FROM accounts
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    public boolean existsById(Connection connection, int id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("Account id must be positive.");
        }

        String sql = """
                SELECT id
                FROM accounts
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public boolean existsByIban(Connection connection, String iban) throws SQLException {
        if (iban == null || iban.isBlank()) {
            throw new IllegalArgumentException("IBAN cannot be null or blank.");
        }

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

    private void validateAccountType(String accountType) {
        if (!ACCOUNT_TYPE_CURRENT.equals(accountType)
                && !ACCOUNT_TYPE_SAVINGS.equals(accountType)) {
            throw new IllegalArgumentException("Invalid account type: " + accountType);
        }
    }

    private void validateAccount(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null.");
        }

        if (account.getId() <= 0) {
            throw new IllegalArgumentException("Account id must be positive.");
        }

        if (account.getIban() == null) {
            throw new IllegalArgumentException("IBAN cannot be null.");
        }

        if (account.getCurrency() == null || account.getCurrency().isBlank()) {
            throw new IllegalArgumentException("Currency cannot be null or blank.");
        }

        if (account.getBalance() < 0) {
            throw new IllegalArgumentException("Balance cannot be negative.");
        }

        if (account.getOwner() == null) {
            throw new IllegalArgumentException("Account owner cannot be null.");
        }

        if (account.getOpeningDate() == null) {
            throw new IllegalArgumentException("Opening date cannot be null.");
        }
    }
}