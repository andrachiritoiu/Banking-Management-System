package com.pao.project.bank.repository.helper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class EmployeeRepositoryHelper {
    public static final String EMPLOYEE_TYPE_BANK_TELLER = "BANK_TELLER";
    public static final String EMPLOYEE_TYPE_FINANCIAL_ADVISOR = "FINANCIAL_ADVISOR";

    public EmployeeRepositoryHelper() {
    }

    public void insertEmployee(Connection connection, int id, String employeeCode, String employeeType, String firstName, String lastName, double salary, String branch) throws SQLException {
        validateEmployeeType(employeeType);
        validateEmployeeData(id, employeeCode, firstName, lastName, salary, branch);

        String sql = """
                INSERT INTO employees (
                    id,
                    employee_code,
                    employee_type,
                    first_name,
                    last_name,
                    salary,
                    branch
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setString(2, employeeCode);
            statement.setString(3, employeeType);
            statement.setString(4, firstName);
            statement.setString(5, lastName);
            statement.setDouble(6, salary);
            statement.setString(7, branch);

            statement.executeUpdate();
        }
    }

    public void updateEmployee(Connection connection, int id, String employeeCode, String firstName, String lastName, double salary, String branch) throws SQLException {
        validateEmployeeData(id, employeeCode, firstName, lastName, salary, branch);

        String sql = """
                UPDATE employees
                SET employee_code = ?,
                    first_name = ?,
                    last_name = ?,
                    salary = ?,
                    branch = ?
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, employeeCode);
            statement.setString(2, firstName);
            statement.setString(3, lastName);
            statement.setDouble(4, salary);
            statement.setString(5, branch);
            statement.setInt(6, id);

            statement.executeUpdate();
        }
    }

    public void deleteEmployee(Connection connection, int id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("Employee id must be positive.");
        }

        String sql = """
                DELETE FROM employees
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    public boolean existsById(Connection connection, int id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("Employee id must be positive.");
        }

        String sql = """
                SELECT id
                FROM employees
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public boolean existsByEmployeeCode(Connection connection, String employeeCode) throws SQLException {
        if (employeeCode == null || employeeCode.isBlank()) {
            throw new IllegalArgumentException("Employee code cannot be null or blank.");
        }

        String sql = """
                SELECT id
                FROM employees
                WHERE employee_code = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, employeeCode);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void validateEmployeeType(String employeeType) {
        if (!EMPLOYEE_TYPE_BANK_TELLER.equals(employeeType)
                && !EMPLOYEE_TYPE_FINANCIAL_ADVISOR.equals(employeeType)) {
            throw new IllegalArgumentException("Invalid employee type: " + employeeType);
        }
    }

    private void validateEmployeeData(int id, String employeeCode, String firstName, String lastName, double salary, String branch) {
        if (id <= 0) {
            throw new IllegalArgumentException("Employee id must be positive.");
        }

        if (employeeCode == null || employeeCode.isBlank()) {
            throw new IllegalArgumentException("Employee code cannot be null or blank.");
        }

        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("First name cannot be null or blank.");
        }

        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("Last name cannot be null or blank.");
        }

        if (salary < 0) {
            throw new IllegalArgumentException("Salary cannot be negative.");
        }

        if (branch == null || branch.isBlank()) {
            throw new IllegalArgumentException("Branch cannot be null or blank.");
        }
    }
}