package com.pao.project.bank.repository.account;

import com.pao.project.bank.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class IbanAliasRepository {
    private final Connection connection;

    public IbanAliasRepository() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    public void save(String alias, int accountId) {
        validateAlias(alias);
        validateAccountId(accountId);

        String sql = """
                INSERT INTO iban_aliases (
                    alias,
                    account_id
                )
                VALUES (?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizeAlias(alias));
            statement.setInt(2, accountId);

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not save IBAN alias.", e);
        }
    }

    public void upsert(String alias, int accountId) {
        validateAlias(alias);
        validateAccountId(accountId);

        String sql = """
                INSERT INTO iban_aliases (
                    alias,
                    account_id
                )
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE account_id = VALUES(account_id)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizeAlias(alias));
            statement.setInt(2, accountId);

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not save IBAN alias.", e);
        }
    }

    public Optional<Integer> findAccountIdByAlias(String alias) {
        validateAlias(alias);

        String sql = """
                SELECT account_id
                FROM iban_aliases
                WHERE alias = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizeAlias(alias));

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(resultSet.getInt("account_id"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find account by IBAN alias.", e);
        }

        return Optional.empty();
    }

    public Map<String, Integer> findAll() {
        String sql = """
                SELECT alias,
                       account_id
                FROM iban_aliases
                ORDER BY alias
                """;

        Map<String, Integer> aliases = new HashMap<>();

        try (
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                aliases.put(
                        resultSet.getString("alias"),
                        resultSet.getInt("account_id")
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find IBAN aliases.", e);
        }

        return aliases;
    }

    public void delete(String alias) {
        validateAlias(alias);

        String sql = """
                DELETE FROM iban_aliases
                WHERE alias = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizeAlias(alias));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete IBAN alias.", e);
        }
    }

    private String normalizeAlias(String alias) {
        return alias.trim().toLowerCase();
    }

    private void validateAlias(String alias) {
        if (alias == null || alias.isBlank()) {
            throw new IllegalArgumentException("Alias cannot be null or blank.");
        }
    }

    private void validateAccountId(int accountId) {
        if (accountId <= 0) {
            throw new IllegalArgumentException("Account id must be positive.");
        }
    }
}
