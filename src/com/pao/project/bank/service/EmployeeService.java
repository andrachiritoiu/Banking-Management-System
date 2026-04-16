package com.pao.project.bank.service;

public class EmployeeService {
    private static final EmployeeService INSTANCE = new EmployeeService();

    private EmployeeService(){}

    public static EmployeeService getInstance(){
        return INSTANCE;
    }
}
