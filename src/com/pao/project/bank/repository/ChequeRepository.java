package com.pao.project.bank.repository;

import com.pao.project.bank.model.Cheque;
import com.pao.project.bank.model.account.Account;
import com.pao.project.bank.model.enums.ChequeStatus;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ChequeRepository implements Repository<Cheque, String> {
    private final Connection connection;
    private final CurrentAccountRepository currentAccountRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final IndividualClientRepository individualClientRepository;
    private final CorporateClientRepository corporateClientRepository;

    public ChequeRepository() {
        this.connection = DatabaseConnection.getInstance().getConnection();
        this.currentAccountRepository = new CurrentAccountRepository();
        this.savingsAccountRepository = new SavingsAccountRepository();
        this.individualClientRepository = new IndividualClientRepository();
        this.corporateClientRepository = new CorporateClientRepository();
    }

    @Override
    public void save(Cheque cheque) {
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
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, cheque.getSeries());
            statement.setInt(2, cheque.getIssuerAccount().getId());
            statement.setInt(3, cheque.getBeneficiary().getId());
            statement.setDouble(4, cheque.getAmount());
            statement.setDate(5, Date.valueOf(cheque.getIssueDate()));
            statement.setDate(6, Date.valueOf(cheque.getExpiryDate()));
            statement.setString(7, cheque.getStatus().name());

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not save cheque.", e);
        }
    }

    @Override
    public Optional<Cheque> findById(String series) {
        String sql = """
                SELECT
                    series,
                    issuer_account_id,
                    beneficiary_client_id,
                    amount,
                    issue_date,
                    expiry_date,
                    status
                FROM cheques
                WHERE series = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, series);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapToCheque(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find cheque by series.", e);
        }

        return Optional.empty();
    }

    @Override
    public List<Cheque> findAll() {
        String sql = """
                SELECT
                    series,
                    issuer_account_id,
                    beneficiary_client_id,
                    amount,
                    issue_date,
                    expiry_date,
                    status
                FROM cheques
                ORDER BY issue_date DESC
                """;

        List<Cheque> cheques = new ArrayList<>();

        try (
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                cheques.add(mapToCheque(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find all cheques.", e);
        }

        return cheques;
    }

    @Override
    public void update(Cheque cheque) {
        String sql = """
                UPDATE cheques
                SET issuer_account_id = ?,
                    beneficiary_client_id = ?,
                    amount = ?,
                    issue_date = ?,
                    expiry_date = ?,
                    status = ?
                WHERE series = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, cheque.getIssuerAccount().getId());
            statement.setInt(2, cheque.getBeneficiary().getId());
            statement.setDouble(3, cheque.getAmount());
            statement.setDate(4, Date.valueOf(cheque.getIssueDate()));
            statement.setDate(5, Date.valueOf(cheque.getExpiryDate()));
            statement.setString(6, cheque.getStatus().name());
            statement.setString(7, cheque.getSeries());

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not update cheque.", e);
        }
    }

    @Override
    public void delete(String series) {
        String sql = """
                DELETE FROM cheques
                WHERE series = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, series);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete cheque.", e);
        }
    }

    public void updateStatus(String series, ChequeStatus status) {
        String sql = """
                UPDATE cheques
                SET status = ?
                WHERE series = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setString(2, series);

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not update cheque status.", e);
        }
    }

    private Cheque mapToCheque(ResultSet resultSet) throws SQLException {
        Account issuerAccount = loadAccount(resultSet.getInt("issuer_account_id"));
        Client beneficiary = loadClient(resultSet.getInt("beneficiary_client_id"));

        return new Cheque(
                resultSet.getString("series"),
                issuerAccount,
                beneficiary,
                resultSet.getDouble("amount"),
                resultSet.getDate("issue_date").toLocalDate(),
                resultSet.getDate("expiry_date").toLocalDate(),
                ChequeStatus.valueOf(resultSet.getString("status"))
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
}