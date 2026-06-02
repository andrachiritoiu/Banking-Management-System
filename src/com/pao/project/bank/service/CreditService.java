package com.pao.project.bank.service;

import com.pao.project.bank.exception.InvalidOperationException;
import com.pao.project.bank.model.account.Account;
import com.pao.project.bank.model.Credit;
import com.pao.project.bank.model.enums.CreditStatus;
import com.pao.project.bank.model.enums.CreditType;
import com.pao.project.bank.model.person.Client;
import com.pao.project.bank.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreditService {
    private static final CreditService INSTANCE = new CreditService();

    private final AccountService accountService = AccountService.getInstance();
    private final Connection connection = DatabaseConnection.getInstance().getConnection();

    private int creditIdCounter = 1;
    private final List<Credit> credits = new ArrayList<>();
    private final Map<Integer, Credit> creditsById = new HashMap<>();

    private CreditService() {}

    public static CreditService getInstance() {
        return INSTANCE;
    }

    public Credit createCredit(Client borrower, String targetIban, CreditType type, double principalAmount, double annualInterestRate, int durationInMonths) {
        if (borrower == null) {
            throw new InvalidOperationException("Borrower cannot be null.");
        }

        Account targetAccount = accountService.findByIban(targetIban);

        if (targetAccount == null) {
            throw new InvalidOperationException("Target account not found.");
        }

        if (!targetAccount.getOwner().equals(borrower)) {
            throw new InvalidOperationException("The target account does not belong to this client.");
        }

        Credit credit = new Credit(
                creditIdCounter++,
                borrower,
                targetAccount,
                type,
                principalAmount,
                annualInterestRate,
                durationInMonths,
                LocalDate.now()
        );

        credits.add(credit);
        creditsById.put(credit.getId(), credit);

        return credit;
    }

    // etapa 2
    public int applyForCreditJdbc(
            Client borrower,
            String targetIban,
            CreditType type,
            double principalAmount,
            double annualInterestRate,
            int durationInMonths
    ) {
        validateCreditApplication(borrower, targetIban, type, principalAmount, annualInterestRate, durationInMonths);

        try {
            connection.setAutoCommit(false);

            AccountCreditDbData targetAccount = getTargetAccountForCredit(targetIban);

            if (targetAccount.clientId != borrower.getId()) {
                throw new SQLException("The target account does not belong to this client.");
            }

            double totalAmountToPay = calculateTotalAmountToPay(principalAmount, annualInterestRate, durationInMonths);
            LocalDate applicationDate = LocalDate.now();

            int creditId = insertCreditJdbc(
                    borrower.getId(),
                    targetAccount.id,
                    type,
                    principalAmount,
                    annualInterestRate,
                    durationInMonths,
                    applicationDate,
                    totalAmountToPay,
                    CreditStatus.PENDING
            );

            insertCreditInstallmentsJdbc(
                    creditId,
                    applicationDate,
                    totalAmountToPay,
                    durationInMonths
            );

            connection.commit();
            System.out.println("Apply for credit JDBC completed successfully.");

            return creditId;
        } catch (SQLException e) {
            try {
                connection.rollback();
                System.out.println("Apply for credit JDBC failed. Rollback executed.");
            } catch (SQLException rollbackException) {
                throw new RuntimeException("Rollback failed.", rollbackException);
            }

            throw new RuntimeException("Apply for credit JDBC failed: " + e.getMessage(), e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                throw new RuntimeException("Could not reset autoCommit.", e);
            }
        }
    }

    public void approveCredit(int creditId) {
        Credit credit = findById(creditId);

        credit.approve();

        accountService.deposit(
                credit.getTargetAccount().getIban().getCode(),
                credit.getPrincipalAmount()
        );
    }

    public void rejectCredit(int creditId) {
        Credit credit = findById(creditId);
        credit.reject();
    }

    public void payInstallment(int creditId, double amount) {
        Credit credit = findById(creditId);

        if (amount <= 0) {
            throw new InvalidOperationException("Installment amount must be positive.");
        }

        accountService.withdraw(
                credit.getTargetAccount().getIban().getCode(),
                amount
        );

        credit.payInstallment(amount);
    }

    public Credit findById(int creditId) {
        Credit credit = creditsById.get(creditId);

        if (credit == null) {
            throw new InvalidOperationException("Credit not found.");
        }

        return credit;
    }

    public List<Credit> getAllCredits() {
        return new ArrayList<>(credits);
    }

    public List<Credit> getCreditsForClient(Client client) {
        List<Credit> result = new ArrayList<>();

        if (client == null) {
            return result;
        }

        for (Credit credit : credits) {
            if (credit.getBorrower().equals(client)) {
                result.add(credit);
            }
        }

        return result;
    }

    public List<Credit> getCreditsByStatus(CreditStatus status) {
        List<Credit> result = new ArrayList<>();

        for (Credit credit : credits) {
            if (credit.getStatus() == status) {
                result.add(credit);
            }
        }

        return result;
    }

    private AccountCreditDbData getTargetAccountForCredit(String iban) throws SQLException {
        String sql = """
                SELECT id, client_id
                FROM accounts
                WHERE iban = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, iban);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new AccountCreditDbData(
                            resultSet.getInt("id"),
                            resultSet.getInt("client_id")
                    );
                }
            }
        }

        throw new SQLException("Target account not found.");
    }

    private int insertCreditJdbc(
            int borrowerId,
            int targetAccountId,
            CreditType type,
            double principalAmount,
            double annualInterestRate,
            int durationInMonths,
            LocalDate startDate,
            double remainingAmount,
            CreditStatus status
    ) throws SQLException {
        String sql = """
                INSERT INTO credits (
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
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, borrowerId);
            statement.setInt(2, targetAccountId);
            statement.setString(3, type.name());
            statement.setDouble(4, principalAmount);
            statement.setDouble(5, annualInterestRate);
            statement.setInt(6, durationInMonths);
            statement.setDate(7, Date.valueOf(startDate));
            statement.setDouble(8, remainingAmount);
            statement.setString(9, status.name());

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Could not insert credit.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }

        throw new SQLException("Could not get generated credit id.");
    }

    private void insertCreditInstallmentsJdbc(
            int creditId,
            LocalDate applicationDate,
            double totalAmountToPay,
            int durationInMonths
    ) throws SQLException {
        String sql = """
                INSERT INTO credit_installments (
                    credit_id,
                    installment_number,
                    due_date,
                    amount
                )
                VALUES (?, ?, ?, ?)
                """;

        double monthlyInstallment = totalAmountToPay / durationInMonths;
        double insertedAmount = 0;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int installmentNumber = 1; installmentNumber <= durationInMonths; installmentNumber++) {
                double amount = installmentNumber == durationInMonths
                        ? totalAmountToPay - insertedAmount
                        : monthlyInstallment;

                statement.setInt(1, creditId);
                statement.setInt(2, installmentNumber);
                statement.setDate(3, Date.valueOf(applicationDate.plusMonths(installmentNumber)));
                statement.setDouble(4, amount);
                statement.addBatch();

                insertedAmount += amount;
            }

            int[] affectedRows = statement.executeBatch();

            for (int affectedRow : affectedRows) {
                if (affectedRow == 0) {
                    throw new SQLException("Could not insert all credit installments.");
                }
            }
        }
    }

    private double calculateTotalAmountToPay(double principalAmount, double annualInterestRate, int durationInMonths) {
        double years = durationInMonths / 12.0;
        return principalAmount + principalAmount * annualInterestRate * years / 100.0;
    }

    private void validateCreditApplication(
            Client borrower,
            String targetIban,
            CreditType type,
            double principalAmount,
            double annualInterestRate,
            int durationInMonths
    ) {
        if (borrower == null) {
            throw new InvalidOperationException("Borrower cannot be null.");
        }

        if (targetIban == null || targetIban.isBlank()) {
            throw new InvalidOperationException("Target IBAN cannot be null or blank.");
        }

        if (type == null) {
            throw new InvalidOperationException("Credit type cannot be null.");
        }

        if (principalAmount <= 0) {
            throw new InvalidOperationException("Principal amount must be greater than 0.");
        }

        if (annualInterestRate < 0) {
            throw new InvalidOperationException("Annual interest rate cannot be negative.");
        }

        if (durationInMonths <= 0) {
            throw new InvalidOperationException("Duration must be greater than 0.");
        }
    }

    private static class AccountCreditDbData {
        private final int id;
        private final int clientId;

        private AccountCreditDbData(int id, int clientId) {
            this.id = id;
            this.clientId = clientId;
        }
    }
}
