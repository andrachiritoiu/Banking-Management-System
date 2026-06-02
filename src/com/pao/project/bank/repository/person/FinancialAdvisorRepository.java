package com.pao.project.bank.repository.person;

import com.pao.project.bank.util.DatabaseConnection;
import com.pao.project.bank.model.person.FinancialAdvisor;
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

public class FinancialAdvisorRepository implements Repository<FinancialAdvisor, Integer> {
    private final Connection connection;
    private final PersonRepositoryHelper personHelper;
    private final EmployeeRepositoryHelper employeeHelper;

    public FinancialAdvisorRepository() {
        this.connection = DatabaseConnection.getInstance().getConnection();
        this.personHelper = new PersonRepositoryHelper();
        this.employeeHelper = new EmployeeRepositoryHelper();
    }

    @Override
    public void save(FinancialAdvisor advisor) {
        String sql = """
                INSERT INTO financial_advisors (
                    employee_id,
                    specialization
                )
                VALUES (?, ?)
                """;

        try {
            connection.setAutoCommit(false);

            personHelper.insertPerson(
                    connection,
                    advisor.getId(),
                    PersonRepositoryHelper.PERSON_TYPE_EMPLOYEE,
                    advisor.getEmail(),
                    advisor.getPhoneNumber()
            );

            employeeHelper.insertEmployee(
                    connection,
                    advisor.getId(),
                    advisor.getEmployeeCode(),
                    EmployeeRepositoryHelper.EMPLOYEE_TYPE_FINANCIAL_ADVISOR,
                    advisor.getFirstName(),
                    advisor.getLastName(),
                    advisor.getSalary(),
                    advisor.getBranch()
            );

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, advisor.getId());
                statement.setString(2, advisor.getSpecialization());

                statement.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            rollback();
            throw new RuntimeException("Could not save financial advisor.", e);
        } finally {
            resetAutoCommit();
        }
    }

    @Override
    public Optional<FinancialAdvisor> findById(Integer id) {
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
                    fa.specialization
                FROM persons p
                JOIN employees e ON p.id = e.id
                JOIN financial_advisors fa ON e.id = fa.employee_id
                WHERE p.id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapToFinancialAdvisor(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find financial advisor by id.", e);
        }

        return Optional.empty();
    }

    @Override
    public List<FinancialAdvisor> findAll() {
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
                    fa.specialization
                FROM persons p
                JOIN employees e ON p.id = e.id
                JOIN financial_advisors fa ON e.id = fa.employee_id
                ORDER BY e.last_name, e.first_name
                """;

        List<FinancialAdvisor> advisors = new ArrayList<>();

        try (
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                advisors.add(mapToFinancialAdvisor(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find all financial advisors.", e);
        }

        return advisors;
    }

    @Override
    public void update(FinancialAdvisor advisor) {
        String sql = """
                UPDATE financial_advisors
                SET specialization = ?
                WHERE employee_id = ?
                """;

        try {
            connection.setAutoCommit(false);

            personHelper.updatePerson(
                    connection,
                    advisor.getId(),
                    advisor.getEmail(),
                    advisor.getPhoneNumber()
            );

            employeeHelper.updateEmployee(
                    connection,
                    advisor.getId(),
                    advisor.getEmployeeCode(),
                    advisor.getFirstName(),
                    advisor.getLastName(),
                    advisor.getSalary(),
                    advisor.getBranch()
            );

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, advisor.getSpecialization());
                statement.setInt(2, advisor.getId());

                statement.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            rollback();
            throw new RuntimeException("Could not update financial advisor.", e);
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
            throw new RuntimeException("Could not delete financial advisor.", e);
        } finally {
            resetAutoCommit();
        }
    }

    private FinancialAdvisor mapToFinancialAdvisor(ResultSet resultSet) throws SQLException {
        return new FinancialAdvisor(
                resultSet.getInt("id"),
                resultSet.getString("email"),
                resultSet.getString("phone_number"),
                resultSet.getString("last_name"),
                resultSet.getString("first_name"),
                resultSet.getString("employee_code"),
                resultSet.getDouble("salary"),
                resultSet.getString("branch"),
                resultSet.getString("specialization")
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