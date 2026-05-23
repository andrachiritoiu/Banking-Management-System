package com.pao.project.bank;

import com.pao.project.bank.model.AccountStatement;
import com.pao.project.bank.model.Card;
import com.pao.project.bank.model.Cheque;
import com.pao.project.bank.model.IBAN;
import com.pao.project.bank.model.account.Account;
import com.pao.project.bank.model.account.CurrentAccount;
import com.pao.project.bank.model.account.SavingsAccount;
import com.pao.project.bank.model.enums.ChequeStatus;
import com.pao.project.bank.model.enums.TransactionType;
import com.pao.project.bank.model.person.BankTeller;
import com.pao.project.bank.model.person.Client;
import com.pao.project.bank.model.person.CorporateClient;
import com.pao.project.bank.model.person.FinancialAdvisor;
import com.pao.project.bank.model.person.IndividualClient;
import com.pao.project.bank.service.AccountService;
import com.pao.project.bank.service.CardService;
import com.pao.project.bank.service.ChequeService;
import com.pao.project.bank.service.ClientService;
import com.pao.project.bank.service.EmployeeService;
import com.pao.project.bank.service.ReportService;
import com.pao.project.bank.service.TransactionService;

import java.time.LocalDate;
import java.util.Scanner;

public class Main {
    private static final Scanner scanner = new Scanner(System.in);

    private static final ClientService clientService = ClientService.getInstance();
    private static final EmployeeService employeeService = EmployeeService.getInstance();
    private static final AccountService accountService = AccountService.getInstance();
    private static final CardService cardService = CardService.getInstance();
    private static final TransactionService transactionService = TransactionService.getInstance();
    private static final ChequeService chequeService = ChequeService.getInstance();
    private static final ReportService reportService = ReportService.getInstance();

    public static void main(String[] args) {
        printHeader("BANKING MANAGEMENT SYSTEM");
        seedDemoData();
        System.out.println("Demo data loaded successfully.");
        runMenu();
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
            System.out.println("""
                    
                    ---- MAIN MENU ----
                    1. Clients
                    2. Employees
                    3. Accounts & Transactions
                    4. Cards
                    5. Cheques
                    6. Reports
                    7. View All Data
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
                    case 7 -> showAllData();
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
                    11. Show all transactions for account
                    12. Show transactions for account by type
                    13. Show transactions for account sorted by date
                    0. Back
                    """);

            int op = readInt("Choose: ");

            try {
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
                        accountService.deposit(readLine("IBAN: "), readDouble("Amount: "));
                        System.out.println("Deposit completed.");
                    }

                    case 8 -> {
                        accountService.withdraw(readLine("IBAN: "), readDouble("Amount: "));
                        System.out.println("Withdrawal completed.");
                    }

                    case 9 -> {
                        accountService.transfer(
                                readLine("Source IBAN: "),
                                readLine("Destination IBAN: "),
                                readDouble("Amount: ")
                        );
                        System.out.println("Transfer completed.");
                    }

                    case 10 -> {
                        accountService.exchange(
                                readLine("Source IBAN: "),
                                readLine("Destination IBAN: "),
                                readDouble("Source amount: "),
                                readDouble("Exchange rate: ")
                        );
                        System.out.println("Exchange completed.");
                    }

                    case 11 -> transactionService.getTransactionsForAccount(readLine("IBAN: ")).forEach(System.out::println);

                    case 12 -> {
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

                    case 13 -> transactionService.getTransactionsSortedByDate(readLine("IBAN: ")).forEach(System.out::println);

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
                    0. Back
                    """);

            int op = readInt("Choose: ");

            try {
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

                    case 0 -> back = true;
                    default -> System.out.println("Invalid option.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void showAllData() {
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

        System.out.println("\n- TRANSACTIONS -");
        transactionService.getAllTransactions().forEach(System.out::println);
    }

    private static void printHeader(String title) {
        System.out.println("-------------------------");
        System.out.println(title);
        System.out.println("-------------------------");
    }

    private static int readInt(String message) {
        System.out.print(message);
        while (!scanner.hasNextInt()) {
            System.out.print("Enter a valid integer: ");
            scanner.next();
        }
        int value = scanner.nextInt();
        scanner.nextLine();
        return value;
    }

    private static double readDouble(String message) {
        System.out.print(message);
        while (!scanner.hasNextDouble()) {
            System.out.print("Enter a valid number: ");
            scanner.next();
        }
        double value = scanner.nextDouble();
        scanner.nextLine();
        return value;
    }

    private static String readLine(String message) {
        System.out.print(message);
        return scanner.nextLine();
    }
}
