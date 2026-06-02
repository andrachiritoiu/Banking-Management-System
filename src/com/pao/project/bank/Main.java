package com.pao.project.bank;

import com.pao.project.bank.model.AccountStatement;
import com.pao.project.bank.model.Card;
import com.pao.project.bank.model.Cheque;
import com.pao.project.bank.model.IBAN;
import com.pao.project.bank.model.account.Account;
import com.pao.project.bank.model.account.CurrentAccount;
import com.pao.project.bank.model.account.SavingsAccount;
import com.pao.project.bank.model.enums.ChequeStatus;
import com.pao.project.bank.model.enums.CreditStatus;
import com.pao.project.bank.model.enums.CreditType;
import com.pao.project.bank.model.enums.TransactionType;
import com.pao.project.bank.model.person.BankTeller;
import com.pao.project.bank.model.person.Client;
import com.pao.project.bank.model.person.CorporateClient;
import com.pao.project.bank.model.person.FinancialAdvisor;
import com.pao.project.bank.model.person.IndividualClient;
import com.pao.project.bank.service.AccountService;
import com.pao.project.bank.service.AuditService;
import com.pao.project.bank.service.CardService;
import com.pao.project.bank.service.ChequeService;
import com.pao.project.bank.service.ClientService;
import com.pao.project.bank.service.CreditService;
import com.pao.project.bank.service.DatabaseViewService;
import com.pao.project.bank.service.EmployeeService;
import com.pao.project.bank.service.ReportService;
import com.pao.project.bank.service.TransactionService;
import com.pao.project.bank.util.DatabaseConnection;
import com.pao.project.bank.util.DatabaseSeeder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Random;
import java.util.Scanner;

public class Main {
    private static final Scanner scanner = new Scanner(System.in);

    private static ClientService clientService;
    private static EmployeeService employeeService;
    private static AccountService accountService;
    private static CardService cardService;
    private static TransactionService transactionService;
    private static ChequeService chequeService;
    private static ReportService reportService;
    private static CreditService creditService;
    private static DatabaseViewService databaseViewService;
    private static final AuditService auditService = AuditService.getInstance();
    private static boolean databaseMode;

    public static void main(String[] args) {
        printHeader("BANKING MANAGEMENT SYSTEM");

        if (args.length > 0 && "--seed-db".equalsIgnoreCase(args[0])) {
            DatabaseSeeder.seedDemoData(true);
            auditService.logAction("seed_database_cli");
            System.out.println("Database demo data seeded successfully.");
            return;
        }

        databaseMode = trySeedDatabaseDemoData();
        initializeServices();

        if (databaseMode) {
            System.out.println("MySQL is available. Database demo data seeded successfully.");
            System.out.println("In-memory demo data was not loaded.");
        } else {
            seedDemoData();
            System.out.println("MySQL is not available. In-memory demo data loaded successfully.");
        }

        runMenu();
    }

    private static boolean trySeedDatabaseDemoData() {
        try {
            DatabaseSeeder.seedDemoData();
            auditService.logAction("seed_database_startup");
            return true;
        } catch (RuntimeException e) {
            System.out.println("MySQL connection was found, but database startup seed failed: " + e.getMessage());
            System.out.println("If the schema is outdated, recreate the Docker volume or rerun resources/schema.sql.");
            return false;
        }
    }

    private static void initializeServices() {
        clientService = ClientService.getInstance();
        employeeService = EmployeeService.getInstance();
        accountService = AccountService.getInstance();
        cardService = CardService.getInstance();
        transactionService = TransactionService.getInstance();
        chequeService = ChequeService.getInstance();
        reportService = ReportService.getInstance();
        creditService = CreditService.getInstance();
        databaseViewService = DatabaseViewService.getInstance();
    }

    private static void seedDemoData() {
        try {
            IndividualClient ic1 = new IndividualClient(1, "ion.popescu@mail.com", "0711111111", "C001", true, "Ion", "Popescu", "1980101010017");
            IndividualClient ic2 = new IndividualClient(2, "maria.ionescu@mail.com", "0722222222", "C002", true, "Maria", "Ionescu", "2960730156784");

            CorporateClient cc1 = new CorporateClient(3, "office@techvision.ro", "0733333333", "C003", true, "TechVision SRL", "RO12345678", ic1);
            CorporateClient cc2 = new CorporateClient(4, "contact@finexpert.ro", "0744444444", "C004", true, "FinExpert SRL", "RO87654321", ic2);

            clientService.addClient(ic1);
            clientService.addClient(ic2);
            clientService.addClient(cc1);
            clientService.addClient(cc2);

            employeeService.addEmployee(new BankTeller(11, "teller1@bank.ro", "0750000001", "Marin", "Andreea", "E001", 4500.0, "Unirii Branch", 1));
            employeeService.addEmployee(new BankTeller(12, "teller2@bank.ro", "0750000002", "Georgescu", "Paul", "E002", 4700.0, "Victoriei Branch", 2));
            employeeService.addEmployee(new FinancialAdvisor(13, "advisor1@bank.ro", "0750000003", "Dumitrescu", "Radu", "E003", 6000.0, "Unirii Branch", "Investments"));
            employeeService.addEmployee(new FinancialAdvisor(14, "advisor2@bank.ro", "0750000004", "Stan", "Bianca", "E004", 6200.0, "Victoriei Branch", "Loans"));

            Account a1 = new CurrentAccount(1, new IBAN("RO49AAAA1B31007593840000"), 2500.0, "RON", ic1, 10.0);
            Account a2 = new SavingsAccount(2, new IBAN("RO49AAAA1B31007593840001"), 4000.0, "RON", ic2, 5.0, 0);
            Account a3 = new CurrentAccount(3, new IBAN("RO49AAAA1B31007593840002"), 12000.0, "RON", cc1, 20.0);
            Account a4 = new CurrentAccount(4, new IBAN("RO49AAAA1B31007593840003"), 15000.0, "RON", cc2, 25.0);

            accountService.openAccount(a1);
            accountService.openAccount(a2);
            accountService.openAccount(a3);
            accountService.openAccount(a4);

            cardService.issueCard(a1);
            cardService.issueCard(a2);
            cardService.issueCard(a3);
            cardService.issueCard(a4);

            accountService.deposit(a1.getIban().getCode(), 500.0);
            accountService.withdraw(a1.getIban().getCode(), 100.0);
            accountService.transfer(a3.getIban().getCode(), a1.getIban().getCode(), 300.0);

            Cheque cheque = new Cheque(a3, ic1, 200.0, LocalDate.now().plusDays(7));
            chequeService.issueCheque(cheque);

        } catch (Exception e) {
            System.out.println("Demo data already loaded or an error occurred: " + e.getMessage());
        }
    }

