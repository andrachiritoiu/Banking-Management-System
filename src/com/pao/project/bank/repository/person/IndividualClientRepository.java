package com.pao.project.bank.repository.person;

import com.pao.project.bank.util.DatabaseConnection;
import com.pao.project.bank.model.person.IndividualClient;
import com.pao.project.bank.repository.Repository;
import com.pao.project.bank.repository.helper.ClientRepositoryHelper;
import com.pao.project.bank.repository.helper.PersonRepositoryHelper;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class IndividualClientRepository implements Repository<IndividualClient, Integer> {
    private final Connection connection;
    private final PersonRepositoryHelper personHelper;
    private final ClientRepositoryHelper clientHelper;

    public IndividualClientRepository() {
        this.connection = DatabaseConnection.getInstance().getConnection();
        this.personHelper = new PersonRepositoryHelper();
        this.clientHelper = new ClientRepositoryHelper();
    }

    @Override
    public void save(IndividualClient client) {
        String sql = """
                INSERT INTO individual_clients (
                    client_id,
                    first_name,
                    last_name,
                    cnp,
                    birth_date
                )
                VALUES (?, ?, ?, ?, ?)
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
                    ClientRepositoryHelper.CLIENT_TYPE_INDIVIDUAL,
                    client.isActive()
            );

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, client.getId());
                statement.setString(2, client.getFirstName());
                statement.setString(3, client.getLastName());
                statement.setString(4, client.getCnp());
                statement.setDate(5, Date.valueOf(client.getBirthDate()));

                statement.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            rollback();
            throw new RuntimeException("Could not save individual client.", e);
        } finally {
            resetAutoCommit();
        }
    }

    @Override
    public Optional<IndividualClient> findById(Integer id) {
        String sql = """
                SELECT
                    p.id,
                    p.email,
                    p.phone_number,
                    c.client_code,
                    c.active,
                    ic.first_name,
                    ic.last_name,
                    ic.cnp
                FROM persons p
                JOIN clients c ON p.id = c.id
                JOIN individual_clients ic ON c.id = ic.client_id
                WHERE p.id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapToIndividualClient(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find individual client by id.", e);
        }

        return Optional.empty();
    }

    @Override
    public List<IndividualClient> findAll() {
        String sql = """
                SELECT
                    p.id,
                    p.email,
                    p.phone_number,
                    c.client_code,
                    c.active,
                    ic.first_name,
                    ic.last_name,
                    ic.cnp
                FROM persons p
                JOIN clients c ON p.id = c.id
                JOIN individual_clients ic ON c.id = ic.client_id
                ORDER BY ic.last_name, ic.first_name
                """;

        List<IndividualClient> clients = new ArrayList<>();

        try (
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                clients.add(mapToIndividualClient(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find all individual clients.", e);
        }

        return clients;
    }

    @Override
    public void update(IndividualClient client) {
        String sql = """
                UPDATE individual_clients
                SET first_name = ?,
                    last_name = ?,
                    cnp = ?,
                    birth_date = ?
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
                statement.setString(1, client.getFirstName());
                statement.setString(2, client.getLastName());
                statement.setString(3, client.getCnp());
                statement.setDate(4, Date.valueOf(client.getBirthDate()));
                statement.setInt(5, client.getId());

                statement.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            rollback();
            throw new RuntimeException("Could not update individual client.", e);
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
            throw new RuntimeException("Could not delete individual client.", e);
        } finally {
            resetAutoCommit();
        }
    }

    private IndividualClient mapToIndividualClient(ResultSet resultSet) throws SQLException {
        return new IndividualClient(
                resultSet.getInt("id"),
                resultSet.getString("email"),
                resultSet.getString("phone_number"),
                resultSet.getString("client_code"),
                resultSet.getBoolean("active"),
                resultSet.getString("last_name"),
                resultSet.getString("first_name"),
                resultSet.getString("cnp")
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