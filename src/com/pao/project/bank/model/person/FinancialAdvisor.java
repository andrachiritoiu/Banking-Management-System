package com.pao.project.bank.model.person;

public class FinancialAdvisor extends BankEmployee{
    private String specialization;

    public FinancialAdvisor(int id, String email, String phoneNumber, String lastName, String firstName, String employeeCode, double salary, String branch, String specialization) {
        super(id, email, phoneNumber, lastName, firstName, employeeCode, salary, branch);
        this.specialization = specialization;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }


    @Override
    public String getPosition() {
        return "Financial Advisor";
    }

    @Override
    public String toString() {
        return "FinancialAdvisor{" +
                "id=" + id +
                ", specialization='" + specialization + '\'' +
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
