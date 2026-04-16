package com.pao.project.bank.service;

import com.pao.project.bank.model.Cheque;
import com.pao.project.bank.model.account.Account;
import com.pao.project.bank.model.enums.ChequeStatus;

import java.util.ArrayList;
import java.util.List;

public class ChequeService {
    private static final ChequeService INSTANCE = new ChequeService();

    private final List<Cheque> cheques = new ArrayList<>();
    private final AccountService accountService = AccountService.getInstance();

    private ChequeService() {
    }

    public static ChequeService getInstance() {
        return INSTANCE;
    }

    public void issueCheque(Cheque cheque) {
        if (cheque == null) {
            throw new IllegalArgumentException("Cheque cannot be null.");
        }

        if (findBySeries(cheque.getSeries()) != null) {
            throw new IllegalArgumentException("Cheque series already exists.");
        }

        cheques.add(cheque);
    }

    public Cheque findBySeries(String series) {
        if (series == null) {
            return null;
        }

        for (Cheque cheque : cheques) {
            if (series.equals(cheque.getSeries())) {
                return cheque;
            }
        }

        return null;
    }

    public List<Cheque> getAllCheques() {
        return new ArrayList<>(cheques);
    }

    public List<Cheque> getChequesByStatus(ChequeStatus status) {
        List<Cheque> result = new ArrayList<>();

        if (status == null) {
            return result;
        }

        for (Cheque cheque : cheques) {
            if (cheque.getStatus() == status) {
                result.add(cheque);
            }
        }

        return result;
    }

    public void cancelCheque(String series) {
        Cheque cheque = findBySeries(series);

        if (cheque == null) {
            throw new IllegalArgumentException("Cheque not found.");
        }

        cheque.cancel();
    }

    public void cashCheque(String series, Account beneficiaryAccount) {
        if (beneficiaryAccount == null) {
            throw new IllegalArgumentException("Beneficiary account cannot be null.");
        }

        Cheque cheque = findBySeries(series);

        if (cheque == null) {
            throw new IllegalArgumentException("Cheque not found.");
        }

        if (!beneficiaryAccount.getOwner().equals(cheque.getBeneficiary())) {
            throw new IllegalArgumentException("Beneficiary account does not belong to the cheque beneficiary.");
        }

        if (cheque.getIssuerAccount().equals(beneficiaryAccount)) {
            throw new IllegalArgumentException("Issuer account and beneficiary account must be different.");
        }

        accountService.transfer(
                cheque.getIssuerAccount().getIban().getCode(),
                beneficiaryAccount.getIban().getCode(),
                cheque.getAmount()
        );

        cheque.cash();
    }

}
