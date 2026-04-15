package com.pao.project.bank.model.person;

public class CorporateClient extends Client{
    private String companyName;
    private String cui;
    private IndividualClient  legalRepresentative;

    public CorporateClient(int id, String email, String phoneNumber, String clientCode, boolean active, String companyName, String cui, IndividualClient legalRepresentative) {
        super(id, email, phoneNumber, clientCode, active);
        this.companyName = companyName;
        this.cui = cui;
        this.legalRepresentative = legalRepresentative;
    }


    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCui() {
        return cui;
    }

    public void setCui(String cui) {
        this.cui = cui;
    }

    public IndividualClient getLegalRepresentative() {
        return legalRepresentative;
    }

    public void setLegalRepresentative(IndividualClient legalRepresentative) {
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
