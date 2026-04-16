package com.pao.project.bank.service;

import com.pao.project.bank.model.Card;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuditService {
    private static final AuditService INSTANCE = new AuditService();

    private AuditService() {}

    public static AuditService getInstance() {
        return INSTANCE;
    }
}
