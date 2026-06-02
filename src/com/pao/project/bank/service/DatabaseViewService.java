package com.pao.project.bank.service;

import com.pao.project.bank.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseViewService {
    private static final DatabaseViewService INSTANCE = new DatabaseViewService();

    private final AuditService auditService = AuditService.getInstance();

    private DatabaseViewService() {
    }

    public static DatabaseViewService getInstance() {
        return INSTANCE;
    }

    public void showAllData() {
        try {
            showClients();
            showEmployees();
            showAccounts();
            showCards();
            showCheques();
            showCredits();
            showTransactions();
        } catch (SQLException e) {
            throw new RuntimeException("Could not show database data: " + e.getMessage(), e);
        }
    }

    private void showClients() throws SQLException {
        printQuery("- CLIENTS FROM DB -", """
                SELECT
                    c.id,
                    c.client_code,
                    c.client_type,
                    c.active,
                    COALESCE(CONCAT(ic.first_name, ' ', ic.last_name), cc.company_name) AS display_name,
                    p.email,
                    p.phone_number
                FROM clients c
                JOIN persons p ON c.id = p.id
                LEFT JOIN individual_clients ic ON c.id = ic.client_id
                LEFT JOIN corporate_clients cc ON c.id = cc.client_id
                ORDER BY c.id
                """, resultSet -> String.format(
                "id=%d, code=%s, type=%s, active=%s, name=%s, email=%s, phone=%s",
                resultSet.getInt("id"),
                resultSet.getString("client_code"),
                resultSet.getString("client_type"),
                resultSet.getBoolean("active"),
                resultSet.getString("display_name"),
                resultSet.getString("email"),
                resultSet.getString("phone_number")
        ));
    }

    private void showEmployees() throws SQLException {
        printQuery("- EMPLOYEES FROM DB -", """
                SELECT
                    e.id,
                    e.employee_code,
                    e.employee_type,
                    e.first_name,
                    e.last_name,
                    e.salary,
                    e.branch,
                    p.email
                FROM employees e
                JOIN persons p ON e.id = p.id
                ORDER BY e.id
                """, resultSet -> String.format(
                "id=%d, code=%s, type=%s, name=%s %s, salary=%.2f, branch=%s, email=%s",
                resultSet.getInt("id"),
                resultSet.getString("employee_code"),
                resultSet.getString("employee_type"),
                resultSet.getString("first_name"),
                resultSet.getString("last_name"),
                resultSet.getDouble("salary"),
                resultSet.getString("branch"),
                resultSet.getString("email")
        ));
    }

    private void showAccounts() throws SQLException {
        printQuery("- ACCOUNTS FROM DB -", """
                SELECT
                    a.id,
                    a.iban,
                    a.account_type,
                    a.balance,
                    a.currency,
                    a.active,
                    a.client_id,
                    COALESCE(CONCAT(ic.first_name, ' ', ic.last_name), cc.company_name) AS owner_name
                FROM accounts a
                JOIN clients c ON a.client_id = c.id
                LEFT JOIN individual_clients ic ON c.id = ic.client_id
                LEFT JOIN corporate_clients cc ON c.id = cc.client_id
                ORDER BY a.id
                """, resultSet -> String.format(
                "id=%d, iban=%s, type=%s, balance=%.2f %s, active=%s, owner=%s(%d)",
                resultSet.getInt("id"),
                resultSet.getString("iban"),
                resultSet.getString("account_type"),
                resultSet.getDouble("balance"),
                resultSet.getString("currency"),
                resultSet.getBoolean("active"),
                resultSet.getString("owner_name"),
                resultSet.getInt("client_id")
        ));
    }

    private void showCards() throws SQLException {
        printQuery("- CARDS FROM DB -", """
                SELECT card_number, expiration_date, contactless, status, account_id
                FROM cards
                ORDER BY card_number
                """, resultSet -> String.format(
                "card=%s, expires=%s, contactless=%s, status=%s, account_id=%d",
                resultSet.getString("card_number"),
                resultSet.getDate("expiration_date"),
                resultSet.getBoolean("contactless"),
                resultSet.getString("status"),
                resultSet.getInt("account_id")
        ));
    }

    private void showCheques() throws SQLException {
        printQuery("- CHEQUES FROM DB -", """
                SELECT series, issuer_account_id, beneficiary_client_id, amount, issue_date, expiry_date, status
                FROM cheques
                ORDER BY series
                """, resultSet -> String.format(
                "series=%s, issuer_account_id=%d, beneficiary_client_id=%d, amount=%.2f, issue=%s, expiry=%s, status=%s",
                resultSet.getString("series"),
                resultSet.getInt("issuer_account_id"),
                resultSet.getInt("beneficiary_client_id"),
                resultSet.getDouble("amount"),
                resultSet.getDate("issue_date"),
                resultSet.getDate("expiry_date"),
                resultSet.getString("status")
        ));
    }

    private void showCredits() throws SQLException {
        printQuery("- CREDITS FROM DB -", """
                SELECT id, borrower_id, target_account_id, credit_type, principal_amount, remaining_amount, status
                FROM credits
                ORDER BY id
                """, resultSet -> String.format(
                "id=%d, borrower_id=%d, target_account_id=%d, type=%s, principal=%.2f, remaining=%.2f, status=%s",
                resultSet.getInt("id"),
                resultSet.getInt("borrower_id"),
                resultSet.getInt("target_account_id"),
                resultSet.getString("credit_type"),
                resultSet.getDouble("principal_amount"),
                resultSet.getDouble("remaining_amount"),
                resultSet.getString("status")
        ));
    }

    private void showTransactions() throws SQLException {
        printQuery("- TRANSACTIONS FROM DB -", """
                SELECT id, transaction_type, amount, `timestamp`, description
                FROM transactions
                ORDER BY `timestamp`
                """, resultSet -> String.format(
                "id=%d, type=%s, amount=%.2f, timestamp=%s, description=%s",
                resultSet.getInt("id"),
                resultSet.getString("transaction_type"),
                resultSet.getDouble("amount"),
                resultSet.getTimestamp("timestamp"),
                resultSet.getString("description")
        ));
    }

    private void printQuery(String title, String sql, ResultSetFormatter formatter) throws SQLException {
        auditService.logAction("database_view_" + title.toLowerCase()
                .replace("-", "")
                .replace("from db", "")
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_|_$", ""));

        System.out.println("\n" + title);

        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            boolean found = false;

            while (resultSet.next()) {
                found = true;
                System.out.println(formatter.format(resultSet));
            }

            if (!found) {
                System.out.println("(no rows)");
            }
        }
    }

    @FunctionalInterface
    private interface ResultSetFormatter {
        String format(ResultSet resultSet) throws SQLException;
    }
}
