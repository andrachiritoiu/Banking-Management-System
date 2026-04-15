package com.pao.project.bank.model.person;

import java.util.Objects;

abstract public class BankEmployee extends Person{
    protected String lastName;
    protected String firstName;
    protected String employeeCode;
    protected double salary;
    protected String branch;

    public BankEmployee(int id, String email, String phoneNumber, String lastName, String firstName, String employeeCode, double salary, String branch) {
        super(id, email, phoneNumber);
        this.lastName = lastName;
        this.firstName = firstName;
        this.employeeCode = employeeCode;
        this.salary = salary;
        this.branch = branch;
    }


    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public double getSalary() {
        return salary;
    }

    public void setSalary(double salary) {
        this.salary = salary;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }


    abstract public String getPosition();

    @Override
    public String getRole() {
        return "Bank Employee";
    }

    @Override
    public String getFullName() {
        return this.lastName + " " + this.firstName;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BankEmployee that)) return false;
        return Objects.equals(employeeCode, that.employeeCode);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(employeeCode);
    }

    @Override
    public String toString() {
        return "BankEmployee{" +
                "id=" + id +
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
