package com.pao.project.bank.repository.helper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class PersonRepositoryHelper {
    public static final String PERSON_TYPE_CLIENT = "CLIENT";
    public static final String PERSON_TYPE_EMPLOYEE = "EMPLOYEE";

    public PersonRepositoryHelper() {}

    public void insertPerson(Connection connection, int id, String personType, String email, String phoneNumber) throws SQLException {
        validatePersonType(personType);
        validatePersonData(id, email, phoneNumber);

        String sql = """
                INSERT INTO persons (
                    id,
                    person_type,
                    email,
                    phone_number
                )
                VALUES (?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setString(2, personType);
            statement.setString(3, email);
            statement.setString(4, phoneNumber);

            statement.executeUpdate();
        }
    }

    public void updatePerson(Connection connection, int id, String email, String phoneNumber) throws SQLException {
        validatePersonData(id, email, phoneNumber);

        String sql = """
                UPDATE persons
                SET email = ?,
                    phone_number = ?
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            statement.setString(2, phoneNumber);
            statement.setInt(3, id);

            statement.executeUpdate();
        }
    }

    public void deletePerson(Connection connection, int id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("Person id must be positive.");
        }

        String sql = """
                DELETE FROM persons
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    public boolean existsById(Connection connection, int id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("Person id must be positive.");
        }

        String sql = """
                SELECT id
                FROM persons
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public boolean existsByEmail(Connection connection, String email) throws SQLException {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank.");
        }

        String sql = """
                SELECT id
                FROM persons
                WHERE email = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void validatePersonType(String personType) {
        if (!PERSON_TYPE_CLIENT.equals(personType)
                && !PERSON_TYPE_EMPLOYEE.equals(personType)) {
            throw new IllegalArgumentException("Invalid person type: " + personType);
        }
    }

    private void validatePersonData(int id, String email, String phoneNumber) {
        if (id <= 0) {
            throw new IllegalArgumentException("Person id must be positive.");
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank.");
        }

        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("Phone number cannot be null or blank.");
        }
    }
}