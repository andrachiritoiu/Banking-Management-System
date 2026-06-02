package com.pao.project.bank.repository;

import com.pao.project.bank.model.AccountStatement;
import com.pao.project.bank.model.account.Account;
import com.pao.project.bank.model.transaction.Transaction;
import com.pao.project.bank.repository.Repository;
import com.pao.project.bank.repository.TransactionRepository;
import com.pao.project.bank.repository.account.CurrentAccountRepository;
import com.pao.project.bank.repository.account.SavingsAccountRepository;
import com.pao.project.bank.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AccountStatementRepository implements Repository<AccountStatement, Integer> {
    private final Connection connection;
    private final CurrentAccountRepository currentAccountRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final TransactionRepository transactionRepository;

    public AccountStatementRepository() {
        this.connection = DatabaseConnection.getInstance().getConnection();
        this.currentAccountRepository = new CurrentAccountRepository();
        this.savingsAccountRepository = new SavingsAccountRepository();
        this.transactionRepository = new TransactionRepository();
    }

    @Override
    public void save(AccountStatement statement) {
        String insertStatementSql = """
                INSERT INTO account_statements (
                    account_id,
                    generated_at,
                    total_inflows,
                    total_outflows,
                    initial_balance,
                    final_balance
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try {
            connection.setAutoCommit(false);

            try (PreparedStatement preparedStatement = connection.prepareStatement(insertStatementSql, Statement.RETURN_GENERATED_KEYS)) {
                preparedStatement.setInt(1, statement.getAccount().getId());
                preparedStatement.setDate(2, Date.valueOf(statement.getGeneratedAt()));
                preparedStatement.setDouble(3, statement.getTotalInflows());
                preparedStatement.setDouble(4, statement.getTotalOutflows());
                preparedStatement.setDouble(5, statement.getInitialBalance());
                preparedStatement.setDouble(6, statement.getFinalBalance());

                preparedStatement.executeUpdate();

                try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        statement.setId(generatedKeys.getInt(1));
                    } else {
                        throw new SQLException("Could not get generated statement id.");
                    }
                }
            }

            insertStatementTransactions(statement);

            connection.commit();
        } catch (SQLException e) {
            rollback();
            throw new RuntimeException("Could not save account statement.", e);
        } finally {
            resetAutoCommit();
        }
    }

    @Override
    public Optional<AccountStatement> findById(Integer id) {
        String sql = """
                SELECT
                    id,
                    account_id,
                    generated_at,
                    total_inflows,
                    total_outflows,
                    initial_balance,
                    final_balance
                FROM account_statements
                WHERE id = ?
                """;

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapToAccountStatement(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find account statement by id.", e);
        }

        return Optional.empty();
    }

    @Override
    public List<AccountStatement> findAll() {
        String sql = """
                SELECT
                    id,
                    account_id,
                    generated_at,
                    total_inflows,
                    total_outflows,
                    initial_balance,
                    final_balance
                FROM account_statements
                ORDER BY generated_at DESC, id DESC
                """;

        List<AccountStatement> statements = new ArrayList<>();

        try (
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                ResultSet resultSet = preparedStatement.executeQuery()
        ) {
            while (resultSet.next()) {
                statements.add(mapToAccountStatement(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find all account statements.", e);
        }

        return statements;
    }

    @Override
    public void update(AccountStatement statement) {
        String updateStatementSql = """
                UPDATE account_statements
                SET account_id = ?,
                    generated_at = ?,
                    total_inflows = ?,
                    total_outflows = ?,
                    initial_balance = ?,
                    final_balance = ?
                WHERE id = ?
                """;

        try {
            connection.setAutoCommit(false);

            try (PreparedStatement preparedStatement = connection.prepareStatement(updateStatementSql)) {
                preparedStatement.setInt(1, statement.getAccount().getId());
                preparedStatement.setDate(2, Date.valueOf(statement.getGeneratedAt()));
                preparedStatement.setDouble(3, statement.getTotalInflows());
                preparedStatement.setDouble(4, statement.getTotalOutflows());
                preparedStatement.setDouble(5, statement.getInitialBalance());
                preparedStatement.setDouble(6, statement.getFinalBalance());
                preparedStatement.setInt(7, statement.getId());

                preparedStatement.executeUpdate();
            }

            deleteStatementTransactions(statement.getId());
            insertStatementTransactions(statement);

            connection.commit();
        } catch (SQLException e) {
            rollback();
            throw new RuntimeException("Could not update account statement.", e);
        } finally {
            resetAutoCommit();
        }
    }

    @Override
    public void delete(Integer id) {
        String sql = """
                DELETE FROM account_statements
                WHERE id = ?
                """;

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete account statement.", e);
        }
    }

    private void insertStatementTransactions(AccountStatement statement) throws SQLException {
        String sql = """
                INSERT INTO account_statement_transactions (
                    statement_id,
                    transaction_id
                )
                VALUES (?, ?)
                """;

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            for (Transaction transaction : statement.getTransactions()) {
                preparedStatement.setInt(1, statement.getId());
                preparedStatement.setInt(2, transaction.getId());
                preparedStatement.addBatch();
            }

            preparedStatement.executeBatch();
        }
    }

    private void deleteStatementTransactions(int statementId) throws SQLException {
        String sql = """
                DELETE FROM account_statement_transactions
                WHERE statement_id = ?
                """;

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, statementId);
            preparedStatement.executeUpdate();
        }
    }

    private List<Transaction> findTransactionsForStatement(int statementId) throws SQLException {
        String sql = """
                SELECT transaction_id
                FROM account_statement_transactions
                WHERE statement_id = ?
                """;

        List<Transaction> transactions = new ArrayList<>();

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, statementId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    int transactionId = resultSet.getInt("transaction_id");

                    Transaction transaction = transactionRepository.findById(transactionId)
                            .orElseThrow(() -> new RuntimeException("Transaction not found for id: " + transactionId));

                    transactions.add(transaction);
                }
            }
        }

        return transactions;
    }

    private AccountStatement mapToAccountStatement(ResultSet resultSet) throws SQLException {
        int statementId = resultSet.getInt("id");
        Account account = loadAccount(resultSet.getInt("account_id"));
        List<Transaction> transactions = findTransactionsForStatement(statementId);

        return new AccountStatement(
                statementId,
                account,
                resultSet.getDate("generated_at").toLocalDate(),
                transactions,
                resultSet.getDouble("total_inflows"),
                resultSet.getDouble("total_outflows"),
                resultSet.getDouble("initial_balance"),
                resultSet.getDouble("final_balance")
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