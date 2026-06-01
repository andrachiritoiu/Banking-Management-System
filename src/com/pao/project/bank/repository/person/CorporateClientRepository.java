package com.pao.project.bank.repository.person;

import com.pao.project.bank.util.DatabaseConnection;
import com.pao.project.bank.model.person.CorporateClient;
import com.pao.project.bank.model.person.IndividualClient;
import com.pao.project.bank.repository.Repository;
import com.pao.project.bank.repository.helper.ClientRepositoryHelper;
import com.pao.project.bank.repository.helper.PersonRepositoryHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CorporateClientRepository implements Repository<CorporateClient, Integer> {
    private final Connection connection;
    private final PersonRepositoryHelper personHelper;
    private final ClientRepositoryHelper clientHelper;

    public CorporateClientRepository() {
        this.connection = DatabaseConnection.getInstance().getConnection();
        this.personHelper = new PersonRepositoryHelper();
        this.clientHelper = new ClientRepositoryHelper();
    }

    @Override
    public void save(CorporateClient client) {
        String sql = """
                INSERT INTO corporate_clients (
                    client_id,
                    company_name,
                    cui,
                    legal_representative_id
                )
                VALUES (?, ?, ?, ?)
                """;

        try {
            connection.setAutoCommit(false);

            personHelper.insertPerson(
                    connection,
                    client.getId(),
                    PersonRepositoryHelper.PERSON_TYPE_CLIENT,
                    client.getEmail(),
                    client.getPhoneNumber()
            );

            clientHelper.insertClient(
                    connection,
                    client.getId(),
                    client.getClientCode(),
                    ClientRepositoryHelper.CLIENT_TYPE_CORPORATE,
                    client.isActive()
            );

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, client.getId());
                statement.setString(2, client.getCompanyName());
                statement.setString(3, client.getCui());
                statement.setInt(4, client.getLegalRepresentative().getId());

                statement.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            rollback();
            throw new RuntimeException("Could not save corporate client.", e);
        } finally {
            resetAutoCommit();
        }
    }

    @Override
    public Optional<CorporateClient> findById(Integer id) {
        String sql = """
                SELECT
                    p.id AS corporate_id,
                    p.email AS corporate_email,
                    p.phone_number AS corporate_phone_number,
                    c.client_code AS corporate_client_code,
                    c.active AS corporate_active,
                    cc.company_name,
                    cc.cui,

                    lp.id AS legal_id,
                    lp.email AS legal_email,
                    lp.phone_number AS legal_phone_number,
                    lc.client_code AS legal_client_code,
                    lc.active AS legal_active,
                    lic.first_name AS legal_first_name,
                    lic.last_name AS legal_last_name,
                    lic.cnp AS legal_cnp

                FROM corporate_clients cc
                JOIN clients c ON cc.client_id = c.id
                JOIN persons p ON c.id = p.id

                JOIN individual_clients lic ON cc.legal_representative_id = lic.client_id
                JOIN clients lc ON lic.client_id = lc.id
                JOIN persons lp ON lc.id = lp.id

                WHERE cc.client_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapToCorporateClient(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find corporate client by id.", e);
        }

        return Optional.empty();
    }

    @Override
    public List<CorporateClient> findAll() {
        String sql = """
                SELECT
                    p.id AS corporate_id,
                    p.email AS corporate_email,
                    p.phone_number AS corporate_phone_number,
                    c.client_code AS corporate_client_code,
                    c.active AS corporate_active,
                    cc.company_name,
                    cc.cui,

                    lp.id AS legal_id,
                    lp.email AS legal_email,
                    lp.phone_number AS legal_phone_number,
                    lc.client_code AS legal_client_code,
                    lc.active AS legal_active,
                    lic.first_name AS legal_first_name,
                    lic.last_name AS legal_last_name,
                    lic.cnp AS legal_cnp

                FROM corporate_clients cc
                JOIN clients c ON cc.client_id = c.id
                JOIN persons p ON c.id = p.id

                JOIN individual_clients lic ON cc.legal_representative_id = lic.client_id
                JOIN clients lc ON lic.client_id = lc.id
                JOIN persons lp ON lc.id = lp.id

                ORDER BY cc.company_name
                """;

        List<CorporateClient> clients = new ArrayList<>();

        try (
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                clients.add(mapToCorporateClient(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find all corporate clients.", e);
        }

        return clients;
    }

    @Override
    public void update(CorporateClient client) {
        String sql = """
                UPDATE corporate_clients
                SET company_name = ?,
                    cui = ?,
                    legal_representative_id = ?
                WHERE client_id = ?
                """;

        try {
            connection.setAutoCommit(false);

            personHelper.updatePerson(
                    connection,
                    client.getId(),
                    client.getEmail(),
                    client.getPhoneNumber()
            );

            clientHelper.updateClient(
                    connection,
                    client.getId(),
                    client.getClientCode(),
                    client.isActive()
            );

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, client.getCompanyName());
                statement.setString(2, client.getCui());
                statement.setInt(3, client.getLegalRepresentative().getId());
                statement.setInt(4, client.getId());

                statement.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            rollback();
            throw new RuntimeException("Could not update corporate client.", e);
        } finally {
            resetAutoCommit();
        }
    }

    @Override
    public void delete(Integer id) {
        try {
            connection.setAutoCommit(false);

            personHelper.deletePerson(connection, id);

            connection.commit();
        } catch (SQLException e) {
            rollback();
            throw new RuntimeException("Could not delete corporate client.", e);
        } finally {
            resetAutoCommit();
        }
    }

    private CorporateClient mapToCorporateClient(ResultSet resultSet) throws SQLException {
        IndividualClient legalRepresentative = new IndividualClient(
                resultSet.getInt("legal_id"),
                resultSet.getString("legal_email"),
                resultSet.getString("legal_phone_number"),
                resultSet.getString("legal_client_code"),
                resultSet.getBoolean("legal_active"),
                resultSet.getString("legal_last_name"),
                resultSet.getString("legal_first_name"),
                resultSet.getString("legal_cnp")
        );

        return new CorporateClient(
                resultSet.getInt("corporate_id"),
                resultSet.getString("corporate_email"),
                resultSet.getString("corporate_phone_number"),
                resultSet.getString("corporate_client_code"),
                resultSet.getBoolean("corporate_active"),
                resultSet.getString("company_name"),
                resultSet.getString("cui"),
                legalRepresentative
        );
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