package com.pao.project.bank.repository.account;

import com.pao.project.bank.util.DatabaseConnection;
import com.pao.project.bank.model.IBAN;
import com.pao.project.bank.model.account.SavingsAccount;
import com.pao.project.bank.model.person.Client;
import com.pao.project.bank.repository.Repository;
import com.pao.project.bank.repository.helper.AccountRepositoryHelper;
import com.pao.project.bank.repository.person.CorporateClientRepository;
import com.pao.project.bank.repository.person.IndividualClientRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SavingsAccountRepository implements Repository<SavingsAccount, Integer> {
    private final Connection connection;
    private final AccountRepositoryHelper accountHelper;
    private final IndividualClientRepository individualClientRepository;
    private final CorporateClientRepository corporateClientRepository;

    public SavingsAccountRepository() {
        this.connection = DatabaseConnection.getInstance().getConnection();
        this.accountHelper = new AccountRepositoryHelper();
        this.individualClientRepository = new IndividualClientRepository();
        this.corporateClientRepository = new CorporateClientRepository();
    }

    @Override
    public void save(SavingsAccount account) {
        String sql = """
                INSERT INTO savings_accounts (
                    account_id,
                    interest_rate,
                    withdrawals_this_month
                )
                VALUES (?, ?, ?)
                """;

        try {
            connection.setAutoCommit(false);

            accountHelper.insertAccount(
                    connection,
                    account,
                    AccountRepositoryHelper.ACCOUNT_TYPE_SAVINGS
            );

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, account.getId());
                statement.setDouble(2, account.getInterestRate());
                statement.setInt(3, account.getWithdrawalsThisMonth());

                statement.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            rollback();
            throw new RuntimeException("Could not save savings account.", e);
        } finally {
            resetAutoCommit();
        }
    }

    @Override
    public Optional<SavingsAccount> findById(Integer id) {
        String sql = """
                SELECT
                    a.id,
                    a.iban,
                    a.balance,
                    a.currency,
                    a.active,
                    a.opening_date,
                    a.client_id,
                    sa.interest_rate,
                    sa.withdrawals_this_month
                FROM accounts a
                JOIN savings_accounts sa ON a.id = sa.account_id
                WHERE a.id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapToSavingsAccount(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find savings account by id.", e);
        }

        return Optional.empty();
    }

    @Override
    public List<SavingsAccount> findAll() {
        String sql = """
                SELECT
                    a.id,
                    a.iban,
                    a.balance,
                    a.currency,
                    a.active,
                    a.opening_date,
                    a.client_id,
                    sa.interest_rate,
                    sa.withdrawals_this_month
                FROM accounts a
                JOIN savings_accounts sa ON a.id = sa.account_id
                ORDER BY a.iban
                """;

        List<SavingsAccount> accounts = new ArrayList<>();

        try (
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                accounts.add(mapToSavingsAccount(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find all savings accounts.", e);
        }

        return accounts;
    }

    @Override
    public void update(SavingsAccount account) {
        String sql = """
                UPDATE savings_accounts
                SET interest_rate = ?,
                    withdrawals_this_month = ?
                WHERE account_id = ?
                """;

        try {
            connection.setAutoCommit(false);

            accountHelper.updateAccount(connection, account);

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setDouble(1, account.getInterestRate());
                statement.setInt(2, account.getWithdrawalsThisMonth());
                statement.setInt(3, account.getId());

                statement.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            rollback();
            throw new RuntimeException("Could not update savings account.", e);
        } finally {
            resetAutoCommit();
        }
    }

    @Override
    public void delete(Integer id) {
        try {
            connection.setAutoCommit(false);

            accountHelper.deleteAccount(connection, id);

            connection.commit();
        } catch (SQLException e) {
            rollback();
            throw new RuntimeException("Could not delete savings account.", e);
        } finally {
            resetAutoCommit();
        }
    }

    private SavingsAccount mapToSavingsAccount(ResultSet resultSet) throws SQLException {
        Client owner = loadOwner(resultSet.getInt("client_id"));

        return new SavingsAccount(
                resultSet.getInt("id"),
                new IBAN(resultSet.getString("iban")),
                resultSet.getDouble("balance"),
                resultSet.getString("currency"),
                owner,
                resultSet.getBoolean("active"),
                resultSet.getDate("opening_date").toLocalDate(),
                resultSet.getDouble("interest_rate"),
                resultSet.getInt("withdrawals_this_month")
        );
    }

    private Client loadOwner(int clientId) {
        Optional<? extends Client> individualClient = individualClientRepository.findById(clientId);
        if (individualClient.isPresent()) {
            return individualClient.get();
        }

        Optional<? extends Client> corporateClient = corporateClientRepository.findById(clientId);
        if (corporateClient.isPresent()) {
            return corporateClient.get();
        }

        throw new RuntimeException("Owner client not found for id: " + clientId);
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