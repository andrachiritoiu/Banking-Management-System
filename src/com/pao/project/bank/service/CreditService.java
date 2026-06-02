package com.pao.project.bank.service;

import com.pao.project.bank.exception.InvalidOperationException;
import com.pao.project.bank.model.account.Account;
import com.pao.project.bank.model.Credit;
import com.pao.project.bank.model.enums.CreditStatus;
import com.pao.project.bank.model.enums.CreditType;
import com.pao.project.bank.model.enums.TransactionType;
import com.pao.project.bank.model.person.Client;
import com.pao.project.bank.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreditService {
    private static final CreditService INSTANCE = new CreditService();

    private final AccountService accountService = AccountService.getInstance();

    private int creditIdCounter = 1;
    private final List<Credit> credits = new ArrayList<>();
    private final Map<Integer, Credit> creditsById = new HashMap<>();

    private CreditService() {}

    public static CreditService getInstance() {
        return INSTANCE;
    }

    private Connection getConnection() {
        return DatabaseConnection.getInstance().getConnection();
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
            getConnection().setAutoCommit(false);

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

            getConnection().commit();
            System.out.println("Apply for credit JDBC completed successfully.");

            return creditId;
        } catch (SQLException e) {
            try {
                getConnection().rollback();
                System.out.println("Apply for credit JDBC failed. Rollback executed.");
            } catch (SQLException rollbackException) {
                throw new RuntimeException("Rollback failed.", rollbackException);
            }

            throw new RuntimeException("Apply for credit JDBC failed: " + e.getMessage(), e);
        } finally {
            try {
                getConnection().setAutoCommit(true);
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

    // etapa 2
    public void payInstallmentJdbc(int creditId, int installmentNumber) {
        if (creditId <= 0) {
            throw new InvalidOperationException("Credit id must be positive.");
        }

        if (installmentNumber <= 0) {
            throw new InvalidOperationException("Installment number must be positive.");
        }

        try {
            getConnection().setAutoCommit(false);

            CreditInstallmentPaymentDbData paymentData = getInstallmentForPayment(creditId, installmentNumber);

            if (paymentData.status != CreditStatus.ACTIVE) {
                throw new SQLException("Only active credits can be paid.");
            }

            if (paymentData.installmentPaid) {
                throw new SQLException("Installment is already paid.");
            }

            if (!paymentData.accountActive) {
                throw new SQLException("Target account is closed.");
            }

            if (paymentData.accountBalance < paymentData.installmentAmount) {
                throw new SQLException("Insufficient funds for installment payment.");
            }

            updateAccountBalanceForInstallment(paymentData.accountId, paymentData.installmentAmount);
            markInstallmentPaid(paymentData.installmentId);

            double remainingAmount = Math.max(0, paymentData.remainingAmount - paymentData.installmentAmount);
            updateCreditAfterInstallment(creditId, remainingAmount);

            int transactionId = insertInstallmentTransaction(paymentData.installmentAmount);
            insertInstallmentWithdrawalDetails(transactionId, paymentData.accountId);

            getConnection().commit();
            System.out.println("Pay installment JDBC completed successfully.");
        } catch (SQLException e) {
            try {
                getConnection().rollback();
                System.out.println("Pay installment JDBC failed. Rollback executed.");
            } catch (SQLException rollbackException) {
                throw new RuntimeException("Rollback failed.", rollbackException);
            }

            throw new RuntimeException("Pay installment JDBC failed: " + e.getMessage(), e);
        } finally {
            try {
                getConnection().setAutoCommit(true);
            } catch (SQLException e) {
                throw new RuntimeException("Could not reset autoCommit.", e);
            }
        }
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

        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
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

    private CreditInstallmentPaymentDbData getInstallmentForPayment(int creditId, int installmentNumber) throws SQLException {
        String sql = """
                SELECT
                    ci.id AS installment_id,
                    ci.amount AS installment_amount,
                    ci.paid AS installment_paid,
                    c.remaining_amount,
                    c.status,
                    a.id AS account_id,
                    a.balance AS account_balance,
                    a.active AS account_active
                FROM credit_installments ci
                JOIN credits c ON ci.credit_id = c.id
                JOIN accounts a ON c.target_account_id = a.id
                WHERE ci.credit_id = ?
                  AND ci.installment_number = ?
                FOR UPDATE
                """;

        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, creditId);
            statement.setInt(2, installmentNumber);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new CreditInstallmentPaymentDbData(
                            resultSet.getInt("installment_id"),
                            resultSet.getDouble("installment_amount"),
                            resultSet.getBoolean("installment_paid"),
                            resultSet.getDouble("remaining_amount"),
                            CreditStatus.valueOf(resultSet.getString("status")),
                            resultSet.getInt("account_id"),
                            resultSet.getDouble("account_balance"),
                            resultSet.getBoolean("account_active")
                    );
                }
            }
        }

        throw new SQLException("Installment not found for credit id: " + creditId);
    }

    private void updateAccountBalanceForInstallment(int accountId, double installmentAmount) throws SQLException {
        String sql = """
                UPDATE accounts
                SET balance = balance - ?
                WHERE id = ?
                """;

        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setDouble(1, installmentAmount);
            statement.setInt(2, accountId);

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Could not update account balance.");
            }
        }
    }

    private void markInstallmentPaid(int installmentId) throws SQLException {
        String sql = """
                UPDATE credit_installments
                SET paid = true
                WHERE id = ?
                """;

        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, installmentId);

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Could not mark installment as paid.");
            }
        }
    }

    private void updateCreditAfterInstallment(int creditId, double remainingAmount) throws SQLException {
        String sql = """
                UPDATE credits
                SET remaining_amount = ?,
                    status = ?
                WHERE id = ?
                """;

        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setDouble(1, remainingAmount);
            statement.setString(2, remainingAmount == 0 ? CreditStatus.PAID.name() : CreditStatus.ACTIVE.name());
            statement.setInt(3, creditId);

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Could not update credit after installment payment.");
            }
        }
    }

    private int insertInstallmentTransaction(double amount) throws SQLException {
        String sql = """
                INSERT INTO transactions (
                    transaction_type,
                    amount,
                    `timestamp`,
                    description
                )
                VALUES (?, ?, ?, ?)
                """;

        try (PreparedStatement statement = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, TransactionType.WITHDRAWAL.name());
            statement.setDouble(2, amount);
            statement.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            statement.setString(4, "Credit installment payment");

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Could not insert installment transaction.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }

        throw new SQLException("Could not get generated transaction id.");
    }

    private void insertInstallmentWithdrawalDetails(int transactionId, int accountId) throws SQLException {
        String sql = """
                INSERT INTO withdrawal_transactions (
                    transaction_id,
                    source_account_id
                )
                VALUES (?, ?)
                """;

        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, transactionId);
            statement.setInt(2, accountId);

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Could not insert installment withdrawal details.");
            }
        }
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

        try (PreparedStatement statement = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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

        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
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

    private static class CreditInstallmentPaymentDbData {
        private final int installmentId;
        private final double installmentAmount;
        private final boolean installmentPaid;
        private final double remainingAmount;
        private final CreditStatus status;
        private final int accountId;
        private final double accountBalance;
        private final boolean accountActive;

        private CreditInstallmentPaymentDbData(
                int installmentId,
                double installmentAmount,
                boolean installmentPaid,
                double remainingAmount,
                CreditStatus status,
                int accountId,
                double accountBalance,
                boolean accountActive
        ) {
            this.installmentId = installmentId;
            this.installmentAmount = installmentAmount;
            this.installmentPaid = installmentPaid;
            this.remainingAmount = remainingAmount;
            this.status = status;
            this.accountId = accountId;
            this.accountBalance = accountBalance;
            this.accountActive = accountActive;
        }
    }
}
