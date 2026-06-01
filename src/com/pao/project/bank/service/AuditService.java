package com.pao.project.bank.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

public class AuditService {
    private static final AuditService INSTANCE = new AuditService();
    private static final Path AUDIT_FILE = Path.of("audit.csv");

    private AuditService() {}

    public static AuditService getInstance() {
        return INSTANCE;
    }

    public synchronized void log(String actionName) {
        if (actionName == null || actionName.isBlank()) {
            throw new IllegalArgumentException("Action name cannot be null or blank.");
        }

        boolean fileDoesNotExist = Files.notExists(AUDIT_FILE);

        try (BufferedWriter writer = Files.newBufferedWriter(
                AUDIT_FILE,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        )) {
            if (fileDoesNotExist) {
                writer.write("action_name,timestamp");
                writer.newLine();
            }

            writer.write(escapeCsv(actionName));
            writer.write(",");
            writer.write(LocalDateTime.now().toString());
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException("Could not write audit log.", e);
        }
    }

    private String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }

        return value;
    }
}
