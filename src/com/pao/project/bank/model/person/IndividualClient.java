package com.pao.project.bank.model.person;

import java.time.LocalDate;

public class IndividualClient extends Client{
    private String lastName;
    private String firstName;
    private String cnp;
    private LocalDate birthDate;


    public IndividualClient(int id, String email, String phoneNumber, String clientCode, boolean active, String lastName, String firstName, String cnp, LocalDate birthDate) {
        super(id, email, phoneNumber, clientCode, active);
        this.lastName = lastName;
        this.firstName = firstName;
        this.cnp = cnp;
        this.birthDate = birthDate;
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

    public String getCnp() {
        return cnp;
    }

    public void setCnp(String cnp) {
        this.cnp = cnp;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }


    @Override
    public String getClientType() {
        return "Individual Client";
    }

    @Override
    public String getFiscalIdentifier() {
        return this.cnp;
    }

    @Override
    public String getFullName() {
        return this.lastName + " " + this.firstName;
    }

    @Override
    public String toString() {
        return "IndividualClient{" +
                "id=" + id +
                ", lastName='" + lastName + '\'' +
                ", firstName='" + firstName + '\'' +
                ", cnp='" + cnp + '\'' +
                ", birthDate=" + birthDate +
                ", clientCode='" + clientCode + '\'' +
                ", active=" + active +
                ", email='" + email + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                '}';
    }
}
