package com.pao.project.bank.model.person;

import com.pao.project.bank.exception.InvalidOperationException;

public class CorporateClient extends Client{
    private String companyName;
    private String cui;
    private IndividualClient  legalRepresentative;

    public CorporateClient(int id, String email, String phoneNumber, String clientCode, boolean active, String companyName, String cui, IndividualClient legalRepresentative) {
        super(id, email, phoneNumber, clientCode, active);

        if (companyName == null || companyName.isBlank()) {
            throw new InvalidOperationException("Company name cannot be null or blank.");
        }
        if (cui == null || cui.isBlank()) {
            throw new InvalidOperationException("CUI cannot be null or blank.");
        }
        if (legalRepresentative == null) {
            throw new InvalidOperationException("Legal representative cannot be null.");
        }

        this.companyName = companyName;
        this.cui = cui;
        this.legalRepresentative = legalRepresentative;
    }


    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        if (companyName == null || companyName.isBlank()) {
            throw new InvalidOperationException("Company name cannot be null or blank.");
        }
        this.companyName = companyName;
    }

    public String getCui() {
        return cui;
    }

    public void setCui(String cui) {
        if (cui == null || cui.isBlank()) {
            throw new InvalidOperationException("CUI cannot be null or blank.");
        }
        this.cui = cui;
    }

    public IndividualClient getLegalRepresentative() {
        return legalRepresentative;
    }

    public void setLegalRepresentative(IndividualClient legalRepresentative) {
        if (legalRepresentative == null) {
            throw new InvalidOperationException("Legal representative cannot be null.");
        }
        this.legalRepresentative = legalRepresentative;
    }


    @Override
    public String getClientType() {
        return "Corporate Client";
    }

    @Override
    public String getFiscalIdentifier() {
        return this.cui;
    }

    @Override
    public String getFullName() {
        return this.companyName;
    }

    @Override
    public String toString() {
        return "CorporateClient{" +
                "id=" + id +
                ", companyName='" + companyName + '\'' +
                ", cui='" + cui + '\'' +
                ", legalRepresentative=" + legalRepresentative +
                ", clientCode='" + clientCode + '\'' +
                ", active=" + active +
                ", email='" + email + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                '}';
    }
}
