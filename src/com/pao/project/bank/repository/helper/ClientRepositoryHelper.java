package com.pao.project.bank.repository.helper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class ClientRepositoryHelper {
    public static final String CLIENT_TYPE_INDIVIDUAL = "INDIVIDUAL";
    public static final String CLIENT_TYPE_CORPORATE = "CORPORATE";

    public ClientRepositoryHelper() {
    }

    public void insertClient(Connection connection, int id, String clientCode, String clientType, boolean active) throws SQLException {
        validateClientType(clientType);
        validateClientData(id, clientCode);

        String sql = """
                INSERT INTO clients (
                    id,
                    client_code,
                    client_type,
                    active
                )
                VALUES (?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setString(2, clientCode);
            statement.setString(3, clientType);
            statement.setBoolean(4, active);

            statement.executeUpdate();
        }
    }

    public void updateClient(Connection connection, int id, String clientCode, boolean active) throws SQLException {
        validateClientData(id, clientCode);

        String sql = """
                UPDATE clients
                SET client_code = ?,
                    active = ?
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, clientCode);
            statement.setBoolean(2, active);
            statement.setInt(3, id);

            statement.executeUpdate();
        }
    }

    public void deleteClient(Connection connection, int id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("Client id must be positive.");
        }

        String sql = """
                DELETE FROM clients
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    public boolean existsById(Connection connection, int id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("Client id must be positive.");
        }

        String sql = """
                SELECT id
                FROM clients
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public boolean existsByClientCode(Connection connection, String clientCode) throws SQLException {
        if (clientCode == null || clientCode.isBlank()) {
            throw new IllegalArgumentException("Client code cannot be null or blank.");
        }

        String sql = """
                SELECT id
                FROM clients
                WHERE client_code = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, clientCode);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void validateClientType(String clientType) {
        if (!CLIENT_TYPE_INDIVIDUAL.equals(clientType)
                && !CLIENT_TYPE_CORPORATE.equals(clientType)) {
            throw new IllegalArgumentException("Invalid client type: " + clientType);
        }
    }

    private void validateClientData(int id, String clientCode) {
        if (id <= 0) {
            throw new IllegalArgumentException("Client id must be positive.");
        }

        if (clientCode == null || clientCode.isBlank()) {
            throw new IllegalArgumentException("Client code cannot be null or blank.");
        }
    }
}