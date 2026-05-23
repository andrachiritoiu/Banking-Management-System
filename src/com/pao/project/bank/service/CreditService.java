package com.pao.project.bank.service;

import com.pao.project.bank.exception.InvalidOperationException;
import com.pao.project.bank.model.account.Account;
import com.pao.project.bank.model.Credit;
import com.pao.project.bank.model.enums.CreditStatus;
import com.pao.project.bank.model.enums.CreditType;
import com.pao.project.bank.model.person.Client;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreditService {
    private static final CreditService INSTANCE = new CreditService();

    private final AccountService accountService = AccountService.getInstance();

    private int creditIdCounter = 1;
    private final List<Credit> credits = new ArrayList<>();
    private final Map<Integer, Credit> creditsById = new HashMap<>();

    private CreditService() {}

    public static CreditService getInstance() {
        return INSTANCE;
    }

    public Credit createCredit(Client borrower, String targetIban, CreditType type, double principalAmount, double annualInterestRate, int durationInMonths) {
        if (borrower == null) {
            throw new InvalidOperationException("Borrower cannot be null.");
        }

        Account targetAccount = accountService.findByIban(targetIban);

        if (targetAccount == null) {
            throw new InvalidOperationException("Target account not found.");
        }

        if (!targetAccount.getOwner().equals(borrower)) {
            throw new InvalidOperationException("The target account does not belong to this client.");
        }

        Credit credit = new Credit(
                creditIdCounter++,
                borrower,
                targetAccount,
                type,
                principalAmount,
                annualInterestRate,
                durationInMonths,
                LocalDate.now()
        );

        credits.add(credit);
        creditsById.put(credit.getId(), credit);

        return credit;
    }

    public void approveCredit(int creditId) {
        Credit credit = findById(creditId);

        credit.approve();

        accountService.deposit(
                credit.getTargetAccount().getIban().getCode(),
                credit.getPrincipalAmount()
        );
    }

    public void rejectCredit(int creditId) {
        Credit credit = findById(creditId);
        credit.reject();
    }

    public void payInstallment(int creditId, double amount) {
        Credit credit = findById(creditId);

        if (amount <= 0) {
            throw new InvalidOperationException("Installment amount must be positive.");
        }

        accountService.withdraw(
                credit.getTargetAccount().getIban().getCode(),
                amount
        );

        credit.payInstallment(amount);
    }

    public Credit findById(int creditId) {
        Credit credit = creditsById.get(creditId);

        if (credit == null) {
            throw new InvalidOperationException("Credit not found.");
        }

        return credit;
    }

    public List<Credit> getAllCredits() {
        return new ArrayList<>(credits);
    }

    public List<Credit> getCreditsForClient(Client client) {
        List<Credit> result = new ArrayList<>();

        if (client == null) {
            return result;
        }

        for (Credit credit : credits) {
            if (credit.getBorrower().equals(client)) {
                result.add(credit);
            }
        }

        return result;
    }

    public List<Credit> getCreditsByStatus(CreditStatus status) {
        List<Credit> result = new ArrayList<>();

        for (Credit credit : credits) {
            if (credit.getStatus() == status) {
                result.add(credit);
            }
        }

        return result;
    }
}