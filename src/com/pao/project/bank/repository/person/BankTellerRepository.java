package com.pao.project.bank.repository.person;

import com.pao.project.bank.util.DatabaseConnection;
import com.pao.project.bank.model.person.BankTeller;
import com.pao.project.bank.repository.Repository;
import com.pao.project.bank.repository.helper.EmployeeRepositoryHelper;
import com.pao.project.bank.repository.helper.PersonRepositoryHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BankTellerRepository implements Repository<BankTeller, Integer> {
    private final Connection connection;
    private final PersonRepositoryHelper personHelper;
    private final EmployeeRepositoryHelper employeeHelper;

    public BankTellerRepository() {
        this.connection = DatabaseConnection.getInstance().getConnection();
        this.personHelper = new PersonRepositoryHelper();
        this.employeeHelper = new EmployeeRepositoryHelper();
    }

    @Override
    public void save(BankTeller teller) {
        String sql = """
                INSERT INTO bank_tellers (
                    employee_id,
                    desk_number
                )
                VALUES (?, ?)
                """;

        try {
            connection.setAutoCommit(false);

            personHelper.insertPerson(
                    connection,
                    teller.getId(),
                    PersonRepositoryHelper.PERSON_TYPE_EMPLOYEE,
                    teller.getEmail(),
                    teller.getPhoneNumber()
            );

            employeeHelper.insertEmployee(
                    connection,
                    teller.getId(),
                    teller.getEmployeeCode(),
                    EmployeeRepositoryHelper.EMPLOYEE_TYPE_BANK_TELLER,
                    teller.getFirstName(),
                    teller.getLastName(),
                    teller.getSalary(),
                    teller.getBranch()
            );

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, teller.getId());
                statement.setInt(2, teller.getDeskNumber());

                statement.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            rollback();
            throw new RuntimeException("Could not save bank teller.", e);
        } finally {
            resetAutoCommit();
        }
    }

    @Override
    public Optional<BankTeller> findById(Integer id) {
        String sql = """
                SELECT
                    p.id,
                    p.email,
                    p.phone_number,
                    e.employee_code,
                    e.first_name,
                    e.last_name,
                    e.salary,
                    e.branch,
                    bt.desk_number
                FROM persons p
                JOIN employees e ON p.id = e.id
                JOIN bank_tellers bt ON e.id = bt.employee_id
                WHERE p.id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapToBankTeller(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find bank teller by id.", e);
        }

        return Optional.empty();
    }

    @Override
    public List<BankTeller> findAll() {
        String sql = """
                SELECT
                    p.id,
                    p.email,
                    p.phone_number,
                    e.employee_code,
                    e.first_name,
                    e.last_name,
                    e.salary,
                    e.branch,
                    bt.desk_number
                FROM persons p
                JOIN employees e ON p.id = e.id
                JOIN bank_tellers bt ON e.id = bt.employee_id
                ORDER BY e.last_name, e.first_name
                """;

        List<BankTeller> tellers = new ArrayList<>();

        try (
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                tellers.add(mapToBankTeller(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find all bank tellers.", e);
        }

        return tellers;
    }

    @Override
    public void update(BankTeller teller) {
        String sql = """
                UPDATE bank_tellers
                SET desk_number = ?
                WHERE employee_id = ?
                """;

        try {
            connection.setAutoCommit(false);

            personHelper.updatePerson(
                    connection,
                    teller.getId(),
                    teller.getEmail(),
                    teller.getPhoneNumber()
            );

            employeeHelper.updateEmployee(
                    connection,
                    teller.getId(),
                    teller.getEmployeeCode(),
                    teller.getFirstName(),
                    teller.getLastName(),
                    teller.getSalary(),
                    teller.getBranch()
            );

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, teller.getDeskNumber());
                statement.setInt(2, teller.getId());

                statement.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            rollback();
            throw new RuntimeException("Could not update bank teller.", e);
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
            throw new RuntimeException("Could not delete bank teller.", e);
        } finally {
            resetAutoCommit();
        }
    }

    private BankTeller mapToBankTeller(ResultSet resultSet) throws SQLException {
        return new BankTeller(
                resultSet.getInt("id"),
                resultSet.getString("email"),
                resultSet.getString("phone_number"),
                resultSet.getString("last_name"),
                resultSet.getString("first_name"),
                resultSet.getString("employee_code"),
                resultSet.getDouble("salary"),
                resultSet.getString("branch"),
                resultSet.getInt("desk_number")
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