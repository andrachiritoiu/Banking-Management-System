package com.pao.project.bank.model.person;

import com.pao.project.bank.exception.InvalidOperationException;

import java.util.Objects;

public abstract class BankEmployee extends Person{
    protected String lastName;
    protected String firstName;
    protected String employeeCode;
    protected double salary;
    protected String branch;

    public BankEmployee(int id, String email, String phoneNumber, String lastName, String firstName, String employeeCode, double salary, String branch) {
        super(id, email, phoneNumber);

        if (lastName == null || lastName.isBlank()) {
            throw new InvalidOperationException("Last name cannot be null or blank.");
        }
        if (firstName == null || firstName.isBlank()) {
            throw new InvalidOperationException("First name cannot be null or blank.");
        }
        if (employeeCode == null || employeeCode.isBlank()) {
            throw new InvalidOperationException("Employee code cannot be null or blank.");
        }
        if (salary < 0) {
            throw new InvalidOperationException("Salary cannot be negative.");
        }
        if (branch == null || branch.isBlank()) {
            throw new InvalidOperationException("Branch cannot be null or blank.");
        }

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
        if (lastName == null || lastName.isBlank()) {
            throw new InvalidOperationException("Last name cannot be null or blank.");
        }
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        if (firstName == null || firstName.isBlank()) {
            throw new InvalidOperationException("First name cannot be null or blank.");
        }
        this.firstName = firstName;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        if (employeeCode == null || employeeCode.isBlank()) {
            throw new InvalidOperationException("Employee code cannot be null or blank.");
        }
        this.employeeCode = employeeCode;
    }

    public double getSalary() {
        return salary;
    }

    public void setSalary(double salary) {
        if (salary < 0) {
            throw new InvalidOperationException("Salary cannot be negative.");
        }
        this.salary = salary;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        if (branch == null || branch.isBlank()) {
            throw new InvalidOperationException("Branch cannot be null or blank.");
        }
        this.branch = branch;
    }


    public abstract String getPosition();

    @Override
    public String getRole() {
        return "Bank Employee";
    }

    @Override
    public String getFullName() {
        return (lastName == null ? "" : lastName) + " " +
                (firstName == null ? "" : firstName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
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
