package com.pao.project.bank.util;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class DatabaseSeeder {
    private DatabaseSeeder() {
    }

    public static void seedDemoData() {
        seedDemoData(false);
    }

    public static void seedDemoData(boolean force) {
        Connection connection = DatabaseConnection.getInstance().getConnection();

        try {
            if (!force && databaseAlreadySeeded(connection)) {
                System.out.println("Database already contains data. Startup seed skipped.");
                return;
            }

            connection.setAutoCommit(false);

            seedPersons(connection);
            seedClients(connection);
            seedEmployees(connection);
            seedAccounts(connection);
            seedCards(connection);
            seedTransactions(connection);
            seedCheques(connection);
            seedCredits(connection);
            seedAliases(connection);

            connection.commit();
        } catch (SQLException e) {
            rollback(connection);
            throw new RuntimeException("Database seeding failed: " + e.getMessage(), e);
        } finally {
            resetAutoCommit(connection);
        }
    }

    private static void seedPersons(Connection connection) throws SQLException {
        upsertPerson(connection, 1, "CLIENT", "ion.popescu@mail.com", "0711111111");
        upsertPerson(connection, 2, "CLIENT", "maria.ionescu@mail.com", "0722222222");
        upsertPerson(connection, 3, "CLIENT", "office@techvision.ro", "0733333333");
        upsertPerson(connection, 4, "CLIENT", "contact@finexpert.ro", "0744444444");

        upsertPerson(connection, 11, "EMPLOYEE", "teller1@bank.ro", "0750000001");
        upsertPerson(connection, 12, "EMPLOYEE", "teller2@bank.ro", "0750000002");
        upsertPerson(connection, 13, "EMPLOYEE", "advisor1@bank.ro", "0750000003");
        upsertPerson(connection, 14, "EMPLOYEE", "advisor2@bank.ro", "0750000004");
    }

    private static void seedClients(Connection connection) throws SQLException {
        upsertClient(connection, 1, "C001", "INDIVIDUAL", true);
        upsertClient(connection, 2, "C002", "INDIVIDUAL", true);
        upsertClient(connection, 3, "C003", "CORPORATE", true);
        upsertClient(connection, 4, "C004", "CORPORATE", true);

        upsertIndividualClient(connection, 1, "Ion", "Popescu", "1980101010017", LocalDate.of(1980, 10, 10));
        upsertIndividualClient(connection, 2, "Maria", "Ionescu", "2960730156784", LocalDate.of(1996, 7, 30));

        upsertCorporateClient(connection, 3, "TechVision SRL", "RO12345678", 1);
        upsertCorporateClient(connection, 4, "FinExpert SRL", "RO87654321", 2);
    }

    private static void seedEmployees(Connection connection) throws SQLException {
        upsertEmployee(connection, 11, "E001", "BANK_TELLER", "Andreea", "Marin", 4500.0, "Unirii Branch");
        upsertEmployee(connection, 12, "E002", "BANK_TELLER", "Paul", "Georgescu", 4700.0, "Victoriei Branch");
        upsertEmployee(connection, 13, "E003", "FINANCIAL_ADVISOR", "Radu", "Dumitrescu", 6000.0, "Unirii Branch");
        upsertEmployee(connection, 14, "E004", "FINANCIAL_ADVISOR", "Bianca", "Stan", 6200.0, "Victoriei Branch");

        upsertBankTeller(connection, 11, 1);
        upsertBankTeller(connection, 12, 2);
        upsertFinancialAdvisor(connection, 13, "Investments");
        upsertFinancialAdvisor(connection, 14, "Loans");
    }

    private static void seedAccounts(Connection connection) throws SQLException {
        LocalDate openingDate = LocalDate.of(2026, 6, 2);

        upsertAccount(connection, 1, "RO49AAAA1B31007593840000", "CURRENT", 3200.0, "RON", true, openingDate, 1);
        upsertAccount(connection, 2, "RO49AAAA1B31007593840001", "SAVINGS", 4000.0, "RON", true, openingDate, 2);
        upsertAccount(connection, 3, "RO49AAAA1B31007593840002", "CURRENT", 11700.0, "RON", true, openingDate, 3);
        upsertAccount(connection, 4, "RO49AAAA1B31007593840003", "CURRENT", 15000.0, "RON", true, openingDate, 4);

        upsertCurrentAccount(connection, 1, 10.0);
        upsertSavingsAccount(connection, 2, 5.0, 0);
        upsertCurrentAccount(connection, 3, 20.0);
        upsertCurrentAccount(connection, 4, 25.0);
    }

    private static void seedCards(Connection connection) throws SQLException {
        LocalDate expiryDate = LocalDate.of(2029, 6, 30);

        upsertCard(connection, "4000000000000001", "101", expiryDate, true, "ACTIVE", 1);
        upsertCard(connection, "4000000000000002", "102", expiryDate, true, "ACTIVE", 2);
        upsertCard(connection, "4000000000000003", "103", expiryDate, true, "ACTIVE", 3);
        upsertCard(connection, "4000000000000004", "104", expiryDate, true, "ACTIVE", 4);
    }

    private static void seedTransactions(Connection connection) throws SQLException {
        LocalDateTime baseTime = LocalDateTime.of(2026, 6, 2, 10, 0);

        upsertTransaction(connection, 1, "DEPOSIT", 500.0, baseTime, "Demo deposit for Ion Popescu");
        upsertDepositTransaction(connection, 1, 1);

        upsertTransaction(connection, 2, "WITHDRAWAL", 100.0, baseTime.plusMinutes(10), "Demo withdrawal for Ion Popescu");
        upsertWithdrawalTransaction(connection, 2, 1);

        upsertTransaction(connection, 3, "TRANSFER", 300.0, baseTime.plusMinutes(20), "Demo transfer from TechVision SRL to Ion Popescu");
        upsertTransferTransaction(connection, 3, 3, 1);
    }

    private static void seedCheques(Connection connection) throws SQLException {
        upsertCheque(
                connection,
                "CHQ000000001",
                3,
                1,
                200.0,
                LocalDate.of(2026, 6, 2),
                LocalDate.of(2026, 6, 9),
                "ISSUED"
        );
    }

    private static void seedCredits(Connection connection) throws SQLException {
        upsertCredit(
                connection,
                1,
                1,
                1,
                "PERSONAL",
                5000.0,
                7.5,
                12,
                LocalDate.of(2026, 6, 2),
                5375.0,
                "ACTIVE"
        );
        upsertCredit(
                connection,
                2,
                3,
                3,
                "BUSINESS",
                20000.0,
                9.0,
                24,
                null,
                23600.0,
                "PENDING"
        );

        if (tableExists(connection, "credit_installments")) {
            seedInstallments(connection, 1, LocalDate.of(2026, 6, 2), 5375.0, 12, 1);
            seedInstallments(connection, 2, LocalDate.of(2026, 6, 2), 23600.0, 24, 0);
        } else {
            System.out.println("Warning: table credit_installments does not exist. Credit installments were not seeded.");
        }
    }

    private static void seedAliases(Connection connection) throws SQLException {
        upsertAlias(connection, "ion-main", 1);
        upsertAlias(connection, "maria-savings", 2);
        upsertAlias(connection, "techvision", 3);
        upsertAlias(connection, "finexpert", 4);
    }

    private static void upsertPerson(Connection connection, int id, String personType, String email, String phoneNumber)
            throws SQLException {
        String sql = """
                INSERT INTO persons (id, person_type, email, phone_number)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    person_type = VALUES(person_type),
                    email = VALUES(email),
                    phone_number = VALUES(phone_number)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setString(2, personType);
            statement.setString(3, email);
            statement.setString(4, phoneNumber);
            statement.executeUpdate();
        }
    }

    private static void upsertClient(Connection connection, int id, String clientCode, String clientType, boolean active)
            throws SQLException {
        String sql = """
                INSERT INTO clients (id, client_code, client_type, active)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE id = id
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setString(2, clientCode);
            statement.setString(3, clientType);
            statement.setBoolean(4, active);
            statement.executeUpdate();
        }
    }

    private static void upsertIndividualClient(
            Connection connection,
            int clientId,
            String firstName,
            String lastName,
            String cnp,
            LocalDate birthDate
    ) throws SQLException {
        String sql = """
                INSERT INTO individual_clients (client_id, first_name, last_name, cnp, birth_date)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    first_name = VALUES(first_name),
                    last_name = VALUES(last_name),
                    cnp = VALUES(cnp),
                    birth_date = VALUES(birth_date)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, clientId);
            statement.setString(2, firstName);
            statement.setString(3, lastName);
            statement.setString(4, cnp);
            statement.setDate(5, Date.valueOf(birthDate));
            statement.executeUpdate();
        }
    }

    private static void upsertCorporateClient(
            Connection connection,
            int clientId,
            String companyName,
            String cui,
            int legalRepresentativeId
    ) throws SQLException {
        String sql = """
                INSERT INTO corporate_clients (client_id, company_name, cui, legal_representative_id)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    company_name = VALUES(company_name),
                    cui = VALUES(cui),
                    legal_representative_id = VALUES(legal_representative_id)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, clientId);
            statement.setString(2, companyName);
            statement.setString(3, cui);
            statement.setInt(4, legalRepresentativeId);
            statement.executeUpdate();
        }
    }

    private static void upsertEmployee(
            Connection connection,
            int id,
            String employeeCode,
            String employeeType,
            String firstName,
            String lastName,
            double salary,
            String branch
    ) throws SQLException {
        String sql = """
                INSERT INTO employees (id, employee_code, employee_type, first_name, last_name, salary, branch)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    employee_code = VALUES(employee_code),
                    employee_type = VALUES(employee_type),
                    first_name = VALUES(first_name),
                    last_name = VALUES(last_name),
                    salary = VALUES(salary),
                    branch = VALUES(branch)
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

    private static void upsertBankTeller(Connection connection, int employeeId, int deskNumber) throws SQLException {
        String sql = """
                INSERT INTO bank_tellers (employee_id, desk_number)
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE desk_number = VALUES(desk_number)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, employeeId);
            statement.setInt(2, deskNumber);
            statement.executeUpdate();
        }
    }

    private static void upsertFinancialAdvisor(Connection connection, int employeeId, String specialization)
            throws SQLException {
        String sql = """
                INSERT INTO financial_advisors (employee_id, specialization)
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE specialization = VALUES(specialization)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, employeeId);
            statement.setString(2, specialization);
            statement.executeUpdate();
        }
    }

    private static void upsertAccount(
            Connection connection,
            int id,
            String iban,
            String accountType,
            double balance,
            String currency,
            boolean active,
            LocalDate openingDate,
            int clientId
    ) throws SQLException {
        String sql = """
                INSERT INTO accounts (id, iban, account_type, balance, currency, active, opening_date, client_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    iban = VALUES(iban),
                    account_type = VALUES(account_type),
                    balance = VALUES(balance),
                    currency = VALUES(currency),
                    active = VALUES(active),
                    opening_date = VALUES(opening_date),
                    client_id = VALUES(client_id)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setString(2, iban);
            statement.setString(3, accountType);
            statement.setDouble(4, balance);
            statement.setString(5, currency);
            statement.setBoolean(6, active);
            statement.setDate(7, Date.valueOf(openingDate));
            statement.setInt(8, clientId);
            statement.executeUpdate();
        }
    }

    private static void upsertCurrentAccount(Connection connection, int accountId, double monthlyFee)
            throws SQLException {
        String sql = """
                INSERT INTO current_accounts (account_id, monthly_fee)
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE monthly_fee = VALUES(monthly_fee)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, accountId);
            statement.setDouble(2, monthlyFee);
            statement.executeUpdate();
        }
    }

    private static void upsertSavingsAccount(
            Connection connection,
            int accountId,
            double interestRate,
            int withdrawalsThisMonth
    ) throws SQLException {
        String sql = """
                INSERT INTO savings_accounts (account_id, interest_rate, withdrawals_this_month)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    interest_rate = VALUES(interest_rate),
                    withdrawals_this_month = VALUES(withdrawals_this_month)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, accountId);
            statement.setDouble(2, interestRate);
            statement.setInt(3, withdrawalsThisMonth);
            statement.executeUpdate();
        }
    }

    private static void upsertCard(
            Connection connection,
            String cardNumber,
            String cvv,
            LocalDate expirationDate,
            boolean contactless,
            String status,
            int accountId
    ) throws SQLException {
        String sql = """
                INSERT INTO cards (card_number, cvv, expiration_date, contactless, status, account_id)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    cvv = VALUES(cvv),
                    expiration_date = VALUES(expiration_date),
                    contactless = VALUES(contactless),
                    status = VALUES(status),
                    account_id = VALUES(account_id)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, cardNumber);
            statement.setString(2, cvv);
            statement.setDate(3, Date.valueOf(expirationDate));
            statement.setBoolean(4, contactless);
            statement.setString(5, status);
            statement.setInt(6, accountId);
            statement.executeUpdate();
        }
    }

    private static void upsertTransaction(
            Connection connection,
            int id,
            String transactionType,
            double amount,
            LocalDateTime timestamp,
            String description
    ) throws SQLException {
        String sql = """
                INSERT INTO transactions (id, transaction_type, amount, `timestamp`, description)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    transaction_type = VALUES(transaction_type),
                    amount = VALUES(amount),
                    `timestamp` = VALUES(`timestamp`),
                    description = VALUES(description)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setString(2, transactionType);
            statement.setDouble(3, amount);
            statement.setTimestamp(4, Timestamp.valueOf(timestamp));
            statement.setString(5, description);
            statement.executeUpdate();
        }
    }

    private static void upsertDepositTransaction(Connection connection, int transactionId, int destinationAccountId)
            throws SQLException {
        String sql = """
                INSERT INTO deposit_transactions (transaction_id, destination_account_id)
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE destination_account_id = VALUES(destination_account_id)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, transactionId);
            statement.setInt(2, destinationAccountId);
            statement.executeUpdate();
        }
    }

    private static void upsertWithdrawalTransaction(Connection connection, int transactionId, int sourceAccountId)
            throws SQLException {
        String sql = """
                INSERT INTO withdrawal_transactions (transaction_id, source_account_id)
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE source_account_id = VALUES(source_account_id)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, transactionId);
            statement.setInt(2, sourceAccountId);
            statement.executeUpdate();
        }
    }

    private static void upsertTransferTransaction(
            Connection connection,
            int transactionId,
            int sourceAccountId,
            int destinationAccountId
    ) throws SQLException {
        String sql = """
                INSERT INTO transfer_transactions (transaction_id, source_account_id, destination_account_id)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    source_account_id = VALUES(source_account_id),
                    destination_account_id = VALUES(destination_account_id)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, transactionId);
            statement.setInt(2, sourceAccountId);
            statement.setInt(3, destinationAccountId);
            statement.executeUpdate();
        }
    }

    private static void upsertCheque(
            Connection connection,
            String series,
            int issuerAccountId,
            int beneficiaryClientId,
            double amount,
            LocalDate issueDate,
            LocalDate expiryDate,
            String status
    ) throws SQLException {
        String sql = """
                INSERT INTO cheques (
                    series,
                    issuer_account_id,
                    beneficiary_client_id,
                    amount,
                    issue_date,
                    expiry_date,
                    status
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    issuer_account_id = VALUES(issuer_account_id),
                    beneficiary_client_id = VALUES(beneficiary_client_id),
                    amount = VALUES(amount),
                    issue_date = VALUES(issue_date),
                    expiry_date = VALUES(expiry_date),
                    status = VALUES(status)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, series);
            statement.setInt(2, issuerAccountId);
            statement.setInt(3, beneficiaryClientId);
            statement.setDouble(4, amount);
            statement.setDate(5, Date.valueOf(issueDate));
            statement.setDate(6, Date.valueOf(expiryDate));
            statement.setString(7, status);
            statement.executeUpdate();
        }
    }

    private static void upsertAlias(Connection connection, String alias, int accountId) throws SQLException {
        String sql = """
                INSERT INTO iban_aliases (alias, account_id)
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE account_id = VALUES(account_id)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, alias);
            statement.setInt(2, accountId);
            statement.executeUpdate();
        }
    }

    private static void upsertCredit(
            Connection connection,
            int id,
            int borrowerId,
            int targetAccountId,
            String creditType,
            double principalAmount,
            double annualInterestRate,
            int durationInMonths,
            LocalDate startDate,
            double remainingAmount,
            String status
    ) throws SQLException {
        String sql = """
                INSERT INTO credits (
                    id,
                    borrower_id,
                    target_account_id,
                    credit_type,
                    principal_amount,
                    annual_interest_rate,
                    duration_in_months,
                    start_date,
                    remaining_amount,
                    status
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    borrower_id = VALUES(borrower_id),
                    target_account_id = VALUES(target_account_id),
                    credit_type = VALUES(credit_type),
                    principal_amount = VALUES(principal_amount),
                    annual_interest_rate = VALUES(annual_interest_rate),
                    duration_in_months = VALUES(duration_in_months),
                    start_date = VALUES(start_date),
                    remaining_amount = VALUES(remaining_amount),
                    status = VALUES(status)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setInt(2, borrowerId);
            statement.setInt(3, targetAccountId);
            statement.setString(4, creditType);
            statement.setDouble(5, principalAmount);
            statement.setDouble(6, annualInterestRate);
            statement.setInt(7, durationInMonths);
            if (startDate == null) {
                statement.setNull(8, java.sql.Types.DATE);
            } else {
                statement.setDate(8, Date.valueOf(startDate));
            }
            statement.setDouble(9, remainingAmount);
            statement.setString(10, status);
            statement.executeUpdate();
        }
    }

    private static void seedInstallments(
            Connection connection,
            int creditId,
            LocalDate startDate,
            double totalAmount,
            int durationInMonths,
            int paidInstallments
    ) throws SQLException {
        double monthlyAmount = Math.round((totalAmount / durationInMonths) * 100.0) / 100.0;
        double assignedAmount = 0.0;

        for (int installmentNumber = 1; installmentNumber <= durationInMonths; installmentNumber++) {
            double amount = installmentNumber == durationInMonths
                    ? Math.round((totalAmount - assignedAmount) * 100.0) / 100.0
                    : monthlyAmount;
            assignedAmount += amount;

            upsertCreditInstallment(
                    connection,
                    creditId,
                    installmentNumber,
                    startDate.plusMonths(installmentNumber),
                    amount,
                    installmentNumber <= paidInstallments
            );
        }
    }

    private static void upsertCreditInstallment(
            Connection connection,
            int creditId,
            int installmentNumber,
            LocalDate dueDate,
            double amount,
            boolean paid
    ) throws SQLException {
        String sql = """
                INSERT INTO credit_installments (
                    credit_id,
                    installment_number,
                    due_date,
                    amount,
                    paid
                )
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    due_date = VALUES(due_date),
                    amount = VALUES(amount),
                    paid = VALUES(paid)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, creditId);
            statement.setInt(2, installmentNumber);
            statement.setDate(3, Date.valueOf(dueDate));
            statement.setDouble(4, amount);
            statement.setBoolean(5, paid);
            statement.executeUpdate();
        }
    }

    private static void rollback(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            throw new RuntimeException("Database seeding rollback failed.", rollbackException);
        }
    }

    private static void resetAutoCommit(Connection connection) {
        try {
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException("Could not reset database autoCommit after seeding.", e);
        }
    }

    private static boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                """)) {
            statement.setString(1, tableName);

            try (var resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private static boolean databaseAlreadySeeded(Connection connection) throws SQLException {
        if (!tableExists(connection, "persons")) {
            return false;
        }

        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM persons");
             var resultSet = statement.executeQuery()) {
            return resultSet.next() && resultSet.getInt(1) > 0;
        }
    }
}
