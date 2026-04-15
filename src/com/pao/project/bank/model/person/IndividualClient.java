package com.pao.project.bank.model.person;

import com.pao.project.bank.exception.InvalidOperationException;

import java.time.LocalDate;

public class IndividualClient extends Client{
    private String lastName;
    private String firstName;
    private String cnp;
    private LocalDate birthDate;


    public IndividualClient(int id, String email, String phoneNumber, String clientCode, boolean active, String lastName, String firstName, String cnp) {
        super(id, email, phoneNumber, clientCode, active);

        if (lastName == null || lastName.isBlank()) {
            throw new InvalidOperationException("Last name cannot be null or blank.");
        }
        if (firstName == null || firstName.isBlank()) {
            throw new InvalidOperationException("First name cannot be null or blank.");
        }
        if (cnp == null || cnp.isBlank()) {
            throw new InvalidOperationException("CNP cannot be null or blank.");
        }
        if (!isCnpValid(cnp)) {
            throw new InvalidOperationException("Invalid CNP.");
        }

        this.lastName = lastName;
        this.firstName = firstName;
        this.cnp = cnp;
        this.birthDate = extractBirthDateFromCnp(cnp);
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

    public String getCnp() {
        return cnp;
    }

    public void setCnp(String cnp) {
        if (cnp == null || cnp.isBlank()) {
            throw new InvalidOperationException("CNP cannot be null or blank.");
        }
        if (!isCnpValid(cnp)) {
            throw new InvalidOperationException("Invalid CNP.");
        }
        this.cnp = cnp;
        this.birthDate = extractBirthDateFromCnp(cnp);
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }



    private boolean isCnpValid(String cnp) {
        if (cnp == null || cnp.length() != 13) {
            return false;
        }

        for (char c : cnp.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }

        //1-m,before 1999
        //2-f,before 1999
        //3-m,before 1899
        //4-f,before 1899
        //5-m,after 2000
        //6-f,after 2000
        char firstDigit = cnp.charAt(0);
        if (firstDigit == '0' || firstDigit >= '7') {
            return false;
        }

        int year = Integer.parseInt(cnp.substring(1, 3));
        int month = Integer.parseInt(cnp.substring(3, 5));
        int day = Integer.parseInt(cnp.substring(5, 7));

        if (month < 1 || month > 12) {
            return false;
        }

        int fullYear;
        switch (firstDigit) {
            case '1':
            case '2':
                fullYear = 1900 + year;
                break;
            case '3':
            case '4':
                fullYear = 1800 + year;
                break;
            case '5':
            case '6':
                fullYear = 2000 + year;
                break;
            default:
                return false;
        }

        try {
            java.time.LocalDate.of(fullYear, month, day);
        } catch (Exception e) {
            return false;
        }

        //control digit
        String control = "279146358279";
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            sum += (control.charAt(i) - '0') * (cnp.charAt(i) - '0');
        }

        int controlDigit = sum % 11;
        if (controlDigit == 10) {
            controlDigit = 1;
        }

        return controlDigit == (cnp.charAt(12) - '0');
    }


    private LocalDate extractBirthDateFromCnp(String cnp) {
        char firstDigit = cnp.charAt(0);
        int year = Integer.parseInt(cnp.substring(1, 3));
        int month = Integer.parseInt(cnp.substring(3, 5));
        int day = Integer.parseInt(cnp.substring(5, 7));

        int fullYear;
        switch (firstDigit) {
            case '1':
            case '2':
                fullYear = 1900 + year;
                break;
            case '3':
            case '4':
                fullYear = 1800 + year;
                break;
            case '5':
            case '6':
                fullYear = 2000 + year;
                break;
            default:
                throw new InvalidOperationException("Invalid CNP.");
        }

        return LocalDate.of(fullYear, month, day);
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
        return (lastName == null ? "" : lastName) + " " +
                (firstName == null ? "" : firstName);
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