    private static void runMenu() {
        boolean running = true;

        while (running) {
            System.out.println("\nCurrent mode: " + (databaseMode ? "DATABASE / MySQL" : "IN-MEMORY FALLBACK"));
            System.out.println("""
                    
                    ---- MAIN MENU ----
                    1. Clients
                    2. Employees
                    3. Accounts & Transactions
                    4. Cards
                    5. Cheques
                    6. Reports
                    7. Credits
                    8. View All Data
                    9. Seed database demo data
                    10. Show startup mode
                    0. Exit
                    """);

            int option = readInt("Choose option: ");

            try {
                switch (option) {
                    case 1 -> clientMenu();
                    case 2 -> employeeMenu();
                    case 3 -> accountMenu();
                    case 4 -> cardMenu();
                    case 5 -> chequeMenu();
                    case 6 -> reportMenu();
                    case 7 -> creditMenu();
                    case 8 -> showAllData();
                    case 9 -> {
                        DatabaseSeeder.seedDemoData(true);
                        databaseMode = true;
                        auditService.logAction("seed_database_menu");
                        System.out.println("Database demo data seeded successfully.");
                    }
                    case 10 -> System.out.println(databaseMode
                            ? "Startup mode: MySQL database. In-memory demo data was not loaded."
                            : "Startup mode: in-memory fallback. MySQL seed was not available at startup.");
                    case 0 -> {
                        System.out.println("Exiting...");
                        running = false;
                    }
                    default -> System.out.println("Invalid option.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void clientMenu() {
        boolean back = false;

        while (!back) {
            System.out.println("""
                    
                    --- CLIENT MENU ---
                    1. Show all clients
                    2. Add individual client
                    3. Add corporate client
                    4. Remove client
                    5. Find by client code
                    6. Find individual client by CNP
                    7. Find corporate client by CUI
                    8. Sort clients by name
                    0. Back
                    """);

            int op = readInt("Choose: ");

            try {
                if (databaseMode) {
                    back = handleClientMenuJdbc(op);
                    continue;
                }

                switch (op) {
                    case 1 -> clientService.getAllClients().forEach(System.out::println);

                    case 2 -> {
                        IndividualClient c = new IndividualClient(
                                readInt("ID: "),
                                readLine("Email: "),
                                readLine("Phone: "),
                                readLine("Client code: "),
                                true,
                                readLine("First name: "),
                                readLine("Last name: "),
                                readLine("CNP: ")
                        );
                        clientService.addClient(c);
                        System.out.println("Individual client added.");
                    }

                    case 3 -> {
                        System.out.println("Legal representative data:");
                        IndividualClient rep = new IndividualClient(
                                readInt("Representative ID: "),
                                readLine("Representative email: "),
                                readLine("Representative phone: "),
                                readLine("Representative client code: "),
                                true,
                                readLine("Representative first name: "),
                                readLine("Representative last name: "),
                                readLine("Representative CNP: ")
                        );

                        CorporateClient comp = new CorporateClient(
                                readInt("Company ID: "),
                                readLine("Company email: "),
                                readLine("Company phone: "),
                                readLine("Company client code: "),
                                true,
                                readLine("Company name: "),
                                readLine("CUI: "),
                                rep
                        );

                        clientService.addClient(rep);
                        clientService.addClient(comp);
                        System.out.println("Corporate client added.");
                    }

                    case 4 -> {
                        clientService.removeClient(readLine("Client code: "));
                        System.out.println("Client removed if it existed.");
                    }

                    case 5 -> System.out.println(clientService.findByClientCode(readLine("Client code: ")));
                    case 6 -> System.out.println(clientService.findIndividualByCnp(readLine("CNP: ")));
                    case 7 -> System.out.println(clientService.findCorporateByCui(readLine("CUI: ")));
                    case 8 -> clientService.getClientsSortedByName().forEach(System.out::println);
                    case 0 -> back = true;
                    default -> System.out.println("Invalid option.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void employeeMenu() {
        boolean back = false;

        while (!back) {
            System.out.println("""
                    
                    --- EMPLOYEE MENU ---
                    1. Show all employees
                    2. Add bank teller
                    3. Add financial advisor
                    4. Remove employee
                    5. Find by employee code
                    6. Find by email
                    7. Find tellers by desk number
                    8. Find advisors by specialization
                    0. Back
                    """);

            int op = readInt("Choose: ");

            try {
                if (databaseMode) {
                    back = handleEmployeeMenuJdbc(op);
                    continue;
                }

                switch (op) {
                    case 1 -> employeeService.getAllEmployees().forEach(System.out::println);

                    case 2 -> {
                        BankTeller teller = new BankTeller(
                                readInt("ID: "),
                                readLine("Email: "),
                                readLine("Phone: "),
                                readLine("Last name: "),
                                readLine("First name: "),
                                readLine("Employee code: "),
                                readDouble("Salary: "),
                                readLine("Branch: "),
                                readInt("Desk number: ")
                        );
                        employeeService.addEmployee(teller);
                        System.out.println("Bank teller added.");
                    }

                    case 3 -> {
                        FinancialAdvisor advisor = new FinancialAdvisor(
                                readInt("ID: "),
                                readLine("Email: "),
                                readLine("Phone: "),
                                readLine("Last name: "),
                                readLine("First name: "),
                                readLine("Employee code: "),
                                readDouble("Salary: "),
                                readLine("Branch: "),
                                readLine("Specialization: ")
                        );
                        employeeService.addEmployee(advisor);
                        System.out.println("Financial advisor added.");
                    }

                    case 4 -> {
                        employeeService.removeEmployee(readLine("Employee code: "));
                        System.out.println("Employee removed if it existed.");
                    }

                    case 5 -> System.out.println(employeeService.findByEmployeeCode(readLine("Employee code: ")));
                    case 6 -> System.out.println(employeeService.findByEmail(readLine("Email: ")));
                    case 7 -> employeeService.findTellersByDesk(readInt("Desk number: ")).forEach(System.out::println);
                    case 8 -> employeeService.findAdvisorsBySpecialization(readLine("Specialization: ")).forEach(System.out::println);
                    case 0 -> back = true;
                    default -> System.out.println("Invalid option.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void accountMenu() {
        boolean back = false;

        while (!back) {
            System.out.println("""
                    
                    --- ACCOUNT & TRANSACTION MENU ---
                    1. Show all accounts
                    2. Open current account
                    3. Open savings account
                    4. Find account by IBAN
                    5. Show accounts for client
                    6. Close account
                    7. Deposit
                    8. Withdraw
                    9. Transfer
                    10. Exchange
                    11. Set IBAN alias
                    12. Find account by alias
                    13. Transfer by alias
                    14. Show aliases
                    15. Show all transactions for account
                    16. Show transactions for account by type
                    17. Show transactions for account sorted by date
                    0. Back
                    """);

            int op = readInt("Choose: ");

            try {
                if (databaseMode) {
                    back = handleAccountMenuJdbc(op);
                    continue;
                }

                switch (op) {
                    case 1 -> accountService.getAllAccounts().forEach(System.out::println);

                    case 2 -> {
                        Client owner = clientService.findByClientCode(readLine("Client code: "));
                        if (owner == null) {
                            System.out.println("Client not found.");
                            break;
                        }

                        CurrentAccount account = new CurrentAccount(
                                readInt("ID: "),
                                IBAN.generate(),
                                readDouble("Initial balance: "),
                                readLine("Currency: "),
                                owner,
                                readDouble("Monthly fee: ")
                        );

                        accountService.openAccount(account);
                        System.out.println("Current account opened.");
                        System.out.println("Generated IBAN: " + account.getIban().getCode());
                    }

                    case 3 -> {
                        Client owner = clientService.findByClientCode(readLine("Client code: "));
                        if (owner == null) {
                            System.out.println("Client not found.");
                            break;
                        }

                        SavingsAccount account = new SavingsAccount(
                                readInt("ID: "),
                                IBAN.generate(),
                                readDouble("Initial balance: "),
                                readLine("Currency: "),
                                owner,
                                readDouble("Interest rate: "),
                                0
                        );

                        accountService.openAccount(account);
                        System.out.println("Savings account opened.");
                    }

                    case 4 -> System.out.println(accountService.findByIban(readLine("IBAN: ")));

                    case 5 -> {
                        Client client = clientService.findByClientCode(readLine("Client code: "));
                        if (client == null) {
                            System.out.println("Client not found.");
                            break;
                        }
                        accountService.getAccountsForClient(client).forEach(System.out::println);
                    }

                    case 6 -> {
                        accountService.closeAccount(readLine("IBAN: "));
                        System.out.println("Account close operation executed.");
                    }

                    case 7 -> {
                        accountService.depositJdbc(readLine("IBAN: "), readDouble("Amount: "));
                        System.out.println("Deposit completed.");
                    }

                    case 8 -> {
                        accountService.withdrawJdbc(readLine("IBAN: "), readDouble("Amount: "));
                        System.out.println("Withdrawal completed.");
                    }

                    case 9 -> {
                        accountService.transferJdbc(
                                readLine("Source IBAN: "),
                                readLine("Destination IBAN: "),
                                readDouble("Amount: ")
                        );
                        System.out.println("Transfer completed.");
                    }

                    case 10 -> {
                        accountService.exchangeJdbc(
                                readLine("Source IBAN: "),
                                readLine("Destination IBAN: "),
                                readDouble("Source amount: "),
                                readDouble("Exchange rate: ")
                        );
                        System.out.println("Exchange completed.");
                    }

                    case 11 -> {
                        accountService.setAlias(
                                readLine("Alias: "),
                                readLine("IBAN: ")
                        );
                        System.out.println("Alias saved.");
                    }

                    case 12 -> System.out.println(accountService.findByAlias(readLine("Alias: ")));

                    case 13 -> {
                        accountService.transferByAlias(
                                readLine("Source IBAN: "),
                                readLine("Destination alias: "),
                                readDouble("Amount: ")
                        );
                        System.out.println("Transfer by alias completed.");
                    }

                    case 14 -> accountService.getIbanAliases()
                            .forEach((alias, iban) -> System.out.println(alias + " -> " + iban));

                    case 15 -> transactionService.getTransactionsForAccount(readLine("IBAN: ")).forEach(System.out::println);

                    case 16 -> {
                        String iban = readLine("IBAN: ");
                        System.out.println("1. Deposit");
                        System.out.println("2. Withdrawal");
                        System.out.println("3. Transfer");
                        System.out.println("4. Exchange");
                        int typeOption = readInt("Choose type: ");

                        switch (typeOption) {
                            case 1 -> transactionService.getTransactionsForAccountByType(iban, TransactionType.DEPOSIT).forEach(System.out::println);
                            case 2 -> transactionService.getTransactionsForAccountByType(iban, TransactionType.WITHDRAWAL).forEach(System.out::println);
                            case 3 -> transactionService.getTransactionsForAccountByType(iban, TransactionType.TRANSFER).forEach(System.out::println);
                            case 4 -> transactionService.getTransactionsForAccountByType(iban, TransactionType.EXCHANGE).forEach(System.out::println);
                            default -> System.out.println("Invalid type.");
                        }
                    }

                    case 17 -> transactionService.getTransactionsSortedByDate(readLine("IBAN: ")).forEach(System.out::println);

                    case 0 -> back = true;
                    default -> System.out.println("Invalid option.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void cardMenu() {
        boolean back = false;

        while (!back) {
            System.out.println("""
                    
                    --- CARD MENU ---
                    1. Issue card for account
                    2. Issue custom card for account
                    3. Find by card number
                    4. Block card
                    5. Unblock card
                    6. Validate card
                    7. Show all cards
                    8. Show cards for account
                    0. Back
                    """);

            int op = readInt("Choose: ");

            try {
                if (databaseMode) {
                    back = handleCardMenuJdbc(op);
                    continue;
                }

                switch (op) {
                    case 1 -> {
                        Account acc = accountService.findByIban(readLine("IBAN: "));
                        if (acc == null) {
                            System.out.println("Account not found.");
                            break;
                        }
                        System.out.println(cardService.issueCard(acc));
                    }

                    case 2 -> {
                        Account acc = accountService.findByIban(readLine("IBAN: "));
                        if (acc == null) {
                            System.out.println("Account not found.");
                            break;
                        }

                        Card card = cardService.issueCard(
                                acc,
                                LocalDate.now().plusYears(readInt("Years until expiration: ")),
                                readLine("Contactless (true/false): ").equalsIgnoreCase("true")
                        );

                        System.out.println(card);
                    }

                    case 3 -> System.out.println(cardService.findByCardNumber(readLine("Card number: ")));

                    case 4 -> {
                        cardService.blockCard(readLine("Card number: "));
                        System.out.println("Card blocked.");
                    }

                    case 5 -> {
                        cardService.unblockCard(readLine("Card number: "));
                        System.out.println("Card unblocked.");
                    }

                    case 6 -> {
                        cardService.validateCard(readLine("Card number: "));
                        System.out.println("Card is valid.");
                    }

                    case 7 -> cardService.getAllCards().forEach(System.out::println);

                    case 8 -> {
                        Account acc = accountService.findByIban(readLine("IBAN: "));
                        if (acc == null) {
                            System.out.println("Account not found.");
                            break;
                        }
                        cardService.getCardsForAccount(acc).forEach(System.out::println);
                    }

                    case 0 -> back = true;
                    default -> System.out.println("Invalid option.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void chequeMenu() {
        boolean back = false;

        while (!back) {
            System.out.println("""
                    
                    --- CHEQUE MENU ---
                    1. Issue cheque
                    2. Find cheque by series
                    3. Show all cheques
                    4. Show cheques by status
                    5. Cash cheque
                    6. Cancel cheque
                    0. Back
                    """);

            int op = readInt("Choose: ");

            try {
                if (databaseMode) {
                    back = handleChequeMenuJdbc(op);
                    continue;
                }

                switch (op) {
                    case 1 -> {
                        Account issuer = accountService.findByIban(readLine("Issuer IBAN: "));
                        Client beneficiary = clientService.findByClientCode(readLine("Beneficiary client code: "));

                        if (issuer == null || beneficiary == null) {
                            System.out.println("Invalid issuer or beneficiary.");
                            break;
                        }

                        Cheque cheque = new Cheque(
                                issuer,
                                beneficiary,
                                readDouble("Amount: "),
                                LocalDate.now().plusDays(readInt("Valid days: "))
                        );

                        chequeService.issueCheque(cheque);
                        System.out.println("Cheque issued.");
                        System.out.println(cheque);
                    }

                    case 2 -> System.out.println(chequeService.findBySeries(readLine("Series: ")));

                    case 3 -> chequeService.getAllCheques().forEach(System.out::println);

                    case 4 -> {
                        System.out.println("1. ISSUED");
                        System.out.println("2. CASHED");
                        System.out.println("3. CANCELLED");
                        System.out.println("4. EXPIRED");
                        int s = readInt("Choose status: ");

                        switch (s) {
                            case 1 -> chequeService.getChequesByStatus(ChequeStatus.ISSUED).forEach(System.out::println);
                            case 2 -> chequeService.getChequesByStatus(ChequeStatus.CASHED).forEach(System.out::println);
                            case 3 -> chequeService.getChequesByStatus(ChequeStatus.CANCELLED).forEach(System.out::println);
                            case 4 -> chequeService.getChequesByStatus(ChequeStatus.EXPIRED).forEach(System.out::println);
                            default -> System.out.println("Invalid status.");
                        }
                    }

                    case 5 -> {
                        String series = readLine("Cheque series: ");
                        Account beneficiaryAccount = accountService.findByIban(readLine("Beneficiary IBAN: "));
                        chequeService.cashCheque(series, beneficiaryAccount);
                        System.out.println("Cheque cashed.");
                    }

                    case 6 -> {
                        chequeService.cancelCheque(readLine("Cheque series: "));
                        System.out.println("Cheque cancelled.");
                    }

                    case 0 -> back = true;
                    default -> System.out.println("Invalid option.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void reportMenu() {
        boolean back = false;

        while (!back) {
            System.out.println("""
                    
                    --- REPORT MENU ---
                    1. Account statement
                    2. Total inflows for account
                    3. Total outflows for account
                    4. Transaction history for account
                    5. Monthly account statement
                    6. Total incoming by month
                    7. Total outgoing by month
                    8. Top clients by balance
                    9. Transactions grouped by type
                    10. Accounts grouped by currency
                    11. Credits grouped by status
                    0. Back
                    """);

            int op = readInt("Choose: ");

            try {
                if (databaseMode) {
                    back = handleReportMenuJdbc(op);
                    continue;
                }

                switch (op) {
                    case 1 -> {
                        Account acc = accountService.findByIban(readLine("IBAN: "));
                        if (acc == null) {
                            System.out.println("Account not found.");
                            break;
                        }
                        AccountStatement statement = reportService.generateAccountStatement(acc);
                        System.out.println(statement);
                    }

                    case 2 -> {
                        Account acc = accountService.findByIban(readLine("IBAN: "));
                        if (acc == null) {
                            System.out.println("Account not found.");
                            break;
                        }
                        System.out.println("Total inflows: " + reportService.calculateTotalInflows(acc));
                    }

                    case 3 -> {
                        Account acc = accountService.findByIban(readLine("IBAN: "));
                        if (acc == null) {
                            System.out.println("Account not found.");
                            break;
                        }
                        System.out.println("Total outflows: " + reportService.calculateTotalOutflows(acc));
                    }

                    case 4 -> {
                        Account acc = accountService.findByIban(readLine("IBAN: "));
                        if (acc == null) {
                            System.out.println("Account not found.");
                            break;
                        }
                        reportService.getTransactionHistory(acc).forEach(System.out::println);
                    }

                    case 5 -> {
                        Account acc = accountService.findByIban(readLine("IBAN: "));
                        if (acc == null) {
                            System.out.println("Account not found.");
                            break;
                        }
                        YearMonth month = YearMonth.parse(readLine("Month (YYYY-MM): "));
                        System.out.println(reportService.generateMonthlyAccountStatement(acc, month));
                    }

                    case 6 -> {
                        Account acc = accountService.findByIban(readLine("IBAN: "));
                        if (acc == null) {
                            System.out.println("Account not found.");
                            break;
                        }
                        reportService.calculateTotalIncomingByMonth(acc)
                                .forEach((month, total) -> System.out.println(month + " -> " + total));
                    }

                    case 7 -> {
                        Account acc = accountService.findByIban(readLine("IBAN: "));
                        if (acc == null) {
                            System.out.println("Account not found.");
                            break;
                        }
                        reportService.calculateTotalOutgoingByMonth(acc)
                                .forEach((month, total) -> System.out.println(month + " -> " + total));
                    }

                    case 8 -> reportService.getTopClientsByBalance(readInt("Limit: "))
                            .forEach((client, balance) -> System.out.println(client.getFullName() + " -> " + balance));

                    case 9 -> reportService.getTransactionsGroupedByType()
                            .forEach((type, transactions) -> {
                                System.out.println("\n" + type + ":");
                                transactions.forEach(System.out::println);
                            });

                    case 10 -> reportService.getAccountsGroupedByCurrency()
                            .forEach((currency, accounts) -> {
                                System.out.println("\n" + currency + ":");
                                accounts.forEach(System.out::println);
                            });

                    case 11 -> reportService.getCreditsGroupedByStatus()
                            .forEach((status, credits) -> {
                                System.out.println("\n" + status + ":");
                                credits.forEach(System.out::println);
                            });

                    case 0 -> back = true;
                    default -> System.out.println("Invalid option.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void creditMenu() {
        boolean back = false;

        while (!back) {
            System.out.println("""
                    
                    --- CREDIT MENU ---
                    1. Create credit
                    2. Approve credit
                    3. Reject credit
                    4. Pay installment
                    5. Find credit by ID
                    6. Show all credits
                    7. Show credits for client
                    8. Show credits by status
                    0. Back
                    """);

            int op = readInt("Choose: ");

            try {
                if (databaseMode) {
                    back = handleCreditMenuJdbc(op);
                    continue;
                }

                switch (op) {
                    case 1 -> {
                        Client borrower = clientService.findByClientCode(readLine("Client code: "));
                        if (borrower == null) {
                            System.out.println("Client not found.");
                            break;
                        }

                        System.out.println("1. Personal");
                        System.out.println("2. Mortgage");
                        System.out.println("3. Business");
                        CreditType type = readCreditType(readInt("Credit type: "));

                        System.out.println(creditService.createCredit(
                                borrower,
                                readLine("Target IBAN: "),
                                type,
                                readDouble("Principal amount: "),
                                readDouble("Annual interest rate: "),
                                readInt("Duration in months: ")
                        ));
                    }

                    case 2 -> {
                        creditService.approveCredit(readInt("Credit ID: "));
                        System.out.println("Credit approved and amount deposited.");
                    }

                    case 3 -> {
                        creditService.rejectCredit(readInt("Credit ID: "));
                        System.out.println("Credit rejected.");
                    }

                    case 4 -> {
                        creditService.payInstallment(
                                readInt("Credit ID: "),
                                readDouble("Amount: ")
                        );
                        System.out.println("Installment paid.");
                    }

                    case 5 -> System.out.println(creditService.findById(readInt("Credit ID: ")));
                    case 6 -> creditService.getAllCredits().forEach(System.out::println);

                    case 7 -> {
                        Client client = clientService.findByClientCode(readLine("Client code: "));
                        if (client == null) {
                            System.out.println("Client not found.");
                            break;
                        }
                        creditService.getCreditsForClient(client).forEach(System.out::println);
                    }

                    case 8 -> {
                        System.out.println("1. Pending");
                        System.out.println("2. Active");
                        System.out.println("3. Paid");
                        System.out.println("4. Rejected");
                        System.out.println("5. Defaulted");
                        CreditStatus status = readCreditStatus(readInt("Status: "));
                        creditService.getCreditsByStatus(status).forEach(System.out::println);
                    }

                    case 0 -> back = true;
                    default -> System.out.println("Invalid option.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static boolean handleClientMenuJdbc(int op) throws SQLException {
        switch (op) {
            case 1 -> showDatabaseClients();
            case 2 -> addIndividualClientJdbc();
            case 3 -> addCorporateClientJdbc();
            case 4 -> removeClientJdbc(readExistingClientCode("Client code: "));
            case 5 -> showClientByColumnJdbc("c.client_code", readLine("Client code: "));
            case 6 -> showClientByColumnJdbc("ic.cnp", readLine("CNP: "));
            case 7 -> showClientByColumnJdbc("cc.cui", readLine("CUI: "));
            case 8 -> showClientsSortedByNameJdbc();
            case 0 -> {
                return true;
            }
            default -> System.out.println("Invalid option.");
        }

        return false;
    }

    private static boolean handleEmployeeMenuJdbc(int op) throws SQLException {
        switch (op) {
            case 1 -> showDatabaseEmployees();
            case 2 -> addBankTellerJdbc();
            case 3 -> addFinancialAdvisorJdbc();
            case 4 -> deleteEmployeeJdbc(readExistingEmployeeCode("Employee code: "));
            case 5 -> showEmployeeByColumnJdbc("e.employee_code", readLine("Employee code: "));
            case 6 -> showEmployeeByColumnJdbc("p.email", readLine("Email: "));
            case 7 -> showBankTellersByDeskJdbc(readInt("Desk number: "));
            case 8 -> showFinancialAdvisorsBySpecializationJdbc(readLine("Specialization: "));
            case 0 -> {
                return true;
            }
            default -> System.out.println("Invalid option.");
        }

        return false;
    }

    private static boolean handleAccountMenuJdbc(int op) throws SQLException {
        switch (op) {
            case 1 -> showDatabaseAccounts();
            case 2 -> openAccountFromMenuJdbc("CURRENT");
            case 3 -> openAccountFromMenuJdbc("SAVINGS");
            case 4 -> showAccountByIbanJdbc(readLine("IBAN: "));
            case 5 -> showAccountsForClientCodeJdbc(readLine("Client code: "));
            case 6 -> {
                accountService.closeAccountJdbc(readLine("IBAN: "));
                System.out.println("Account close operation executed.");
            }
            case 7 -> {
                accountService.depositJdbc(readExistingAccountIban("IBAN: "), readPositiveDouble("Amount: "));
                System.out.println("Deposit completed.");
            }
            case 8 -> {
                accountService.withdrawJdbc(readExistingAccountIban("IBAN: "), readPositiveDouble("Amount: "));
                System.out.println("Withdrawal completed.");
            }
            case 9 -> {
                accountService.transferJdbc(
                        readExistingAccountIban("Source IBAN: "),
                        readExistingAccountIban("Destination IBAN: "),
                        readPositiveDouble("Amount: ")
                );
                System.out.println("Transfer completed.");
            }
            case 10 -> {
                accountService.exchangeJdbc(
                        readExistingAccountIban("Source IBAN: "),
                        readExistingAccountIban("Destination IBAN: "),
                        readPositiveDouble("Source amount: "),
                        readPositiveDouble("Exchange rate: ")
                );
                System.out.println("Exchange completed.");
            }
            case 11 -> setAliasJdbc(readLine("Alias: "), readLine("IBAN: "));
            case 12 -> showAliasJdbc(readLine("Alias: "));
            case 13 -> transferByAliasJdbc(readExistingAccountIban("Source IBAN: "), readLine("Destination alias: "), readPositiveDouble("Amount: "));
            case 14 -> showAliasesJdbc();
            case 15 -> showTransactionsForAccountJdbc(readLine("IBAN: "), null);
            case 16 -> {
                String iban = readLine("IBAN: ");
                System.out.println("1. Deposit");
                System.out.println("2. Withdrawal");
                System.out.println("3. Transfer");
                System.out.println("4. Exchange");
                TransactionType type = readTransactionTypeOption();
                showTransactionsForAccountJdbc(iban, type);
            }
            case 17 -> showTransactionsForAccountJdbc(readLine("IBAN: "), null);
            case 0 -> {
                return true;
            }
            default -> System.out.println("Invalid option.");
        }

        return false;
    }

    private static boolean handleCardMenuJdbc(int op) throws SQLException {
        switch (op) {
            case 1 -> issueCardJdbc(readLine("IBAN: "), LocalDate.now().plusYears(3), true);
            case 2 -> issueCardJdbc(
                    readLine("IBAN: "),
                    LocalDate.now().plusYears(readPositiveInt("Years until expiration: ")),
                    readLine("Contactless (true/false): ").equalsIgnoreCase("true")
            );
            case 3 -> showCardByNumberJdbc(readLine("Card number: "));
            case 4 -> updateCardStatusJdbc(readLine("Card number: "), "BLOCKED");
            case 5 -> updateCardStatusJdbc(readLine("Card number: "), "ACTIVE");
            case 6 -> validateCardJdbc(readLine("Card number: "));
            case 7 -> showDatabaseCards();
            case 8 -> showCardsForAccountJdbc(readLine("IBAN: "));
            case 0 -> {
                return true;
            }
            default -> System.out.println("Invalid option.");
        }

        return false;
    }

    private static boolean handleChequeMenuJdbc(int op) throws SQLException {
        switch (op) {
            case 1 -> issueChequeJdbc();
            case 2 -> showChequeBySeriesJdbc(readLine("Series: "));
            case 3 -> showDatabaseCheques();
            case 4 -> showChequesByStatusJdbc(readChequeStatusOption());
            case 5 -> updateChequeStatusJdbc(readLine("Cheque series: "), "CASHED");
            case 6 -> updateChequeStatusJdbc(readLine("Cheque series: "), "CANCELLED");
            case 0 -> {
                return true;
            }
            default -> System.out.println("Invalid option.");
        }

        return false;
    }

    private static boolean handleReportMenuJdbc(int op) throws SQLException {
        switch (op) {
            case 1 -> reportService.getAccountStatementJdbc(readLine("IBAN: ")).forEach(System.out::println);
            case 2 -> showAccountFlowTotalJdbc(readLine("IBAN: "), true);
            case 3 -> showAccountFlowTotalJdbc(readLine("IBAN: "), false);
            case 4 -> showTransactionsForAccountJdbc(readLine("IBAN: "), null);
            case 5 -> showMonthlyStatementJdbc(readLine("IBAN: "), YearMonth.parse(readLine("Month (YYYY-MM): ")));
            case 6 -> showMonthlyFlowJdbc(readLine("IBAN: "), true);
            case 7 -> showMonthlyFlowJdbc(readLine("IBAN: "), false);
            case 8 -> reportService.getClientBalanceSummaryJdbc().forEach(System.out::println);
            case 9 -> showTransactionsGroupedByTypeJdbc();
            case 10 -> showAccountsGroupedByCurrencyJdbc();
            case 11 -> showCreditsGroupedByStatusJdbc();
            case 0 -> {
                return true;
            }
            default -> System.out.println("Invalid option.");
        }

        return false;
    }

    private static boolean handleCreditMenuJdbc(int op) throws SQLException {
        switch (op) {
            case 1 -> applyForCreditFromMenuJdbc();
            case 2 -> approveCreditJdbc(readInt("Credit ID: "));
            case 3 -> updateJdbc("UPDATE credits SET status = 'REJECTED' WHERE id = ?", readInt("Credit ID: "));
            case 4 -> creditService.payInstallmentJdbc(readPositiveInt("Credit ID: "), readPositiveInt("Installment number: "));
            case 5 -> showCreditByIdJdbc(readInt("Credit ID: "));
            case 6 -> showDatabaseCredits();
            case 7 -> showCreditsForClientJdbc(readLine("Client code: "));
            case 8 -> showCreditsByStatusJdbc(readCreditStatusOption("""
                    1. Pending
                    2. Active
                    3. Paid
                    4. Rejected
                    5. Defaulted
                    Status: """).name());
            case 0 -> {
                return true;
            }
            default -> System.out.println("Invalid option.");
        }

        return false;
    }

    private static CreditType readCreditType(int option) {
        return switch (option) {
            case 1 -> CreditType.PERSONAL;
            case 2 -> CreditType.MORTGAGE;
            case 3 -> CreditType.BUSINESS;
            default -> throw new IllegalArgumentException("Invalid credit type.");
        };
    }

    private static CreditStatus readCreditStatus(int option) {
        return switch (option) {
            case 1 -> CreditStatus.PENDING;
            case 2 -> CreditStatus.ACTIVE;
            case 3 -> CreditStatus.PAID;
            case 4 -> CreditStatus.REJECTED;
            case 5 -> CreditStatus.DEFAULTED;
            default -> throw new IllegalArgumentException("Invalid credit status.");
        };
    }

    private static TransactionType readTransactionType(int option) {
        return switch (option) {
            case 1 -> TransactionType.DEPOSIT;
            case 2 -> TransactionType.WITHDRAWAL;
            case 3 -> TransactionType.TRANSFER;
            case 4 -> TransactionType.EXCHANGE;
            default -> throw new IllegalArgumentException("Invalid transaction type.");
        };
    }

    private static TransactionType readTransactionTypeOption() {
        while (true) {
            try {
                return readTransactionType(readInt("Choose type: "));
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static CreditType readCreditTypeOption(String message) {
        while (true) {
            try {
                return readCreditType(readInt(message));
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
                message = "Credit type: ";
            }
        }
    }

    private static CreditStatus readCreditStatusOption(String message) {
        while (true) {
            try {
                return readCreditStatus(readInt(message));
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
                message = "Status: ";
            }
        }
    }

    private static String readValidCnp(String message) {
        while (true) {
            String cnp = readLine(message);
            try {
                parseBirthDateFromCnp(cnp);
                return cnp;
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
                message = "CNP: ";
            }
        }
    }

    private static int readPositiveInt(String message) {
        while (true) {
            int value = readInt(message);
            if (value > 0) {
                return value;
            }

            System.out.println("Value must be positive.");
            message = "Enter a positive integer: ";
        }
    }

    private static double readPositiveDouble(String message) {
        while (true) {
            double value = readDouble(message);
            if (value > 0) {
                return value;
            }

            System.out.println("Value must be positive.");
            message = "Enter a positive number: ";
        }
    }

    private static double readNonNegativeDouble(String message) {
        while (true) {
            double value = readDouble(message);
            if (value >= 0) {
                return value;
            }

            System.out.println("Value cannot be negative.");
            message = "Enter a non-negative number: ";
        }
    }

    private static String readCurrencyCode(String message) {
        while (true) {
            String currency = readLine(message).trim().toUpperCase();
            if (currency.equals("RON") || currency.equals("EUR") || currency.equals("USD") || currency.equals("GBP")) {
                return currency;
            }

            System.out.println("Currency invalid. Accepted values: RON, EUR, USD, GBP.");
            message = "Currency (RON/EUR/USD/GBP): ";
        }
    }

    private static int readExistingClientIdByCode(String message) throws SQLException {
        while (true) {
            String clientCode = readLine(message);
            try {
                return findClientIdByCodeJdbc(clientCode);
            } catch (SQLException e) {
                System.out.println("Client code not found.");
                message = "Client code: ";
            }
        }
    }

    private static String readExistingClientCode(String message) throws SQLException {
        while (true) {
            String clientCode = readLine(message);
            try {
                findClientIdByCodeJdbc(clientCode);
                return clientCode;
            } catch (SQLException e) {
                System.out.println("Client code not found.");
                message = "Client code: ";
            }
        }
    }

    private static String readExistingEmployeeCode(String message) throws SQLException {
        while (true) {
            String employeeCode = readLine(message);
            try {
                findIntJdbc("SELECT id FROM employees WHERE employee_code = ?", employeeCode);
                return employeeCode;
            } catch (SQLException e) {
                System.out.println("Employee code not found.");
                message = "Employee code: ";
            }
        }
    }

    private static int readExistingAccountIdByIban(String message) throws SQLException {
        return readExistingAccountIdByIban(message, null);
    }

    private static int readExistingAccountIdByIban(String message, String firstValue) throws SQLException {
        String iban = firstValue;
        while (true) {
            if (iban == null || iban.isBlank()) {
                iban = readLine(message);
            }

            try {
                return findAccountIdByIbanJdbc(iban);
            } catch (SQLException e) {
                System.out.println("IBAN not found.");
                iban = null;
                message = "IBAN: ";
            }
        }
    }

    private static String readExistingAccountIban(String message) throws SQLException {
        while (true) {
            String iban = readLine(message);
            try {
                findAccountIdByIbanJdbc(iban);
                return iban;
            } catch (SQLException e) {
                System.out.println("IBAN not found.");
                message = "IBAN: ";
            }
        }
    }

    private static String readChequeStatusOption() {
        while (true) {
            System.out.println("1. ISSUED");
            System.out.println("2. CASHED");
            System.out.println("3. CANCELLED");
            System.out.println("4. EXPIRED");

            switch (readInt("Choose status: ")) {
                case 1 -> {
                    return "ISSUED";
                }
                case 2 -> {
                    return "CASHED";
                }
                case 3 -> {
                    return "CANCELLED";
                }
                case 4 -> {
                    return "EXPIRED";
                }
                default -> System.out.println("Invalid cheque status.");
            }
        }
    }

    private static void addIndividualClientJdbc() throws SQLException {
        String email = readLine("Email: ");
        String phone = readLine("Phone: ");
        String clientCode = readLine("Client code: ");
        String firstName = readLine("First name: ");
        String lastName = readLine("Last name: ");
        String cnp = readValidCnp("CNP: ");
        LocalDate birthDate = parseBirthDateFromCnp(cnp);

        executeInTransaction(connection -> {
            int id = insertPersonJdbc(connection, "CLIENT", email, phone);
            insertClientJdbc(connection, id, clientCode, "INDIVIDUAL", true);
            updateJdbc(connection, """
                    INSERT INTO individual_clients (client_id, first_name, last_name, cnp, birth_date)
                    VALUES (?, ?, ?, ?, ?)
                    """, id, firstName, lastName, cnp, java.sql.Date.valueOf(birthDate));
        });

        System.out.println("Individual client saved in database.");
    }

    private static void addCorporateClientJdbc() throws SQLException {
        System.out.println("Legal representative must already exist as an individual client.");
        String email = readLine("Company email: ");
        String phone = readLine("Company phone: ");
        String clientCode = readLine("Company client code: ");
        String companyName = readLine("Company name: ");
        String cui = readLine("CUI: ");
        int representativeId = readExistingClientIdByCode("Representative client code: ");

        executeInTransaction(connection -> {
            int id = insertPersonJdbc(connection, "CLIENT", email, phone);
            insertClientJdbc(connection, id, clientCode, "CORPORATE", true);
            updateJdbc(connection, """
                    INSERT INTO corporate_clients (client_id, company_name, cui, legal_representative_id)
                    VALUES (?, ?, ?, ?)
                    """, id, companyName, cui, representativeId);
        });

        System.out.println("Corporate client saved in database.");
    }

    private static void addBankTellerJdbc() throws SQLException {
        String email = readLine("Email: ");
        String phone = readLine("Phone: ");
        String lastName = readLine("Last name: ");
        String firstName = readLine("First name: ");
        String employeeCode = readLine("Employee code: ");
        double salary = readPositiveDouble("Salary: ");
        String branch = readLine("Branch: ");
        int deskNumber = readPositiveInt("Desk number: ");

        executeInTransaction(connection -> {
            int id = insertPersonJdbc(connection, "EMPLOYEE", email, phone);
            insertEmployeeJdbc(connection, id, employeeCode, "BANK_TELLER", firstName, lastName, salary, branch);
            updateJdbc(connection, """
                    INSERT INTO bank_tellers (employee_id, desk_number)
                    VALUES (?, ?)
                    """, id, deskNumber);
        });

        System.out.println("Bank teller saved in database.");
    }

    private static void addFinancialAdvisorJdbc() throws SQLException {
        String email = readLine("Email: ");
        String phone = readLine("Phone: ");
        String lastName = readLine("Last name: ");
        String firstName = readLine("First name: ");
        String employeeCode = readLine("Employee code: ");
        double salary = readPositiveDouble("Salary: ");
        String branch = readLine("Branch: ");
        String specialization = readLine("Specialization: ");

        executeInTransaction(connection -> {
            int id = insertPersonJdbc(connection, "EMPLOYEE", email, phone);
            insertEmployeeJdbc(connection, id, employeeCode, "FINANCIAL_ADVISOR", firstName, lastName, salary, branch);
            updateJdbc(connection, """
                    INSERT INTO financial_advisors (employee_id, specialization)
                    VALUES (?, ?)
                    """, id, specialization);
        });

        System.out.println("Financial advisor saved in database.");
    }

    private static void openAccountFromMenuJdbc(String accountType) throws SQLException {
        int ownerId = readExistingClientIdByCode("Client code: ");
        int accountId = nextIntJdbc("SELECT COALESCE(MAX(id), 0) + 1 FROM accounts");
        String iban = IBAN.generate().getCode();
        double balance = readNonNegativeDouble("Initial balance: ");
        String currency = readCurrencyCode("Currency (RON/EUR/USD/GBP): ");
        LocalDate openingDate = LocalDate.now();

        executeInTransaction(connection -> {
            updateJdbc(connection, """
                    INSERT INTO accounts (id, iban, account_type, balance, currency, active, opening_date, client_id)
                    VALUES (?, ?, ?, ?, ?, TRUE, ?, ?)
                    """, accountId, iban, accountType, balance, currency, java.sql.Date.valueOf(openingDate), ownerId);

            if ("CURRENT".equals(accountType)) {
                updateJdbc(connection, """
                        INSERT INTO current_accounts (account_id, monthly_fee)
                        VALUES (?, ?)
                        """, accountId, readNonNegativeDouble("Monthly fee: "));
            } else {
                updateJdbc(connection, """
                        INSERT INTO savings_accounts (account_id, interest_rate, withdrawals_this_month)
                        VALUES (?, ?, 0)
                        """, accountId, readNonNegativeDouble("Interest rate: "));
            }
        });

        System.out.println(accountType + " account opened in database.");
        System.out.println("Generated IBAN: " + iban);
    }

    private static void setAliasJdbc(String alias, String iban) throws SQLException {
        int accountId = readExistingAccountIdByIban("IBAN: ", iban);
        updateJdbc("""
                INSERT INTO iban_aliases (alias, account_id)
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE account_id = VALUES(account_id)
                """, alias, accountId);
        System.out.println("Alias saved in database.");
    }

    private static void transferByAliasJdbc(String sourceIban, String alias, double amount) throws SQLException {
        String destinationIban = findStringJdbc("""
                SELECT a.iban
                FROM iban_aliases ia
                JOIN accounts a ON ia.account_id = a.id
                WHERE ia.alias = ?
                """, alias);
        accountService.transferJdbc(sourceIban, destinationIban, amount);
        System.out.println("Transfer by alias completed.");
    }

    private static void issueCardJdbc(String iban, LocalDate expirationDate, boolean contactless) throws SQLException {
        int accountId = readExistingAccountIdByIban("IBAN: ", iban);
        String cardNumber = generateDigits(16);
        String cvv = generateDigits(3);

        updateJdbc("""
                INSERT INTO cards (card_number, cvv, expiration_date, contactless, status, account_id)
                VALUES (?, ?, ?, ?, 'ACTIVE', ?)
                """, cardNumber, cvv, java.sql.Date.valueOf(expirationDate), contactless, accountId);

        System.out.println("Card issued in database: " + cardNumber);
    }

    private static void updateCardStatusJdbc(String cardNumber, String status) throws SQLException {
        updateJdbc("UPDATE cards SET status = ? WHERE card_number = ?", status, cardNumber);
        System.out.println("Card status updated.");
    }

    private static void validateCardJdbc(String cardNumber) throws SQLException {
        String status = findStringJdbc("SELECT status FROM cards WHERE card_number = ?", cardNumber);
        java.sql.Date expirationDate = findDateJdbc("SELECT expiration_date FROM cards WHERE card_number = ?", cardNumber);
        boolean valid = "ACTIVE".equals(status) && expirationDate.toLocalDate().isAfter(LocalDate.now());
        System.out.println(valid ? "Card is valid." : "Card is not valid.");
    }

    private static void issueChequeJdbc() throws SQLException {
        int issuerAccountId = readExistingAccountIdByIban("Issuer IBAN: ");
        int beneficiaryClientId = readExistingClientIdByCode("Beneficiary client code: ");
        double amount = readPositiveDouble("Amount: ");
        LocalDate issueDate = LocalDate.now();
        LocalDate expiryDate = issueDate.plusDays(readPositiveInt("Valid days: "));
        String series = "CEC" + generateDigits(9);

        updateJdbc("""
                INSERT INTO cheques (series, issuer_account_id, beneficiary_client_id, amount, issue_date, expiry_date, status)
                VALUES (?, ?, ?, ?, ?, ?, 'ISSUED')
                """, series, issuerAccountId, beneficiaryClientId, amount, java.sql.Date.valueOf(issueDate), java.sql.Date.valueOf(expiryDate));

        System.out.println("Cheque issued in database: " + series);
    }

    private static void updateChequeStatusJdbc(String series, String status) throws SQLException {
        updateJdbc("UPDATE cheques SET status = ? WHERE series = ?", status, series);
        System.out.println("Cheque status updated.");
    }

    private static void applyForCreditFromMenuJdbc() throws SQLException {
        int borrowerId = readExistingClientIdByCode("Client code: ");
        int targetAccountId = readExistingAccountIdByIban("Target IBAN: ");
        CreditType type = readCreditTypeOption("""
                1. Personal
                2. Mortgage
                3. Business
                Credit type: """);
        double principalAmount = readPositiveDouble("Principal amount: ");
        double annualInterestRate = readNonNegativeDouble("Annual interest rate: ");
        int durationInMonths = readPositiveInt("Duration in months: ");
        double totalAmount = principalAmount + principalAmount * annualInterestRate / 100.0 * durationInMonths / 12.0;

        executeInTransaction(connection -> {
            int creditId = insertCreditRawJdbc(connection, borrowerId, targetAccountId, type.name(), principalAmount, annualInterestRate, durationInMonths, totalAmount);
            insertInstallmentsRawJdbc(connection, creditId, totalAmount, durationInMonths);
        });

        System.out.println("Credit application saved in database.");
    }

    private static void approveCreditJdbc(int creditId) throws SQLException {
        executeInTransaction(connection -> {
            CreditDbData credit = findCreditDbData(connection, creditId);
            if (!"PENDING".equals(credit.status)) {
                throw new SQLException("Only pending credits can be approved.");
            }
            updateJdbc(connection, "UPDATE credits SET status = 'ACTIVE', start_date = ? WHERE id = ?", java.sql.Date.valueOf(LocalDate.now()), creditId);
            updateJdbc(connection, "UPDATE accounts SET balance = balance + ? WHERE id = ?", credit.principalAmount, credit.targetAccountId);
        });
        System.out.println("Credit approved in database.");
    }

    private static void showClientByColumnJdbc(String column, String value) throws SQLException {
        showClientsWhereJdbc("WHERE " + column + " = ?", value, "CLIENT FROM DB");
    }

    private static void showClientsSortedByNameJdbc() throws SQLException {
        showClientsWhereJdbc("ORDER BY display_name", null, "CLIENTS SORTED FROM DB");
    }

    private static void showClientsWhereJdbc(String clause, String parameter, String title) throws SQLException {
        String sql = """
                SELECT
                    c.id,
                    c.client_code,
                    c.client_type,
                    c.active,
                    COALESCE(CONCAT(ic.first_name, ' ', ic.last_name), cc.company_name) AS display_name,
                    p.email,
                    p.phone_number
                FROM clients c
                JOIN persons p ON c.id = p.id
                LEFT JOIN individual_clients ic ON c.id = ic.client_id
                LEFT JOIN corporate_clients cc ON c.id = cc.client_id
                """ + clause;
        printDatabaseQuery("- " + title + " -", sql, resultSet -> String.format(
                "id=%d, code=%s, type=%s, active=%s, name=%s, email=%s, phone=%s",
                resultSet.getInt("id"),
                resultSet.getString("client_code"),
                resultSet.getString("client_type"),
                resultSet.getBoolean("active"),
                resultSet.getString("display_name"),
                resultSet.getString("email"),
                resultSet.getString("phone_number")
        ), parameter == null ? new Object[]{} : new Object[]{parameter});
    }

    private static void showEmployeeByColumnJdbc(String column, String value) throws SQLException {
        printDatabaseQuery("- EMPLOYEE FROM DB -", employeeBaseSql() + " WHERE " + column + " = ?", Main::formatEmployeeRow, value);
    }

    private static void showBankTellersByDeskJdbc(int deskNumber) throws SQLException {
        printDatabaseQuery("- BANK TELLERS FROM DB -", employeeBaseSql() + """
                JOIN bank_tellers bt ON e.id = bt.employee_id
                WHERE bt.desk_number = ?
                """, Main::formatEmployeeRow, deskNumber);
    }

    private static void showFinancialAdvisorsBySpecializationJdbc(String specialization) throws SQLException {
        printDatabaseQuery("- FINANCIAL ADVISORS FROM DB -", employeeBaseSql() + """
                JOIN financial_advisors fa ON e.id = fa.employee_id
                WHERE fa.specialization = ?
                """, Main::formatEmployeeRow, specialization);
    }

    private static void deleteEmployeeJdbc(String employeeCode) throws SQLException {
        updateJdbc("""
                DELETE p FROM persons p
                JOIN employees e ON p.id = e.id
                WHERE e.employee_code = ?
                """, employeeCode);
        System.out.println("Employee deleted from database.");
    }

    private static void removeClientJdbc(String clientCode) throws SQLException {
        updateJdbc("UPDATE clients SET active = FALSE WHERE client_code = ?", clientCode);
        System.out.println("Client deactivated in database.");
    }

    private static void showAccountByIbanJdbc(String iban) throws SQLException {
        printDatabaseQuery("- ACCOUNT FROM DB -", accountBaseSql() + " WHERE a.iban = ?", Main::formatAccountRow, iban);
    }

    private static void showAccountsForClientCodeJdbc(String clientCode) throws SQLException {
        printDatabaseQuery("- ACCOUNTS FOR CLIENT FROM DB -", accountBaseSql() + " WHERE c.client_code = ?", Main::formatAccountRow, clientCode);
    }

    private static void showAliasJdbc(String alias) throws SQLException {
        printDatabaseQuery("- ALIAS FROM DB -", """
                SELECT ia.alias, a.iban
                FROM iban_aliases ia
                JOIN accounts a ON ia.account_id = a.id
                WHERE ia.alias = ?
                """, resultSet -> resultSet.getString("alias") + " -> " + resultSet.getString("iban"), alias);
    }

    private static void showAliasesJdbc() throws SQLException {
        printDatabaseQuery("- ALIASES FROM DB -", """
                SELECT ia.alias, a.iban
                FROM iban_aliases ia
                JOIN accounts a ON ia.account_id = a.id
                ORDER BY ia.alias
                """, resultSet -> resultSet.getString("alias") + " -> " + resultSet.getString("iban"));
    }

    private static void showTransactionsForAccountJdbc(String iban, TransactionType type) throws SQLException {
        String typeFilter = type == null ? "" : " AND t.transaction_type = ?";
        String sql = """
                SELECT DISTINCT t.id, t.transaction_type, t.amount, t.`timestamp`, t.description
                FROM transactions t
                LEFT JOIN deposit_transactions dt ON t.id = dt.transaction_id
                LEFT JOIN withdrawal_transactions wt ON t.id = wt.transaction_id
                LEFT JOIN transfer_transactions tt ON t.id = tt.transaction_id
                LEFT JOIN exchange_transactions et ON t.id = et.transaction_id
                LEFT JOIN accounts da ON dt.destination_account_id = da.id
                LEFT JOIN accounts wa ON wt.source_account_id = wa.id
                LEFT JOIN accounts tsa ON tt.source_account_id = tsa.id
                LEFT JOIN accounts tda ON tt.destination_account_id = tda.id
                LEFT JOIN accounts esa ON et.source_account_id = esa.id
                LEFT JOIN accounts eda ON et.destination_account_id = eda.id
                WHERE (da.iban = ? OR wa.iban = ? OR tsa.iban = ? OR tda.iban = ? OR esa.iban = ? OR eda.iban = ?)
                """ + typeFilter + " ORDER BY t.`timestamp`";
        if (type == null) {
            printDatabaseQuery("- TRANSACTIONS FOR ACCOUNT FROM DB -", sql, Main::formatTransactionRow, iban, iban, iban, iban, iban, iban);
        } else {
            printDatabaseQuery("- TRANSACTIONS FOR ACCOUNT FROM DB -", sql, Main::formatTransactionRow, iban, iban, iban, iban, iban, iban, type.name());
        }
    }

    private static void showCardByNumberJdbc(String cardNumber) throws SQLException {
        printDatabaseQuery("- CARD FROM DB -", "SELECT card_number, expiration_date, contactless, status, account_id FROM cards WHERE card_number = ?", Main::formatCardRow, cardNumber);
    }

    private static void showCardsForAccountJdbc(String iban) throws SQLException {
        printDatabaseQuery("- CARDS FOR ACCOUNT FROM DB -", """
                SELECT c.card_number, c.expiration_date, c.contactless, c.status, c.account_id
                FROM cards c
                JOIN accounts a ON c.account_id = a.id
                WHERE a.iban = ?
                """, Main::formatCardRow, iban);
    }

    private static void showChequeBySeriesJdbc(String series) throws SQLException {
        printDatabaseQuery("- CHEQUE FROM DB -", "SELECT series, issuer_account_id, beneficiary_client_id, amount, issue_date, expiry_date, status FROM cheques WHERE series = ?", Main::formatChequeRow, series);
    }

    private static void showChequesByStatusJdbc(String status) throws SQLException {
        printDatabaseQuery("- CHEQUES BY STATUS FROM DB -", "SELECT series, issuer_account_id, beneficiary_client_id, amount, issue_date, expiry_date, status FROM cheques WHERE status = ?", Main::formatChequeRow, status);
    }

    private static void showAccountFlowTotalJdbc(String iban, boolean incoming) throws SQLException {
        String sql = incoming ? """
                SELECT COALESCE(SUM(amount), 0) AS total FROM (
                    SELECT t.amount FROM transactions t JOIN deposit_transactions dt ON t.id = dt.transaction_id JOIN accounts a ON dt.destination_account_id = a.id WHERE a.iban = ?
                    UNION ALL
                    SELECT t.amount FROM transactions t JOIN transfer_transactions tt ON t.id = tt.transaction_id JOIN accounts a ON tt.destination_account_id = a.id WHERE a.iban = ?
                ) x
                """ : """
                SELECT COALESCE(SUM(amount), 0) AS total FROM (
                    SELECT t.amount FROM transactions t JOIN withdrawal_transactions wt ON t.id = wt.transaction_id JOIN accounts a ON wt.source_account_id = a.id WHERE a.iban = ?
                    UNION ALL
                    SELECT t.amount FROM transactions t JOIN transfer_transactions tt ON t.id = tt.transaction_id JOIN accounts a ON tt.source_account_id = a.id WHERE a.iban = ?
                ) x
                """;
        System.out.println((incoming ? "Total inflows: " : "Total outflows: ") + findDoubleJdbc(sql, iban, iban));
    }

    private static void showMonthlyStatementJdbc(String iban, YearMonth month) throws SQLException {
        printDatabaseQuery("- MONTHLY STATEMENT FROM DB -", """
                SELECT t.id, t.transaction_type, t.amount, t.`timestamp`, t.description
                FROM transactions t
                JOIN transfer_transactions tt ON t.id = tt.transaction_id
                JOIN accounts sa ON tt.source_account_id = sa.id
                JOIN accounts da ON tt.destination_account_id = da.id
                WHERE (sa.iban = ? OR da.iban = ?)
                  AND t.`timestamp` >= ?
                  AND t.`timestamp` < ?
                ORDER BY t.`timestamp`
                """, Main::formatTransactionRow, iban, iban, Timestamp.valueOf(month.atDay(1).atStartOfDay()), Timestamp.valueOf(month.plusMonths(1).atDay(1).atStartOfDay()));
    }

    private static void showMonthlyFlowJdbc(String iban, boolean incoming) throws SQLException {
        String accountColumn = incoming ? "tt.destination_account_id" : "tt.source_account_id";
        printDatabaseQuery(incoming ? "- MONTHLY INCOMING FROM DB -" : "- MONTHLY OUTGOING FROM DB -", """
                SELECT DATE_FORMAT(t.`timestamp`, '%Y-%m') AS month, SUM(t.amount) AS total
                FROM transactions t
                JOIN transfer_transactions tt ON t.id = tt.transaction_id
                JOIN accounts a ON """ + accountColumn + " = a.id " + """
                WHERE a.iban = ?
                GROUP BY DATE_FORMAT(t.`timestamp`, '%Y-%m')
                ORDER BY month
                """, resultSet -> resultSet.getString("month") + " -> " + resultSet.getDouble("total"), iban);
    }

    private static void showTransactionsGroupedByTypeJdbc() throws SQLException {
        printDatabaseQuery("- TRANSACTIONS GROUPED BY TYPE FROM DB -", """
                SELECT transaction_type, COUNT(*) AS transaction_count, SUM(amount) AS total_amount
                FROM transactions
                GROUP BY transaction_type
                ORDER BY transaction_type
                """, resultSet -> resultSet.getString("transaction_type") + " -> count=" + resultSet.getInt("transaction_count") + ", total=" + resultSet.getDouble("total_amount"));
    }

    private static void showAccountsGroupedByCurrencyJdbc() throws SQLException {
        printDatabaseQuery("- ACCOUNTS GROUPED BY CURRENCY FROM DB -", """
                SELECT currency, COUNT(*) AS account_count, SUM(balance) AS total_balance
                FROM accounts
                GROUP BY currency
                ORDER BY currency
                """, resultSet -> resultSet.getString("currency") + " -> count=" + resultSet.getInt("account_count") + ", totalBalance=" + resultSet.getDouble("total_balance"));
    }

    private static void showCreditsGroupedByStatusJdbc() throws SQLException {
        printDatabaseQuery("- CREDITS GROUPED BY STATUS FROM DB -", """
                SELECT status, COUNT(*) AS credit_count, SUM(remaining_amount) AS total_remaining
                FROM credits
                GROUP BY status
                ORDER BY status
                """, resultSet -> resultSet.getString("status") + " -> count=" + resultSet.getInt("credit_count") + ", remaining=" + resultSet.getDouble("total_remaining"));
    }

    private static void showCreditByIdJdbc(int creditId) throws SQLException {
        printDatabaseQuery("- CREDIT FROM DB -", "SELECT id, borrower_id, target_account_id, credit_type, principal_amount, remaining_amount, status FROM credits WHERE id = ?", Main::formatCreditRow, creditId);
    }

    private static void showCreditsForClientJdbc(String clientCode) throws SQLException {
        printDatabaseQuery("- CREDITS FOR CLIENT FROM DB -", """
                SELECT cr.id, cr.borrower_id, cr.target_account_id, cr.credit_type, cr.principal_amount, cr.remaining_amount, cr.status
                FROM credits cr
                JOIN clients c ON cr.borrower_id = c.id
                WHERE c.client_code = ?
                """, Main::formatCreditRow, clientCode);
    }

    private static void showCreditsByStatusJdbc(String status) throws SQLException {
        printDatabaseQuery("- CREDITS BY STATUS FROM DB -", "SELECT id, borrower_id, target_account_id, credit_type, principal_amount, remaining_amount, status FROM credits WHERE status = ?", Main::formatCreditRow, status);
    }

    private static void showAllData() {
        if (databaseMode) {
            auditService.logAction("show_all_database_data");
            showAllDatabaseData();
            return;
        }

        System.out.println("\n- CLIENTS -");
        clientService.getAllClients().forEach(System.out::println);

        System.out.println("\n- EMPLOYEES -");
        employeeService.getAllEmployees().forEach(System.out::println);

        System.out.println("\n- ACCOUNTS -");
        accountService.getAllAccounts().forEach(System.out::println);

        System.out.println("\n- CARDS -");
        cardService.getAllCards().forEach(System.out::println);

        System.out.println("\n- CHEQUES -");
        chequeService.getAllCheques().forEach(System.out::println);

        System.out.println("\n- CREDITS -");
        creditService.getAllCredits().forEach(System.out::println);

        System.out.println("\n- TRANSACTIONS -");
        transactionService.getAllTransactions().forEach(System.out::println);
    }

    private static void showAllDatabaseData() {
        databaseViewService.showAllData();
    }

    private static void showDatabaseClients() throws SQLException {
        String sql = """
                SELECT
                    c.id,
                    c.client_code,
                    c.client_type,
                    c.active,
                    COALESCE(CONCAT(ic.first_name, ' ', ic.last_name), cc.company_name) AS display_name,
                    p.email,
                    p.phone_number
                FROM clients c
                JOIN persons p ON c.id = p.id
                LEFT JOIN individual_clients ic ON c.id = ic.client_id
                LEFT JOIN corporate_clients cc ON c.id = cc.client_id
                ORDER BY c.id
                """;

        printDatabaseQuery("- CLIENTS FROM DB -", sql, resultSet -> String.format(
                "id=%d, code=%s, type=%s, active=%s, name=%s, email=%s, phone=%s",
                resultSet.getInt("id"),
                resultSet.getString("client_code"),
                resultSet.getString("client_type"),
                resultSet.getBoolean("active"),
                resultSet.getString("display_name"),
                resultSet.getString("email"),
                resultSet.getString("phone_number")
        ));
    }

    private static void showDatabaseEmployees() throws SQLException {
        String sql = """
                SELECT
                    e.id,
                    e.employee_code,
                    e.employee_type,
                    e.first_name,
                    e.last_name,
                    e.salary,
                    e.branch,
                    p.email
                FROM employees e
                JOIN persons p ON e.id = p.id
                ORDER BY e.id
                """;

        printDatabaseQuery("- EMPLOYEES FROM DB -", sql, resultSet -> String.format(
                "id=%d, code=%s, type=%s, name=%s %s, salary=%.2f, branch=%s, email=%s",
                resultSet.getInt("id"),
                resultSet.getString("employee_code"),
                resultSet.getString("employee_type"),
                resultSet.getString("first_name"),
                resultSet.getString("last_name"),
                resultSet.getDouble("salary"),
                resultSet.getString("branch"),
                resultSet.getString("email")
        ));
    }

    private static void showDatabaseAccounts() throws SQLException {
        String sql = """
                SELECT
                    a.id,
                    a.iban,
                    a.account_type,
                    a.balance,
                    a.currency,
                    a.active,
                    a.client_id,
                    COALESCE(CONCAT(ic.first_name, ' ', ic.last_name), cc.company_name) AS owner_name
                FROM accounts a
                JOIN clients c ON a.client_id = c.id
                LEFT JOIN individual_clients ic ON c.id = ic.client_id
                LEFT JOIN corporate_clients cc ON c.id = cc.client_id
                ORDER BY a.id
                """;

        printDatabaseQuery("- ACCOUNTS FROM DB -", sql, resultSet -> String.format(
                "id=%d, iban=%s, type=%s, balance=%.2f %s, active=%s, owner=%s(%d)",
                resultSet.getInt("id"),
                resultSet.getString("iban"),
                resultSet.getString("account_type"),
                resultSet.getDouble("balance"),
                resultSet.getString("currency"),
                resultSet.getBoolean("active"),
                resultSet.getString("owner_name"),
                resultSet.getInt("client_id")
        ));
    }

    private static void showDatabaseCards() throws SQLException {
        String sql = """
                SELECT card_number, expiration_date, contactless, status, account_id
                FROM cards
                ORDER BY card_number
                """;

        printDatabaseQuery("- CARDS FROM DB -", sql, resultSet -> String.format(
                "card=%s, expires=%s, contactless=%s, status=%s, account_id=%d",
                resultSet.getString("card_number"),
                resultSet.getDate("expiration_date"),
                resultSet.getBoolean("contactless"),
                resultSet.getString("status"),
                resultSet.getInt("account_id")
        ));
    }

    private static void showDatabaseCheques() throws SQLException {
        String sql = """
                SELECT series, issuer_account_id, beneficiary_client_id, amount, issue_date, expiry_date, status
                FROM cheques
                ORDER BY series
                """;

        printDatabaseQuery("- CHEQUES FROM DB -", sql, resultSet -> String.format(
                "series=%s, issuer_account_id=%d, beneficiary_client_id=%d, amount=%.2f, issue=%s, expiry=%s, status=%s",
                resultSet.getString("series"),
                resultSet.getInt("issuer_account_id"),
                resultSet.getInt("beneficiary_client_id"),
                resultSet.getDouble("amount"),
                resultSet.getDate("issue_date"),
                resultSet.getDate("expiry_date"),
                resultSet.getString("status")
        ));
    }

    private static void showDatabaseCredits() throws SQLException {
        String sql = """
                SELECT id, borrower_id, target_account_id, credit_type, principal_amount, remaining_amount, status
                FROM credits
                ORDER BY id
                """;

        printDatabaseQuery("- CREDITS FROM DB -", sql, resultSet -> String.format(
                "id=%d, borrower_id=%d, target_account_id=%d, type=%s, principal=%.2f, remaining=%.2f, status=%s",
                resultSet.getInt("id"),
                resultSet.getInt("borrower_id"),
                resultSet.getInt("target_account_id"),
                resultSet.getString("credit_type"),
                resultSet.getDouble("principal_amount"),
                resultSet.getDouble("remaining_amount"),
                resultSet.getString("status")
        ));
    }

    private static void showDatabaseTransactions() throws SQLException {
        String sql = """
                SELECT id, transaction_type, amount, `timestamp`, description
                FROM transactions
                ORDER BY `timestamp`
                """;

        printDatabaseQuery("- TRANSACTIONS FROM DB -", sql, resultSet -> String.format(
                "id=%d, type=%s, amount=%.2f, timestamp=%s, description=%s",
                resultSet.getInt("id"),
                resultSet.getString("transaction_type"),
                resultSet.getDouble("amount"),
                resultSet.getTimestamp("timestamp"),
                resultSet.getString("description")
        ));
    }

    private static String employeeBaseSql() {
        return """
                SELECT
                    e.id,
                    e.employee_code,
                    e.employee_type,
                    e.first_name,
                    e.last_name,
                    e.salary,
                    e.branch,
                    p.email
                FROM employees e
                JOIN persons p ON e.id = p.id
                """;
    }

    private static String accountBaseSql() {
        return """
                SELECT
                    a.id,
                    a.iban,
                    a.account_type,
                    a.balance,
                    a.currency,
                    a.active,
                    a.client_id,
                    COALESCE(CONCAT(ic.first_name, ' ', ic.last_name), cc.company_name) AS owner_name
                FROM accounts a
                JOIN clients c ON a.client_id = c.id
                LEFT JOIN individual_clients ic ON c.id = ic.client_id
                LEFT JOIN corporate_clients cc ON c.id = cc.client_id
                """;
    }

    private static String formatEmployeeRow(ResultSet resultSet) throws SQLException {
        return String.format(
                "id=%d, code=%s, type=%s, name=%s %s, salary=%.2f, branch=%s, email=%s",
                resultSet.getInt("id"),
                resultSet.getString("employee_code"),
                resultSet.getString("employee_type"),
                resultSet.getString("first_name"),
                resultSet.getString("last_name"),
                resultSet.getDouble("salary"),
                resultSet.getString("branch"),
                resultSet.getString("email")
        );
    }

    private static String formatAccountRow(ResultSet resultSet) throws SQLException {
        return String.format(
                "id=%d, iban=%s, type=%s, balance=%.2f %s, active=%s, owner=%s(%d)",
                resultSet.getInt("id"),
                resultSet.getString("iban"),
                resultSet.getString("account_type"),
                resultSet.getDouble("balance"),
                resultSet.getString("currency"),
                resultSet.getBoolean("active"),
                resultSet.getString("owner_name"),
                resultSet.getInt("client_id")
        );
    }

    private static String formatCardRow(ResultSet resultSet) throws SQLException {
        return String.format(
                "card=%s, expires=%s, contactless=%s, status=%s, account_id=%d",
                resultSet.getString("card_number"),
                resultSet.getDate("expiration_date"),
                resultSet.getBoolean("contactless"),
                resultSet.getString("status"),
                resultSet.getInt("account_id")
        );
    }

    private static String formatChequeRow(ResultSet resultSet) throws SQLException {
        return String.format(
                "series=%s, issuer_account_id=%d, beneficiary_client_id=%d, amount=%.2f, issue=%s, expiry=%s, status=%s",
                resultSet.getString("series"),
                resultSet.getInt("issuer_account_id"),
                resultSet.getInt("beneficiary_client_id"),
                resultSet.getDouble("amount"),
                resultSet.getDate("issue_date"),
                resultSet.getDate("expiry_date"),
                resultSet.getString("status")
        );
    }

    private static String formatTransactionRow(ResultSet resultSet) throws SQLException {
        return String.format(
                "id=%d, type=%s, amount=%.2f, timestamp=%s, description=%s",
                resultSet.getInt("id"),
                resultSet.getString("transaction_type"),
                resultSet.getDouble("amount"),
                resultSet.getTimestamp("timestamp"),
                resultSet.getString("description")
        );
    }

    private static String formatCreditRow(ResultSet resultSet) throws SQLException {
        return String.format(
                "id=%d, borrower_id=%d, target_account_id=%d, type=%s, principal=%.2f, remaining=%.2f, status=%s",
                resultSet.getInt("id"),
                resultSet.getInt("borrower_id"),
                resultSet.getInt("target_account_id"),
                resultSet.getString("credit_type"),
                resultSet.getDouble("principal_amount"),
                resultSet.getDouble("remaining_amount"),
                resultSet.getString("status")
        );
    }

    private static void printDatabaseQuery(String title, String sql, ResultSetFormatter formatter, Object... parameters) throws SQLException {
        auditService.logAction("database_query_" + title.toLowerCase()
                .replace("-", "")
                .replace("from db", "")
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_|_$", ""));
        System.out.println("\n" + title);

        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
        ) {
            bindParameters(statement, parameters);

            try (ResultSet resultSet = statement.executeQuery()) {
            boolean found = false;

            while (resultSet.next()) {
                found = true;
                System.out.println(formatter.format(resultSet));
            }

            if (!found) {
                System.out.println("(no rows)");
            }
            }
        }
    }

    private static void updateJdbc(String sql, Object... parameters) throws SQLException {
        try (Connection connection = DatabaseConnection.getInstance().getConnection()) {
            updateJdbc(connection, sql, parameters);
        }
    }

    private static void updateJdbc(Connection connection, String sql, Object... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParameters(statement, parameters);
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                System.out.println("No database rows were changed.");
            }
        }
    }

    private static void executeInTransaction(SqlWork work) throws SQLException {
        Connection connection = DatabaseConnection.getInstance().getConnection();
        try {
            connection.setAutoCommit(false);
            work.execute(connection);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private static int insertPersonJdbc(Connection connection, String personType, String email, String phone) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO persons (person_type, email, phone_number)
                VALUES (?, ?, ?)
                """, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, personType);
            statement.setString(2, email);
            statement.setString(3, phone);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }

        throw new SQLException("Could not generate person id.");
    }

    private static void insertClientJdbc(Connection connection, int id, String clientCode, String clientType, boolean active) throws SQLException {
        updateJdbc(connection, """
                INSERT INTO clients (id, client_code, client_type, active)
                VALUES (?, ?, ?, ?)
                """, id, clientCode, clientType, active);
    }

    private static void insertEmployeeJdbc(Connection connection, int id, String code, String type, String firstName, String lastName, double salary, String branch) throws SQLException {
        updateJdbc(connection, """
                INSERT INTO employees (id, employee_code, employee_type, first_name, last_name, salary, branch)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, id, code, type, firstName, lastName, salary, branch);
    }

    private static int insertCreditRawJdbc(Connection connection, int borrowerId, int targetAccountId, String type, double principal, double rate, int duration, double totalAmount) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO credits (
                    borrower_id, target_account_id, credit_type, principal_amount,
                    annual_interest_rate, duration_in_months, start_date, remaining_amount, status
                )
                VALUES (?, ?, ?, ?, ?, ?, NULL, ?, 'PENDING')
                """, PreparedStatement.RETURN_GENERATED_KEYS)) {
            bindParameters(statement, borrowerId, targetAccountId, type, principal, rate, duration, totalAmount);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }

        throw new SQLException("Could not create credit.");
    }

    private static void insertInstallmentsRawJdbc(Connection connection, int creditId, double totalAmount, int duration) throws SQLException {
        double monthlyAmount = Math.round((totalAmount / duration) * 100.0) / 100.0;
        double assigned = 0;
        for (int i = 1; i <= duration; i++) {
            double amount = i == duration ? Math.round((totalAmount - assigned) * 100.0) / 100.0 : monthlyAmount;
            assigned += amount;
            updateJdbc(connection, """
                    INSERT INTO credit_installments (credit_id, installment_number, due_date, amount, paid)
                    VALUES (?, ?, ?, ?, FALSE)
                    """, creditId, i, java.sql.Date.valueOf(LocalDate.now().plusMonths(i)), amount);
        }
    }

    private static CreditDbData findCreditDbData(Connection connection, int creditId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id, target_account_id, principal_amount, status
                FROM credits
                WHERE id = ?
                """)) {
            statement.setInt(1, creditId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new CreditDbData(
                            resultSet.getInt("id"),
                            resultSet.getInt("target_account_id"),
                            resultSet.getDouble("principal_amount"),
                            resultSet.getString("status")
                    );
                }
            }
        }

        throw new SQLException("Credit not found.");
    }

    private static int findClientIdByCodeJdbc(String clientCode) throws SQLException {
        return findIntJdbc("SELECT id FROM clients WHERE client_code = ?", clientCode);
    }

    private static int findAccountIdByIbanJdbc(String iban) throws SQLException {
        return findIntJdbc("SELECT id FROM accounts WHERE iban = ?", iban);
    }

    private static int findIntJdbc(String sql, Object... parameters) throws SQLException {
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParameters(statement, parameters);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        }

        throw new SQLException("Database value not found.");
    }

    private static int nextIntJdbc(String sql) throws SQLException {
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        }

        throw new SQLException("Could not generate next id.");
    }

    private static double findDoubleJdbc(String sql, Object... parameters) throws SQLException {
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParameters(statement, parameters);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getDouble(1);
                }
            }
        }

        return 0;
    }

    private static String findStringJdbc(String sql, Object... parameters) throws SQLException {
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParameters(statement, parameters);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString(1);
                }
            }
        }

        throw new SQLException("Database value not found.");
    }

    private static java.sql.Date findDateJdbc(String sql, Object... parameters) throws SQLException {
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParameters(statement, parameters);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getDate(1);
                }
            }
        }

        throw new SQLException("Database value not found.");
    }

    private static void bindParameters(PreparedStatement statement, Object... parameters) throws SQLException {
        for (int i = 0; i < parameters.length; i++) {
            statement.setObject(i + 1, parameters[i]);
        }
    }

    private static LocalDate parseBirthDateFromCnp(String cnp) {
        if (cnp == null || !cnp.matches("[1-8]\\d{12}")) {
            throw new IllegalArgumentException("CNP invalid: must contain exactly 13 digits and start with 1-8.");
        }

        int century = switch (cnp.charAt(0)) {
            case '1', '2' -> 1900;
            case '3', '4' -> 1800;
            case '5', '6' -> 2000;
            case '7', '8' -> 2000;
            default -> throw new IllegalArgumentException("CNP invalid: first digit is not valid.");
        };

        try {
            int year = century + Integer.parseInt(cnp.substring(1, 3));
            int month = Integer.parseInt(cnp.substring(3, 5));
            int day = Integer.parseInt(cnp.substring(5, 7));
            return LocalDate.of(year, month, day);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("CNP invalid: birth date encoded in CNP is not valid.");
        }
    }

    private static String generateDigits(int length) {
        Random random = new Random();
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < length; i++) {
            value.append(random.nextInt(10));
        }
        return value.toString();
    }

    @FunctionalInterface
    private interface ResultSetFormatter {
        String format(ResultSet resultSet) throws SQLException;
    }

    @FunctionalInterface
    private interface SqlWork {
        void execute(Connection connection) throws SQLException;
    }

    private record CreditDbData(int id, int targetAccountId, double principalAmount, String status) {
    }

    private static void printHeader(String title) {
        System.out.println("-------------------------");
        System.out.println(title);
        System.out.println("-------------------------");
    }

    private static int readInt(String message) {
        while (true) {
            System.out.print(message);
            if (!scanner.hasNextLine()) {
                return 0;
            }

            String value = scanner.nextLine().trim();
            if (value.isEmpty()) {
                message = "Enter a valid integer: ";
                continue;
            }

            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                message = "Enter a valid integer: ";
            }
        }
    }

    private static double readDouble(String message) {
        while (true) {
            System.out.print(message);
            if (!scanner.hasNextLine()) {
                return 0.0;
            }

            String value = scanner.nextLine().trim();
            if (value.isEmpty()) {
                message = "Enter a valid number: ";
                continue;
            }

            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                message = "Enter a valid number: ";
            }
        }
    }

    private static String readLine(String message) {
        System.out.print(message);
        if (!scanner.hasNextLine()) {
            return "";
        }
        return scanner.nextLine();
    }
}
