package com.pao.project.bank.model.account;

import com.pao.project.bank.model.IBAN;
import com.pao.project.bank.model.enums.AccountType;
import com.pao.project.bank.model.enums.Currency;
import com.pao.project.bank.model.person.Client;

public final class AccountFactory {
    private static final double DEFAULT_CURRENT_ACCOUNT_MONTHLY_FEE = 5.0;
    private static final double DEFAULT_SAVINGS_ACCOUNT_INTEREST_RATE = 3.5;

    private AccountFactory() {}

    public static Account createAccount(
            AccountType type,
            int id,
            IBAN iban,
            Client owner,
            double initialBalance,
            Currency currency
    ) {
        if (type == null) {
            throw new IllegalArgumentException("Account type cannot be null.");
        }

        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null.");
        }

        return switch (type) {
            case CURRENT -> new CurrentAccount(
                    id,
                    iban,
                    initialBalance,
                    currency.name(),
                    owner,
                    DEFAULT_CURRENT_ACCOUNT_MONTHLY_FEE
            );
            case SAVINGS -> new SavingsAccount(
                    id,
                    iban,
                    initialBalance,
                    currency.name(),
                    owner,
                    DEFAULT_SAVINGS_ACCOUNT_INTEREST_RATE,
                    0
            );
        };
    }
}
