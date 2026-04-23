package com.pao.project.bank.model.person;

import com.pao.project.bank.exception.InvalidOperationException;

import java.util.Objects;

public abstract class Client extends Person implements Comparable<Client>{
    protected String clientCode;
    protected boolean active;

    public Client(int id, String email, String phoneNumber, String clientCode, boolean active) {
        super(id, email, phoneNumber);

        if (clientCode == null || clientCode.isBlank()) {
            throw new InvalidOperationException("Client code cannot be null or blank.");
        }

        this.clientCode = clientCode;
        this.active = active;
    }

    public String getClientCode() {
        return clientCode;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }


    public abstract String getClientType();
    //cnp-IndividualClient / cui-CorporateClient
    public abstract String getFiscalIdentifier();


    @Override
    public String getRole() {
        return "Client";
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Client client)) return false;
        return Objects.equals(clientCode, client.clientCode);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(clientCode);
    }

    @Override
    public String toString() {
        return "Client{" +
                "clientCode='" + clientCode + '\'' +
                ", active=" + active +
                ", id=" + id +
                ", email='" + email + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                '}';
    }

    @Override
    public int compareTo(Client other) {
        if (other == null) {
            throw new NullPointerException("Cannot compare Client with null.");
        }

        String name1 = this.getFullName();
        String name2 = other.getFullName();

        //null protection
        if (name1 == null) name1 = "";
        if (name2 == null) name2 = "";

        int cmp = name1.compareToIgnoreCase(name2);
        if (cmp != 0) {
            return cmp;
        }

        //if other==this, compare clientCode
        return this.clientCode.compareToIgnoreCase(other.clientCode);
    }
}
