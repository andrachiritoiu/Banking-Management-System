package com.pao.project.bank.repository.transaction;

import com.pao.project.bank.model.account.Account;
import com.pao.project.bank.model.enums.TransactionType;
import com.pao.project.bank.model.transaction.Withdrawal;
import com.pao.project.bank.repository.Repository;
import com.pao.project.bank.repository.account.CurrentAccountRepository;
import com.pao.project.bank.repository.account.SavingsAccountRepository;
import com.pao.project.bank.repository.helper.TransactionRepositoryHelper;
import com.pao.project.bank.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WithdrawalRepository implements Repository<Withdrawal, Integer> {
    private final Connection connection;
    private final TransactionRepositoryHelper transactionHelper;
    private final CurrentAccountRepository currentAccountRepository;
    private final SavingsAccountRepository savingsAccountRepository;

    public WithdrawalRepository() {
        this.connection = DatabaseConnection.getInstance().getConnection();
        this.transactionHelper = new TransactionRepositoryHelper();
        this.currentAccountRepository = new CurrentAccountRepository();
        this.savingsAccountRepository = new SavingsAccountRepository();
    }

    @Override
    public void save(Withdrawal withdrawal) {
        String sql = """
                INSERT INTO withdrawal_transactions (
                    transaction_id,
                    source_account_id
                )
                VALUES (?, ?)
                """;

        try {
            connection.setAutoCommit(false);
            transactionHelper.insertTransaction(connection, withdrawal);

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, withdrawal.getId());
                statement.setInt(2, withdrawal.getSourceAccount().getId());
                statement.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            rollback();
            throw new RuntimeException("Could not save withdrawal.", e);
        } finally {
            resetAutoCommit();
        }
    }

    @Override
    public Optional<Withdrawal> findById(Integer id) {
        String sql = """
                SELECT
                    t.id,
                    t.amount,
                    t.`timestamp`,
                    t.description,
                    wt.source_account_id
                FROM transactions t
                JOIN withdrawal_transactions wt ON t.id = wt.transaction_id
                WHERE t.id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapToWithdrawal(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find withdrawal by id.", e);
        }

        return Optional.empty();
    }

    @Override
    public List<Withdrawal> findAll() {
        String sql = """
                SELECT
                    t.id,
                    t.amount,
                    t.`timestamp`,
                    t.description,
                    wt.source_account_id
                FROM transactions t
                JOIN withdrawal_transactions wt ON t.id = wt.transaction_id
                ORDER BY t.`timestamp` DESC
                """;

        List<Withdrawal> withdrawals = new ArrayList<>();

        try (
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                withdrawals.add(mapToWithdrawal(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find all withdrawals.", e);
        }

        return withdrawals;
    }

    @Override
    public void update(Withdrawal withdrawal) {
        String sql = """
                UPDATE withdrawal_transactions
                SET source_account_id = ?
                WHERE transaction_id = ?
                """;

        try {
            connection.setAutoCommit(false);
            transactionHelper.updateTransaction(connection, withdrawal);

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, withdrawal.getSourceAccount().getId());
                statement.setInt(2, withdrawal.getId());
                statement.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            rollback();
            throw new RuntimeException("Could not update withdrawal.", e);
        } finally {
            resetAutoCommit();
        }
    }

    @Override
    public void delete(Integer id) {
        try {
            connection.setAutoCommit(false);
            transactionHelper.deleteTransaction(connection, id);
            connection.commit();
        } catch (SQLException e) {
            rollback();
            throw new RuntimeException("Could not delete withdrawal.", e);
        } finally {
            resetAutoCommit();
        }
    }

    private Withdrawal mapToWithdrawal(ResultSet resultSet) throws SQLException {
        Account sourceAccount = loadAccount(resultSet.getInt("source_account_id"));

        return new Withdrawal(
                resultSet.getInt("id"),
                TransactionType.WITHDRAWAL,
                resultSet.getDouble("amount"),
                resultSet.getTimestamp("timestamp").toLocalDateTime(),
                resultSet.getString("description"),
                sourceAccount
        );
    }

    private Account loadAccount(int accountId) {
        Optional<? extends Account> currentAccount = currentAccountRepository.findById(accountId);
        if (currentAccount.isPresent()) {
            return currentAccount.get();
        }

        Optional<? extends Account> savingsAccount = savingsAccountRepository.findById(accountId);
        if (savingsAccount.isPresent()) {
            return savingsAccount.get();
        }

        throw new RuntimeException("Account not found for id: " + accountId);
    }

    private void rollback() {
        try {
            connection.rollback();
        } catch (SQLException e) {
            throw new RuntimeException("Rollback failed.", e);
        }
    }

    private void resetAutoCommit() {
        try {
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException("Could not reset auto commit.", e);
        }
    }
}
