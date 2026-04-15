package com.pao.project.bank.model.person;

public class BankTeller extends BankEmployee{
    private int deskNumber;

    public BankTeller(int id, String email, String phoneNumber, String lastName, String firstName, String employeeCode, double salary, String branch, int deskNumber) {
        super(id, email, phoneNumber, lastName, firstName, employeeCode, salary, branch);
        this.deskNumber = deskNumber;
    }

    public int getDeskNumber() {
        return deskNumber;
    }

    public void setDeskNumber(int deskNumber) {
        this.deskNumber = deskNumber;
    }

    @Override
    public String getPosition() {
        return "Bamk Teller";
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
