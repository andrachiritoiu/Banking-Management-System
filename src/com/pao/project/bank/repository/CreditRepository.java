package com.pao.project.bank.repository;

import com.pao.project.bank.model.Credit;
import com.pao.project.bank.model.account.Account;
import com.pao.project.bank.model.enums.CreditStatus;
import com.pao.project.bank.model.enums.CreditType;
import com.pao.project.bank.model.person.Client;
import com.pao.project.bank.repository.Repository;
import com.pao.project.bank.repository.account.CurrentAccountRepository;
import com.pao.project.bank.repository.account.SavingsAccountRepository;
import com.pao.project.bank.repository.person.CorporateClientRepository;
import com.pao.project.bank.repository.person.IndividualClientRepository;
import com.pao.project.bank.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CreditRepository implements Repository<Credit, Integer> {
    private final Connection connection;
    private final IndividualClientRepository individualClientRepository;
    private final CorporateClientRepository corporateClientRepository;
    private final CurrentAccountRepository currentAccountRepository;
    private final SavingsAccountRepository savingsAccountRepository;

    public CreditRepository() {
        this.connection = DatabaseConnection.getInstance().getConnection();
        this.individualClientRepository = new IndividualClientRepository();
        this.corporateClientRepository = new CorporateClientRepository();
        this.currentAccountRepository = new CurrentAccountRepository();
        this.savingsAccountRepository = new SavingsAccountRepository();
    }

    @Override
    public void save(Credit credit) {
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
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, credit.getId());
            statement.setInt(2, credit.getBorrower().getId());
            statement.setInt(3, credit.getTargetAccount().getId());
            statement.setString(4, credit.getType().name());
            statement.setDouble(5, credit.getPrincipalAmount());
            statement.setDouble(6, credit.getAnnualInterestRate());
            statement.setInt(7, credit.getDurationInMonths());
            setNullableDate(statement, 8, credit.getStartDate());
            statement.setDouble(9, credit.getRemainingAmount());
            statement.setString(10, credit.getStatus().name());

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not save credit.", e);
        }
    }

    @Override
    public Optional<Credit> findById(Integer id) {
        String sql = """
                SELECT
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
                FROM credits
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapToCredit(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find credit by id.", e);
        }

        return Optional.empty();
    }

    @Override
    public List<Credit> findAll() {
        String sql = """
                SELECT
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
                FROM credits
                ORDER BY id
                """;

        List<Credit> credits = new ArrayList<>();

        try (
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                credits.add(mapToCredit(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find all credits.", e);
        }

        return credits;
    }

    @Override
    public void update(Credit credit) {
        String sql = """
                UPDATE credits
                SET borrower_id = ?,
                    target_account_id = ?,
                    credit_type = ?,
                    principal_amount = ?,
                    annual_interest_rate = ?,
                    duration_in_months = ?,
                    start_date = ?,
                    remaining_amount = ?,
                    status = ?
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, credit.getBorrower().getId());
            statement.setInt(2, credit.getTargetAccount().getId());
            statement.setString(3, credit.getType().name());
            statement.setDouble(4, credit.getPrincipalAmount());
            statement.setDouble(5, credit.getAnnualInterestRate());
            statement.setInt(6, credit.getDurationInMonths());
            setNullableDate(statement, 7, credit.getStartDate());
            statement.setDouble(8, credit.getRemainingAmount());
            statement.setString(9, credit.getStatus().name());
            statement.setInt(10, credit.getId());

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not update credit.", e);
        }
    }

    @Override
    public void delete(Integer id) {
        String sql = """
                DELETE FROM credits
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete credit.", e);
        }
    }

    public void updateStatus(int id, CreditStatus status) {
        String sql = """
                UPDATE credits
                SET status = ?
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setInt(2, id);

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not update credit status.", e);
        }
    }

    public void updateRemainingAmount(int id, double remainingAmount) {
        String sql = """
                UPDATE credits
                SET remaining_amount = ?
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDouble(1, remainingAmount);
            statement.setInt(2, id);

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not update remaining amount.", e);
        }
    }

    private Credit mapToCredit(ResultSet resultSet) throws SQLException {
        Client borrower = loadClient(resultSet.getInt("borrower_id"));
        Account targetAccount = loadAccount(resultSet.getInt("target_account_id"));

        return new Credit(
                resultSet.getInt("id"),
                borrower,
                targetAccount,
                CreditType.valueOf(resultSet.getString("credit_type")),
                resultSet.getDouble("principal_amount"),
                resultSet.getDouble("annual_interest_rate"),
                resultSet.getInt("duration_in_months"),
                resultSet.getDate("start_date") == null ? null : resultSet.getDate("start_date").toLocalDate(),
                resultSet.getDouble("remaining_amount"),
                CreditStatus.valueOf(resultSet.getString("status"))
        );
    }

    private void setNullableDate(PreparedStatement statement, int parameterIndex, java.time.LocalDate date) throws SQLException {
        if (date == null) {
            statement.setNull(parameterIndex, Types.DATE);
        } else {
            statement.setDate(parameterIndex, Date.valueOf(date));
        }
    }

    private Client loadClient(int clientId) {
        Optional<? extends Client> individualClient = individualClientRepository.findById(clientId);
        if (individualClient.isPresent()) {
            return individualClient.get();
        }

        Optional<? extends Client> corporateClient = corporateClientRepository.findById(clientId);
        if (corporateClient.isPresent()) {
            return corporateClient.get();
        }

        throw new RuntimeException("Client not found for id: " + clientId);
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
}
