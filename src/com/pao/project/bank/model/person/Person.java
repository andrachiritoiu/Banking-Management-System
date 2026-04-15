package com.pao.project.bank.model.person;

import com.pao.project.bank.exception.InvalidOperationException;

public abstract class Person {
    protected int id;
    protected String email;
    protected String phoneNumber;


    public Person(int id, String email, String phoneNumber) {
        if (id <= 0) {
            throw new InvalidOperationException("Id must be positive.");
        }
        if (email == null || email.isBlank()) {
            throw new InvalidOperationException("Email cannot be null or blank.");
        }
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new InvalidOperationException("Phone number cannot be null or blank.");
        }

        this.id = id;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }


    public int getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }


    public abstract String getRole();
    public abstract String getFullName();

    @Override
    public String toString() {
        return "Person{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                '}';
    }
}
