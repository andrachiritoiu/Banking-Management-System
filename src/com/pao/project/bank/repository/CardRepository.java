package com.pao.project.bank.repository;

import com.pao.project.bank.model.Card;
import com.pao.project.bank.model.account.Account;
import com.pao.project.bank.model.enums.CardStatus;
import com.pao.project.bank.repository.account.CurrentAccountRepository;
import com.pao.project.bank.repository.account.SavingsAccountRepository;
import com.pao.project.bank.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CardRepository implements Repository<Card, String> {
    private final Connection connection;
    private final CurrentAccountRepository currentAccountRepository;
    private final SavingsAccountRepository savingsAccountRepository;

    public CardRepository() {
        this.connection = DatabaseConnection.getInstance().getConnection();
        this.currentAccountRepository = new CurrentAccountRepository();
        this.savingsAccountRepository = new SavingsAccountRepository();
    }

    @Override
    public void save(Card card) {
        validateCard(card);

        String sql = """
                INSERT INTO cards (
                    card_number,
                    cvv,
                    expiration_date,
                    contactless,
                    status,
                    account_id
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, card.getCardNumber());
            statement.setString(2, card.getCvv());
            statement.setDate(3, Date.valueOf(card.getExpirationDate()));
            statement.setBoolean(4, card.isContactless());
            statement.setString(5, card.getStatus().name());
            statement.setInt(6, card.getAccount().getId());

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not save card.", e);
        }
    }

    @Override
    public Optional<Card> findById(String cardNumber) {
        validateCardNumber(cardNumber);

        String sql = """
                SELECT card_number,
                       cvv,
                       expiration_date,
                       contactless,
                       status,
                       account_id
                FROM cards
                WHERE card_number = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, cardNumber);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapToCard(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find card by number.", e);
        }

        return Optional.empty();
    }

    @Override
    public List<Card> findAll() {
        String sql = """
                SELECT card_number,
                       cvv,
                       expiration_date,
                       contactless,
                       status,
                       account_id
                FROM cards
                ORDER BY expiration_date, card_number
                """;

        List<Card> cards = new ArrayList<>();

        try (
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                cards.add(mapToCard(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find all cards.", e);
        }

        return cards;
    }

    public List<Card> findByAccountId(int accountId) {
        if (accountId <= 0) {
            throw new IllegalArgumentException("Account id must be positive.");
        }

        String sql = """
                SELECT card_number,
                       cvv,
                       expiration_date,
                       contactless,
                       status,
                       account_id
                FROM cards
                WHERE account_id = ?
                ORDER BY expiration_date, card_number
                """;

        List<Card> cards = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, accountId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    cards.add(mapToCard(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find cards by account id.", e);
        }

        return cards;
    }

    @Override
    public void update(Card card) {
        validateCard(card);

        String sql = """
                UPDATE cards
                SET cvv = ?,
                    expiration_date = ?,
                    contactless = ?,
                    status = ?,
                    account_id = ?
                WHERE card_number = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, card.getCvv());
            statement.setDate(2, Date.valueOf(card.getExpirationDate()));
            statement.setBoolean(3, card.isContactless());
            statement.setString(4, card.getStatus().name());
            statement.setInt(5, card.getAccount().getId());
            statement.setString(6, card.getCardNumber());

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not update card.", e);
        }
    }

    @Override
    public void delete(String cardNumber) {
        validateCardNumber(cardNumber);

        String sql = """
                DELETE FROM cards
                WHERE card_number = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, cardNumber);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete card.", e);
        }
    }

    private Card mapToCard(ResultSet resultSet) throws SQLException {
        Account account = loadAccount(resultSet.getInt("account_id"));

        return new Card(
                resultSet.getString("card_number"),
                resultSet.getString("cvv"),
                resultSet.getDate("expiration_date").toLocalDate(),
                resultSet.getBoolean("contactless"),
                CardStatus.valueOf(resultSet.getString("status")),
                account
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

    private void validateCard(Card card) {
        if (card == null) {
            throw new IllegalArgumentException("Card cannot be null.");
        }

        validateCardNumber(card.getCardNumber());

        if (card.getCvv() == null || !card.getCvv().matches("\\d{3}")) {
            throw new IllegalArgumentException("CVV must contain exactly 3 digits.");
        }

        if (card.getExpirationDate() == null) {
            throw new IllegalArgumentException("Expiration date cannot be null.");
        }

        if (card.getStatus() == null) {
            throw new IllegalArgumentException("Card status cannot be null.");
        }

        if (card.getAccount() == null) {
            throw new IllegalArgumentException("Card account cannot be null.");
        }
    }

    private void validateCardNumber(String cardNumber) {
        if (cardNumber == null || !cardNumber.matches("\\d{16}")) {
            throw new IllegalArgumentException("Card number must contain exactly 16 digits.");
        }
    }
}
