package com.pao.project.bank.repository.transaction;

import com.pao.project.bank.model.account.Account;
import com.pao.project.bank.model.enums.TransactionType;
import com.pao.project.bank.model.transaction.Deposit;
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

public class DepositRepository implements Repository<Deposit, Integer> {
    private final Connection connection;
    private final TransactionRepositoryHelper transactionHelper;
    private final CurrentAccountRepository currentAccountRepository;
    private final SavingsAccountRepository savingsAccountRepository;

    public DepositRepository() {
        this.connection = DatabaseConnection.getInstance().getConnection();
        this.transactionHelper = new TransactionRepositoryHelper();
        this.currentAccountRepository = new CurrentAccountRepository();
        this.savingsAccountRepository = new SavingsAccountRepository();
    }

    @Override
    public void save(Deposit deposit) {
        String sql = """
                INSERT INTO deposit_transactions (
                    transaction_id,
                    destination_account_id
                )
                VALUES (?, ?)
                """;

        try {
            connection.setAutoCommit(false);
            transactionHelper.insertTransaction(connection, deposit);

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, deposit.getId());
                statement.setInt(2, deposit.getDestinationAccount().getId());
                statement.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            rollback();
            throw new RuntimeException("Could not save deposit.", e);
        } finally {
            resetAutoCommit();
        }
    }

    @Override
    public Optional<Deposit> findById(Integer id) {
        String sql = """
                SELECT
                    t.id,
                    t.amount,
                    t.`timestamp`,
                    t.description,
                    dt.destination_account_id
                FROM transactions t
                JOIN deposit_transactions dt ON t.id = dt.transaction_id
                WHERE t.id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapToDeposit(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find deposit by id.", e);
        }

        return Optional.empty();
    }

    @Override
    public List<Deposit> findAll() {
        String sql = """
                SELECT
                    t.id,
                    t.amount,
                    t.`timestamp`,
                    t.description,
                    dt.destination_account_id
                FROM transactions t
                JOIN deposit_transactions dt ON t.id = dt.transaction_id
                ORDER BY t.`timestamp` DESC
                """;

        List<Deposit> deposits = new ArrayList<>();

        try (
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                deposits.add(mapToDeposit(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find all deposits.", e);
        }

        return deposits;
    }

    @Override
    public void update(Deposit deposit) {
        String sql = """
                UPDATE deposit_transactions
                SET destination_account_id = ?
                WHERE transaction_id = ?
                """;

        try {
            connection.setAutoCommit(false);
            transactionHelper.updateTransaction(connection, deposit);

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, deposit.getDestinationAccount().getId());
                statement.setInt(2, deposit.getId());
                statement.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            rollback();
            throw new RuntimeException("Could not update deposit.", e);
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
            throw new RuntimeException("Could not delete deposit.", e);
        } finally {
            resetAutoCommit();
        }
    }

    private Deposit mapToDeposit(ResultSet resultSet) throws SQLException {
        Account destinationAccount = loadAccount(resultSet.getInt("destination_account_id"));

        return new Deposit(
                resultSet.getInt("id"),
                TransactionType.DEPOSIT,
                resultSet.getDouble("amount"),
                resultSet.getTimestamp("timestamp").toLocalDateTime(),
                resultSet.getString("description"),
                destinationAccount
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
