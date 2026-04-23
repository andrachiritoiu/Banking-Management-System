package com.pao.project.bank.model.account;

public interface Transactable {
    void deposit(double amount);
    void withdraw(double amount);
}
