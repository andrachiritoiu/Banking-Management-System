package com.pao.project.bank.repository;

import com.pao.project.bank.model.enums.TransactionType;
import com.pao.project.bank.model.transaction.Deposit;
import com.pao.project.bank.model.transaction.Exchange;
import com.pao.project.bank.model.transaction.Transaction;
import com.pao.project.bank.model.transaction.Transfer;
import com.pao.project.bank.model.transaction.Withdrawal;
import com.pao.project.bank.repository.helper.TransactionRepositoryHelper;
import com.pao.project.bank.repository.transaction.DepositRepository;
import com.pao.project.bank.repository.transaction.ExchangeRepository;
import com.pao.project.bank.repository.transaction.TransferRepository;
import com.pao.project.bank.repository.transaction.WithdrawalRepository;
import com.pao.project.bank.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class TransactionRepository implements Repository<Transaction, Integer> {
    private final Connection connection;
    private final TransactionRepositoryHelper transactionHelper;
    private final DepositRepository depositRepository;
    private final WithdrawalRepository withdrawalRepository;
    private final TransferRepository transferRepository;
    private final ExchangeRepository exchangeRepository;

    public TransactionRepository() {
        this.connection = DatabaseConnection.getInstance().getConnection();
        this.transactionHelper = new TransactionRepositoryHelper();
        this.depositRepository = new DepositRepository();
        this.withdrawalRepository = new WithdrawalRepository();
        this.transferRepository = new TransferRepository();
        this.exchangeRepository = new ExchangeRepository();
    }

    @Override
    public void save(Transaction transaction) {
        if (transaction instanceof Deposit deposit) {
            depositRepository.save(deposit);
        } else if (transaction instanceof Withdrawal withdrawal) {
            withdrawalRepository.save(withdrawal);
        } else if (transaction instanceof Transfer transfer) {
            transferRepository.save(transfer);
        } else if (transaction instanceof Exchange exchange) {
            exchangeRepository.save(exchange);
        } else {
            throw new IllegalArgumentException("Unknown transaction type.");
        }
    }

    @Override
    public Optional<Transaction> findById(Integer id) {
        try {
            Optional<String> transactionType = transactionHelper.findTransactionTypeById(connection, id);

            if (transactionType.isEmpty()) {
                return Optional.empty();
            }

            return switch (TransactionType.valueOf(transactionType.get())) {
                case DEPOSIT -> depositRepository.findById(id).map(transaction -> transaction);
                case WITHDRAWAL -> withdrawalRepository.findById(id).map(transaction -> transaction);
                case TRANSFER -> transferRepository.findById(id).map(transaction -> transaction);
                case EXCHANGE -> exchangeRepository.findById(id).map(transaction -> transaction);
            };
        } catch (SQLException e) {
            throw new RuntimeException("Could not find transaction by id.", e);
        }
    }

    @Override
    public List<Transaction> findAll() {
        List<Transaction> transactions = new ArrayList<>();

        transactions.addAll(depositRepository.findAll());
        transactions.addAll(withdrawalRepository.findAll());
        transactions.addAll(transferRepository.findAll());
        transactions.addAll(exchangeRepository.findAll());

        transactions.sort(Comparator.comparing(Transaction::getTimestamp).reversed());
        return transactions;
    }

    @Override
    public void update(Transaction transaction) {
        if (transaction instanceof Deposit deposit) {
            depositRepository.update(deposit);
        } else if (transaction instanceof Withdrawal withdrawal) {
            withdrawalRepository.update(withdrawal);
        } else if (transaction instanceof Transfer transfer) {
            transferRepository.update(transfer);
        } else if (transaction instanceof Exchange exchange) {
            exchangeRepository.update(exchange);
        } else {
            throw new IllegalArgumentException("Unknown transaction type.");
        }
    }

    @Override
    public void delete(Integer id) {
        try {
            transactionHelper.deleteTransaction(connection, id);
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete transaction.", e);
        }
    }
}
