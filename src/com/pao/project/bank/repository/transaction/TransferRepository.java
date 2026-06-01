package com.pao.project.bank.repository.transaction;

import com.pao.project.bank.model.account.Account;
import com.pao.project.bank.model.enums.TransactionType;
import com.pao.project.bank.model.transaction.Transfer;
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

public class TransferRepository implements Repository<Transfer, Integer> {
    private final Connection connection;
    private final TransactionRepositoryHelper transactionHelper;
    private final CurrentAccountRepository currentAccountRepository;
    private final SavingsAccountRepository savingsAccountRepository;

    public TransferRepository() {
        this.connection = DatabaseConnection.getInstance().getConnection();
        this.transactionHelper = new TransactionRepositoryHelper();
        this.currentAccountRepository = new CurrentAccountRepository();
        this.savingsAccountRepository = new SavingsAccountRepository();
    }

    @Override
    public void save(Transfer transfer) {
        String sql = """
                INSERT INTO transfer_transactions (
                    transaction_id,
                    source_account_id,
                    destination_account_id
                )
                VALUES (?, ?, ?)
                """;

        try {
            connection.setAutoCommit(false);
            transactionHelper.insertTransaction(connection, transfer);

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, transfer.getId());
                statement.setInt(2, transfer.getSourceAccount().getId());
                statement.setInt(3, transfer.getDestinationAccount().getId());
                statement.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            rollback();
            throw new RuntimeException("Could not save transfer.", e);
        } finally {
            resetAutoCommit();
        }
    }

    @Override
    public Optional<Transfer> findById(Integer id) {
        String sql = """
                SELECT
                    t.id,
                    t.amount,
                    t.`timestamp`,
                    t.description,
                    tt.source_account_id,
                    tt.destination_account_id
                FROM transactions t
                JOIN transfer_transactions tt ON t.id = tt.transaction_id
                WHERE t.id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapToTransfer(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find transfer by id.", e);
        }

        return Optional.empty();
    }

    @Override
    public List<Transfer> findAll() {
        String sql = """
                SELECT
                    t.id,
                    t.amount,
                    t.`timestamp`,
                    t.description,
                    tt.source_account_id,
                    tt.destination_account_id
                FROM transactions t
                JOIN transfer_transactions tt ON t.id = tt.transaction_id
                ORDER BY t.`timestamp` DESC
                """;

        List<Transfer> transfers = new ArrayList<>();

        try (
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                transfers.add(mapToTransfer(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find all transfers.", e);
        }

        return transfers;
    }

    @Override
    public void update(Transfer transfer) {
        String sql = """
                UPDATE transfer_transactions
                SET source_account_id = ?,
                    destination_account_id = ?
                WHERE transaction_id = ?
                """;

        try {
            connection.setAutoCommit(false);
            transactionHelper.updateTransaction(connection, transfer);

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, transfer.getSourceAccount().getId());
                statement.setInt(2, transfer.getDestinationAccount().getId());
                statement.setInt(3, transfer.getId());
                statement.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            rollback();
            throw new RuntimeException("Could not update transfer.", e);
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
            throw new RuntimeException("Could not delete transfer.", e);
        } finally {
            resetAutoCommit();
        }
    }

    private Transfer mapToTransfer(ResultSet resultSet) throws SQLException {
        Account sourceAccount = loadAccount(resultSet.getInt("source_account_id"));
        Account destinationAccount = loadAccount(resultSet.getInt("destination_account_id"));

        return new Transfer(
                resultSet.getInt("id"),
                TransactionType.TRANSFER,
                resultSet.getDouble("amount"),
                resultSet.getTimestamp("timestamp").toLocalDateTime(),
                resultSet.getString("description"),
                destinationAccount,
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

