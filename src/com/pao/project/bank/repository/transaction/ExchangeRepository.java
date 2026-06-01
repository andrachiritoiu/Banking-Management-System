package com.pao.project.bank.repository.transaction;

import com.pao.project.bank.model.account.Account;
import com.pao.project.bank.model.enums.Currency;
import com.pao.project.bank.model.transaction.Exchange;
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

public class ExchangeRepository implements Repository<Exchange, Integer> {
    private final Connection connection;
    private final TransactionRepositoryHelper transactionHelper;
    private final CurrentAccountRepository currentAccountRepository;
    private final SavingsAccountRepository savingsAccountRepository;

    public ExchangeRepository() {
        this.connection = DatabaseConnection.getInstance().getConnection();
        this.transactionHelper = new TransactionRepositoryHelper();
        this.currentAccountRepository = new CurrentAccountRepository();
        this.savingsAccountRepository = new SavingsAccountRepository();
    }

    @Override
    public void save(Exchange exchange) {
        String sql = """
                INSERT INTO exchange_transactions (
                    transaction_id,
                    source_account_id,
                    destination_account_id,
                    destination_amount,
                    from_currency,
                    to_currency,
                    exchange_rate
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try {
            connection.setAutoCommit(false);
            transactionHelper.insertTransaction(connection, exchange);

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, exchange.getId());
                statement.setInt(2, exchange.getSourceAccount().getId());
                statement.setInt(3, exchange.getDestinationAccount().getId());
                statement.setDouble(4, exchange.getDestinationAmount());
                statement.setString(5, exchange.getFromCurrency().name());
                statement.setString(6, exchange.getToCurrency().name());
                statement.setDouble(7, exchange.getExchangeRate());
                statement.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            rollback();
            throw new RuntimeException("Could not save exchange.", e);
        } finally {
            resetAutoCommit();
        }
    }

    @Override
    public Optional<Exchange> findById(Integer id) {
        String sql = """
                SELECT
                    t.id,
                    t.amount,
                    t.`timestamp`,
                    t.description,
                    et.source_account_id,
                    et.destination_account_id,
                    et.destination_amount,
                    et.from_currency,
                    et.to_currency,
                    et.exchange_rate
                FROM transactions t
                JOIN exchange_transactions et ON t.id = et.transaction_id
                WHERE t.id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapToExchange(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find exchange by id.", e);
        }

        return Optional.empty();
    }

    @Override
    public List<Exchange> findAll() {
        String sql = """
                SELECT
                    t.id,
                    t.amount,
                    t.`timestamp`,
                    t.description,
                    et.source_account_id,
                    et.destination_account_id,
                    et.destination_amount,
                    et.from_currency,
                    et.to_currency,
                    et.exchange_rate
                FROM transactions t
                JOIN exchange_transactions et ON t.id = et.transaction_id
                ORDER BY t.`timestamp` DESC
                """;

        List<Exchange> exchanges = new ArrayList<>();

        try (
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                exchanges.add(mapToExchange(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find all exchanges.", e);
        }

        return exchanges;
    }

    @Override
    public void update(Exchange exchange) {
        String sql = """
                UPDATE exchange_transactions
                SET source_account_id = ?,
                    destination_account_id = ?,
                    destination_amount = ?,
                    from_currency = ?,
                    to_currency = ?,
                    exchange_rate = ?
                WHERE transaction_id = ?
                """;

        try {
            connection.setAutoCommit(false);
            transactionHelper.updateTransaction(connection, exchange);

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, exchange.getSourceAccount().getId());
                statement.setInt(2, exchange.getDestinationAccount().getId());
                statement.setDouble(3, exchange.getDestinationAmount());
                statement.setString(4, exchange.getFromCurrency().name());
                statement.setString(5, exchange.getToCurrency().name());
                statement.setDouble(6, exchange.getExchangeRate());
                statement.setInt(7, exchange.getId());
                statement.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            rollback();
            throw new RuntimeException("Could not update exchange.", e);
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
            throw new RuntimeException("Could not delete exchange.", e);
        } finally {
            resetAutoCommit();
        }
    }

    private Exchange mapToExchange(ResultSet resultSet) throws SQLException {
        Account sourceAccount = loadAccount(resultSet.getInt("source_account_id"));
        Account destinationAccount = loadAccount(resultSet.getInt("destination_account_id"));

        return new Exchange(
                resultSet.getInt("id"),
                sourceAccount,
                destinationAccount,
                resultSet.getDouble("amount"),
                resultSet.getDouble("destination_amount"),
                Currency.valueOf(resultSet.getString("from_currency")),
                Currency.valueOf(resultSet.getString("to_currency")),
                resultSet.getDouble("exchange_rate"),
                resultSet.getTimestamp("timestamp").toLocalDateTime(),
                resultSet.getString("description")
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
