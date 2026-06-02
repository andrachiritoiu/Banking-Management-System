package com.pao.project.bank.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

public class AuditService {
    private static final Path AUDIT_FILE_PATH = Path.of("audit.csv");
    private static final AuditService INSTANCE = new AuditService();

    private AuditService() {
        createFileWithHeaderIfNeeded();
    }

    public static AuditService getInstance() {
        return INSTANCE;
    }

    public synchronized void logAction(String actionName) {
        if (actionName == null || actionName.isBlank()) {
            throw new IllegalArgumentException("Action name cannot be null or blank.");
        }

        try (BufferedWriter writer = Files.newBufferedWriter(
                AUDIT_FILE_PATH,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        )) {
            writer.write(escapeCsv(actionName));
            writer.write(",");
            writer.write(LocalDateTime.now().toString());
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException("Could not write audit log.", e);
        }
    }

    public synchronized void log(String actionName) {
        logAction(actionName);
    }

    private void createFileWithHeaderIfNeeded() {
        try {
            if (!Files.exists(AUDIT_FILE_PATH) || Files.size(AUDIT_FILE_PATH) == 0) {
                try (BufferedWriter writer = Files.newBufferedWriter(
                        AUDIT_FILE_PATH,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND
                )) {
                    writer.write("action_name,timestamp");
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize audit file.", e);
        }
    }

    private String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }

        return value;
    }
}
