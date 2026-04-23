package com.pao.project.bank.service;

import com.pao.project.bank.model.person.BankEmployee;
import com.pao.project.bank.model.person.BankTeller;
import com.pao.project.bank.model.person.Client;
import com.pao.project.bank.model.person.FinancialAdvisor;

import java.util.ArrayList;
import java.util.List;

public class EmployeeService {
    private static final EmployeeService INSTANCE = new EmployeeService();

    private final List<BankEmployee> employees = new ArrayList<>();

    private EmployeeService(){}

    public static EmployeeService getInstance(){
        return INSTANCE;
    }

    //methods
    public void addEmployee(BankEmployee employee){
        if(employee == null){
            throw new IllegalArgumentException("Employee cannot be null.");
        }

        if (findByEmployeeCode(employee.getEmployeeCode()) != null) {
            throw new IllegalArgumentException("Employee code already exists.");
        }

        if (findByEmail(employee.getEmail()) != null) {
            throw new IllegalArgumentException("Employee email already exists.");
        }

        employees.add(employee);
    }

    public void removeEmployee(String employeeCode){
        employees.removeIf(employee -> employeeCode != null && employee.getEmployeeCode().equals(employeeCode));
    }

    public List<BankEmployee> getAllEmployees(){
        return new ArrayList<>(employees);
    }

    public BankEmployee findById(int employeeId) {
        for (BankEmployee employee : employees) {
            if (employee.getId() == employeeId) {
                return employee;
            }
        }
        return null;
    }

    public BankEmployee findByEmployeeCode(String employeeCode){
        if (employeeCode == null) {
            return null;
        }

        for(BankEmployee employee : employees){
            if(employee.getEmployeeCode().equals(employeeCode))
                return employee;
        }
        return null;
    }

    public BankEmployee findByEmail(String email) {
        if (email == null) {
            return null;
        }

        for (BankEmployee employee : employees) {
            if (employee.getEmail().equals(email)) {
                return employee;
            }
        }

        return null;
    }

    public List<BankTeller> findTellersByDesk(int deskNumber){
        List<BankTeller> result = new ArrayList<>();

        for(BankEmployee employee:employees){
            if(employee instanceof BankTeller bankTeller){
                if(bankTeller.getDeskNumber() == deskNumber)
                    result.add(bankTeller);
            }
        }

        return result;
    }

    public List<FinancialAdvisor> findAdvisorsBySpecialization(String specialization){
        List<FinancialAdvisor> result = new ArrayList<>();

        for(BankEmployee employee:employees){
            if(employee instanceof FinancialAdvisor financialAdvisor){
                if(financialAdvisor.getSpecialization().equals(specialization))
                    result.add(financialAdvisor);
            }
        }

        return result;
    }
}
