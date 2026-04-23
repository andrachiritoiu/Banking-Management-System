package com.pao.project.bank.service;

import com.pao.project.bank.model.Card;
import com.pao.project.bank.model.account.Account;
import com.pao.project.bank.model.enums.CardStatus;
import com.pao.project.bank.model.transaction.Transaction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CardService {
    private static final CardService INSTANCE = new CardService();

    private final List<Card> cards = new ArrayList<>();
    private final Map<String, Card> cardsByNumber = new HashMap<>();

    private CardService() {}

    public static CardService getInstance() {
        return INSTANCE;
    }


    public void issueCard(Card card){
        if (card == null) {
            throw new IllegalArgumentException("Card cannot be null.");
        }

        String cardNumber = card.getCardNumber();

        if (cardsByNumber.containsKey(cardNumber)) {
            throw new IllegalArgumentException("Card already exists.");
        }

        cards.add(card);
        cardsByNumber.put(cardNumber, card);
    }

    public Card issueCard(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null.");
        }

        Card card = new Card(account, LocalDate.now().plusYears(4), true);
        issueCard(card);
        return card;
    }

    public Card issueCard(Account account, LocalDate expirationDate, boolean contactless) {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null.");
        }

        Card card = new Card(account, expirationDate, contactless);
        issueCard(card);
        return card;
    }


    public Card findByCardNumber(String cardNumber) {
        if (cardNumber == null) {
            return null;
        }

        return cardsByNumber.get(cardNumber);
    }


    public void blockCard(String cardNumber){
        Card card = cardsByNumber.get(cardNumber);

        if (card == null) {
            throw new IllegalArgumentException("Card not found.");
        }

        if(card.getStatus() == CardStatus.BLOCKED){
            throw new IllegalArgumentException("Card is already blocked.");
        }

        card.block();
    }

    public void unblockCard(String cardNumber){
        Card card = cardsByNumber.get(cardNumber);

        if (card == null) {
            throw new IllegalArgumentException("Card not found.");
        }

        if(card.getStatus() == CardStatus.ACTIVE){
            throw new IllegalArgumentException("Card is already active.");
        }

        if(card.getStatus() == CardStatus.EXPIRED){
            throw new IllegalArgumentException("Card is expired.");
        }

        card.unblock();
    }

    public List<Card> getAllCards(){
        return new ArrayList<>(cards);
    }

    public List<Card> getCardsForAccount(Account account) {
        List<Card> result = new ArrayList<>();

        if (account == null) {
            return result;
        }

        for (Card card : cards) {
            if (card.getAccount().equals(account)) {
                result.add(card);
            }
        }

        return result;
    }

    public void validateCard(String cardNumber) {
        Card card = findByCardNumber(cardNumber);

        if (card == null) {
            throw new IllegalArgumentException("Card not found.");
        }

        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new RuntimeException("Card is blocked.");
        }

        if (card.getStatus() == CardStatus.EXPIRED) {
            throw new RuntimeException("Card is expired.");
        }
    }
}
