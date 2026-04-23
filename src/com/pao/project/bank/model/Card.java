package com.pao.project.bank.model;

import com.pao.project.bank.model.account.Account;
import com.pao.project.bank.model.enums.CardStatus;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Random;

public class Card {
    private final String cardNumber;
    private final String cvv;
    private final LocalDate expirationDate;
    private final boolean contactless;

    private CardStatus status;
    private final Account account;

    public Card(Account account, LocalDate expirationDate, boolean contactless) {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null.");
        }
        if (expirationDate == null) {
            throw new IllegalArgumentException("Expiration date cannot be null.");
        }
        if (!expirationDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Expiration date must be in the future.");
        }

        this.account = account;
        this.expirationDate = expirationDate;
        this.contactless = contactless;
        this.cardNumber = generateCardNumber();
        this.cvv = generateCvv();
        this.status = CardStatus.ACTIVE;
    }

    private String generateCardNumber() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 16; i++) {
            sb.append(random.nextInt(10));
        }

        return sb.toString();
    }

    private String generateCvv() {
        Random random = new Random();
        int number = 100 + random.nextInt(900);
        return String.valueOf(number);
    }



    public String getCardNumber() {
        return cardNumber;
    }

    public String getCvv() {
        return cvv;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public boolean isContactless() {
        return contactless;
    }

    public CardStatus getStatus() {
        if (status != CardStatus.EXPIRED && isExpired()) {
            status = CardStatus.EXPIRED;
        }
        return status;
    }

    public void setStatus(CardStatus status) {
        this.status = status;
    }

    public Account getAccount() {
        return account;
    }


    public void block() {
        if (status == CardStatus.EXPIRED) {
            throw new IllegalStateException("Expired card cannot be blocked.");
        }
        if (status == CardStatus.BLOCKED) {
            throw new IllegalStateException("Card is already blocked.");
        }
        status = CardStatus.BLOCKED;
    }

    public void unblock() {
        if (status == CardStatus.EXPIRED) {
            throw new IllegalStateException("Expired card cannot be unblocked.");
        }
        if (status == CardStatus.ACTIVE) {
            throw new IllegalStateException("Card is already active.");
        }
        status = CardStatus.ACTIVE;
    }

    public boolean isExpired() {
        return LocalDate.now().isAfter(expirationDate);
    }

    public void expire() {
        status = CardStatus.EXPIRED;
    }

    public boolean isUsable() {
        return status == CardStatus.ACTIVE && !isExpired();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Card card)) return false;
        return Objects.equals(cardNumber, card.cardNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(cardNumber);
    }

    @Override
    public String toString() {
        return "Card{" +
                "cardNumber='" + cardNumber + '\'' +
                ", expirationDate=" + expirationDate +
                ", contactless=" + contactless +
                ", status=" + status +
                ", account=" + account +
                '}';
    }
}
