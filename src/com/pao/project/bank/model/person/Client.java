package com.pao.project.bank.model.person;

import java.util.Objects;

abstract public class Client extends Person implements Comparable<Client>{
    protected String clientCode;
    protected boolean active;

    public Client(int id, String email, String phoneNumber, String clientCode, boolean active) {
        super(id, email, phoneNumber);
        this.clientCode = clientCode;
        this.active = active;
    }

    public String getClientCode() {
        return clientCode;
    }

    public void setClientCode(String clientCode) {
        this.clientCode = clientCode;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }


    abstract public String getClientType();
    //cnp-IndividualClient / cui-CorporateClient
    abstract public String getFiscalIdentifier();


    @Override
    public String getRole() {
        return "Client";
    }


    @Override
    public boolean equals(Object o) {
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
        int cmp = this.getFullName().compareToIgnoreCase(other.getFullName());
        if(cmp!=0){
            return cmp;
        }

        //if other==this, compare clientCode
        return this.clientCode.compareToIgnoreCase(other.clientCode);
    }
}
