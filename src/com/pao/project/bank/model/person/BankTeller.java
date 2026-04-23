package com.pao.project.bank.model.person;

import com.pao.project.bank.exception.InvalidOperationException;

public class BankTeller extends BankEmployee{
    private int deskNumber;

    public BankTeller(int id, String email, String phoneNumber, String lastName, String firstName, String employeeCode, double salary, String branch, int deskNumber) {
        super(id, email, phoneNumber, lastName, firstName, employeeCode, salary, branch);

        if (deskNumber <= 0) {
            throw new InvalidOperationException("Desk number must be positive.");
        }

        this.deskNumber = deskNumber;
    }

    public int getDeskNumber() {
        return deskNumber;
    }

    public void setDeskNumber(int deskNumber) {
        if (deskNumber <= 0) {
            throw new InvalidOperationException("Desk number must be positive.");
        }

        this.deskNumber = deskNumber;
    }

    @Override
    public String getPosition() {
        return "Bank Teller";
    }

    @Override
    public String toString() {
        return "BankTeller{" +
                "id=" + id +
                ", deskNumber=" + deskNumber +
                ", lastName='" + lastName + '\'' +
                ", firstName='" + firstName + '\'' +
                ", employeeCode='" + employeeCode + '\'' +
                ", salary=" + salary +
                ", branch='" + branch + '\'' +
                ", email='" + email + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                '}';
    }
}
