<div align="center">

# Banking Management System
<p>
  <i>
    A full-stack Java banking simulation built from scratch — clean domain modeling, 
    service-oriented architecture,<br/>
    raw JDBC persistence, and real-world business rules. No frameworks. No shortcuts.
  </i>
</p>

<br/>

**[Project Overview](#project-overview)** · **[Features](#features)** · **[Architecture](#architecture)** · **[Domain Model](#domain-model)** · **[Core Functionalities](#core-functionalities)** · **[Services Layer](#services-layer)** · **[Collections Usage](#collections-usage)** · **[Exception Handling](#exception-handling)** · **[Persistence (Stage II)](#persistence-stage-ii)** · **[Technologies](#technologies)** · **[Concepts Demonstrated](#concepts-demonstrated)** · **[Conclusion](#conclusion)**

<br/>

</div>

> **Romanian version available:** [README.ro.md](README.ro.md)
---

## Project Overview

The **Banking Management System** is a Java-based application that models a realistic banking environment using advanced Object-Oriented Programming principles.

The system manages:

- individual and corporate clients  
- bank employees  
- bank accounts (current and savings)  
- cards  
- transactions (deposits, withdrawals, transfers)  
- payment instruments (cheques)  
- account statements and reports  

The project was developed across **two progressive stages**:

| Stage | Scope | Key Deliverable |
|:---:|---|---|
| **I** | Full OOP modeling with in-memory data | Clean domain model, service layer, custom exceptions, collections |
| **II** | Persistent storage and audit trail | JDBC with explicit transactions, PreparedStatements, CSV audit logging |

The application is designed with a layered architecture and focuses on clean domain modeling, extensibility, and realistic business rules.

---

## Features

### Client Management
- Support for individual and corporate clients
- Unique identification and validation logic
- Corporate clients include a legal representative

### Account Management
- Current accounts and savings accounts
- Account lifecycle management (open, close, validate)
- Business rules enforcement (e.g., withdrawal limits)

### Transactions
- Deposit, withdrawal, and transfer operations
- Validation of funds and account state
- Transaction history tracking

### Card System
- Card issuance and association with accounts
- Card blocking and status management

### Reporting
- Account statement generation
- Aggregated financial data (inflows, outflows, balances)

### Additional Features
- Cheque (CEC) support as a payment instrument
- Custom business rules for banking operations

---

## Architecture

### Project Structure

The structure below reflects the files currently present in the repository, including the Stage II JDBC repositories, services, SQL schema, DB configuration, and audit file.

```text
Banking-Management-System/
|-- README.md
|-- README.ro.md
|-- docker-compose.yml
|-- audit.csv
|-- libs/
|   `-- mysql-connector-j-9.7.0.jar
|-- resources/
|   |-- db.properties
|   `-- schema.sql
`-- src/
    `-- com/pao/project/bank/
        |-- Main.java
        |-- exception/
        |   |-- AccountClosedException.java
        |   |-- CardBlockedException.java
        |   |-- ChequeExpiredException.java
        |   |-- CreditNotApprovedException.java
        |   |-- InsufficientFundsException.java
        |   |-- InvalidOperationException.java
        |   `-- WithdrawalLimitExceededException.java
        |-- model/
        |   |-- AccountStatement.java
        |   |-- Card.java
        |   |-- Cheque.java
        |   |-- Credit.java
        |   |-- IBAN.java
        |   |-- account/
        |   |   |-- Account.java
        |   |   |-- AccountFactory.java
        |   |   |-- CurrentAccount.java
        |   |   |-- SavingsAccount.java
        |   |   `-- Transactable.java
        |   |-- enums/
        |   |   |-- AccountType.java
        |   |   |-- CardStatus.java
        |   |   |-- ChequeStatus.java
        |   |   |-- CreditStatus.java
        |   |   |-- CreditType.java
        |   |   |-- Currency.java
        |   |   `-- TransactionType.java
        |   |-- person/
        |   |   |-- BankEmployee.java
        |   |   |-- BankTeller.java
        |   |   |-- Client.java
        |   |   |-- CorporateClient.java
        |   |   |-- FinancialAdvisor.java
        |   |   |-- IndividualClient.java
        |   |   `-- Person.java
        |   `-- transaction/
        |       |-- Deposit.java
        |       |-- Exchange.java
        |       |-- Transaction.java
        |       |-- Transfer.java
        |       `-- Withdrawal.java
        |-- repository/
        |   |-- Repository.java
        |   |-- AccountStatementRepository.java
        |   |-- CardRepository.java
        |   |-- ChequeRepository.java
        |   |-- CreditRepository.java
        |   |-- TransactionRepository.java
        |   |-- account/
        |   |   |-- CurrentAccountRepository.java
        |   |   |-- IbanAliasRepository.java
        |   |   `-- SavingsAccountRepository.java
        |   |-- helper/
        |   |   |-- AccountRepositoryHelper.java
        |   |   |-- ClientRepositoryHelper.java
        |   |   |-- EmployeeRepositoryHelper.java
        |   |   |-- PersonRepositoryHelper.java
        |   |   `-- TransactionRepositoryHelper.java
        |   |-- person/
        |   |   |-- BankTellerRepository.java
        |   |   |-- CorporateClientRepository.java
        |   |   |-- FinancialAdvisorRepository.java
        |   |   `-- IndividualClientRepository.java
        |   `-- transaction/
        |       |-- DepositRepository.java
        |       |-- ExchangeRepository.java
        |       |-- TransferRepository.java
        |       `-- WithdrawalRepository.java
        |-- service/
        |   |-- AccountService.java
        |   |-- AuditService.java
        |   |-- CardService.java
        |   |-- ChequeService.java
        |   |-- ClientService.java
        |   |-- CreditService.java
        |   |-- DatabaseViewService.java
        |   |-- EmployeeService.java
        |   |-- ReportService.java
        |   `-- TransactionService.java
        `-- util/
            |-- DatabaseConnection.java
            `-- DatabaseSeeder.java
```

The project follows a layered architecture:

- **Model Layer** — defines the core domain entities and relationships  
- **Service Layer** — contains business logic and application workflows  
- **Repository Layer (Stage II)** — handles database interaction using JDBC  

This structure ensures separation of concerns, maintainability, and scalability.





## Domain Model

### 1. Person Hierarchy

```text
Person (abstract)
├── Client (abstract)
│   ├── IndividualClient
│   └── CorporateClient
└── BankEmployee (abstract)
    ├── BankTeller
    └── FinancialAdvisor
```

- Separation between clients and employees  
- Corporate clients use composition (legal representative)  
- Multi-level inheritance structure  

---

### 2. Account Hierarchy

```text
Account (abstract)
├── CurrentAccount
└── SavingsAccount
```

- Common account behavior defined in base class  
- Savings accounts include interest logic  
- Business constraints enforced at subclass level  

---

### 3. Transaction Hierarchy

```text
Transaction (abstract)
├── Deposit
├── Withdrawal
└── Transfer
```


- Unified transaction model  
- Specialized subclasses for each operation type  
- Supports history tracking and reporting  

---

### Additional Core Classes

#### IBAN (Immutable)
- Unique identifier for accounts  
- Fully immutable value object  
- Validation logic included  

#### Card
- Linked to a bank account  
- Supports status transitions (active, blocked, expired)  

#### AccountStatement
- Generated from transaction history  
- Contains aggregated financial data  

#### Cheque
- Alternative payment instrument  
- Includes lifecycle (issued, cashed, cancelled, expired)  

---

### Enumerations

```java
enum AccountType     { CURRENT, SAVINGS }
enum TransactionType { DEPOSIT, WITHDRAWAL, TRANSFER }
enum CardStatus      { ACTIVE, BLOCKED, EXPIRED }
enum ChequeStatus    { ISSUED, CASHED, CANCELLED, EXPIRED }
```

---

## Core Functionalities

The system supports a comprehensive set of banking operations, grouped by domain:

### Client Management
- Add individual client  
- Add corporate client  
- Remove client  
- Search client by identifier (CNP / CUI)  
- List all clients  

### Employee Management
- Add bank employee  
- List employees  
- Assign employees to operations (e.g., account creation, client interaction)  

### Account Management
- Open current account  
- Open savings account  
- Close account (with validation: non-zero balance not allowed)  
- Search account by IBAN  
- List all accounts  
- View accounts per client  

### Transaction Operations
- Deposit funds into an account  
- Withdraw funds with validation (insufficient funds check)  
- Transfer funds between two accounts  
- Prevent invalid transfers (e.g., same source and destination account)  
- Record and store all transactions  

### Card Management
- Issue card linked to an account  
- Block card  
- Unblock card  
- Validate card status before operations  

### Reporting and Analytics
- Generate account statement (AccountStatement)  
- View transaction history for an account  
- Calculate total inflows and outflows  
- Display current and final balances  
- Sort transactions by date or amount  

### Cheque (CEC) Operations
- Issue cheque  
- Validate cheque status  
- Cash cheque (with account validation)  
- Cancel cheque  
- Track cheque lifecycle (issued, cashed, cancelled, expired)  

### Business Rules Enforcement
- Prevent withdrawal if insufficient funds  
- Enforce withdrawal limits for savings accounts  
- Restrict operations on closed accounts  
- Restrict operations on blocked cards  
- Validate all transaction amounts  

All functionalities are demonstrated in the `com.pao.project.bank.Main` class through a complete execution scenario.

---

## Services Layer

The application uses the **Singleton pattern** for service classes.

### com.pao.project.bank.Main Services

- `ClientService`: add, remove, search, list clients  

- `AccountService`: open account, deposit, withdraw, transfer  

- `CardService`: issue and block cards  

- `TransactionService`: manage transaction history  

- `ReportService`: generate account statements and statistics  

- `AuditService` (Stage II): thread-safe CSV logging  

---

## Collections Usage

The system uses multiple collection types:

- `List` → transaction history  
- `Set` → sorted clients (`Comparable`)  
- `Map` → indexing (e.g., accounts by IBAN)  

This ensures:
- efficient data access  
- proper ordering  
- logical grouping  

---

## Exception Handling

Custom exceptions are used to enforce business rules:

- `InsufficientFundsException`
- `AccountClosedException`
- `InvalidOperationException`
- `WithdrawalLimitExceededException`
- `CardBlockedException`

Exceptions are both thrown and handled within the service layer.

---

## Persistence (Stage II)

Stage II extends the Stage I OOP project with JDBC persistence, explicit SQL transactions, advanced JOIN queries, and CSV audit logging. The original domain model remains the core of the application, while the new repository and JDBC service layers persist and query data from MySQL.

### Database Configuration

- `resources/schema.sql` contains the relational schema and starts with `DROP TABLE IF EXISTS` statements for clean re-runs.
- `resources/db.properties` stores `db.url`, `db.user`, and `db.password`.
- Database credentials are not hardcoded in Java classes.
- `docker-compose.yml` starts a MySQL instance and mounts `resources/schema.sql` for database initialization.
- `libs/mysql-connector-j-9.7.0.jar` is the JDBC driver used for compilation and runtime.


### Runtime Modes

At startup, the application automatically chooses the data source:

```text
MySQL available   -> runs DatabaseSeeder, uses the database, and does not load in-memory demo lists
MySQL unavailable -> falls back to the Stage I in-memory demo data
```

Useful commands:

```powershell
docker compose up -d
javac -cp "libs\mysql-connector-j-9.7.0.jar" -d out\production\Bank-Project (Get-ChildItem -Recurse -File -Filter *.java -Path src | ForEach-Object { $_.FullName })
java -cp "out\production\Bank-Project;libs\mysql-connector-j-9.7.0.jar;resources" com.pao.project.bank.Main
java -cp "out\production\Bank-Project;libs\mysql-connector-j-9.7.0.jar;resources" com.pao.project.bank.Main --seed-db
```

Main menu option `10. Show startup mode` shows the active mode. In database mode, menus read and modify MySQL data; in fallback mode, they use the in-memory services.
### Relational Schema

The schema covers the main banking areas:

- people and clients: `persons`, `clients`, `individual_clients`, `corporate_clients`
- employees: `employees`, `bank_tellers`, `financial_advisors`
- accounts: `accounts`, `current_accounts`, `savings_accounts`, `iban_aliases`
- transactions: `transactions`, `deposit_transactions`, `withdrawal_transactions`, `transfer_transactions`, `exchange_transactions`
- banking instruments and reports: `cards`, `cheques`, `account_statements`, `account_statement_transactions`
- credits: `credits`, `credit_installments` (the seeder includes demo credits and monthly installments)
- optional database audit table: `audit_logs`

The tables use primary keys, `CHECK` constraints, foreign keys, and controlled cascading deletes where the relationship requires it.

### JDBC Connection

`DatabaseConnection` is implemented as a Singleton. It reads the connection settings from `db.properties`, exposes a reusable `Connection`, and reopens the connection if it was closed.

### Repository Pattern

The generic `Repository<T, ID>` interface defines the standard CRUD operations:

```java
void save(T entity);
Optional<T> findById(ID id);
List<T> findAll();
void update(T entity);
void delete(ID id);
```

The project includes concrete repositories for more than four entities, including:

- `IndividualClientRepository`, `CorporateClientRepository`
- `CurrentAccountRepository`, `SavingsAccountRepository`, `IbanAliasRepository`
- `CardRepository`, `ChequeRepository`, `CreditRepository`
- `TransactionRepository`, `DepositRepository`, `WithdrawalRepository`, `TransferRepository`, `ExchangeRepository`
- `AccountStatementRepository`

Repositories use `PreparedStatement` and `try-with-resources` to close JDBC resources correctly.

### JDBC Operations and Explicit Transactions

The services now include JDBC methods that work directly with the database:

- `openAccountJdbc(...)` creates an account after checking that the IBAN does not already exist.
- `closeAccountJdbc(...)` closes an account logically with `active = false`, without physically deleting it.
- `depositJdbc(...)` updates the account balance and inserts a deposit transaction.
- `withdrawJdbc(...)` validates funds, debits the account, and inserts a withdrawal transaction.
- `transferJdbc(...)` debits the source account, credits the destination account, and inserts transfer details.
- `exchangeJdbc(...)` processes currency exchange between accounts with different currencies.
- `getAccountStatementJdbc(...)` reads account transactions for a date interval and calculates inflows/outflows.
- `applyForCreditJdbc(...)` inserts a credit and generates monthly installments in `credit_installments`.
- `payInstallmentJdbc(...)` checks balance, debits the account, marks the installment as paid, and inserts the transaction.

Operations that modify multiple tables use explicit JDBC transactions with `setAutoCommit(false)`, `commit()`, and `rollback()`.

### Advanced JOIN Queries

The project exposes more than three JOIN-based queries through `ReportService` and `TransactionService`:

- `getClientsWithAccountsJdbc()` lists clients with all their accounts.
- `getTransferHistoryForClientJdbc(int clientId)` returns transfers sent or received by a client.
- `getTopClientsByTransferredAmountJdbc(int limit)` ranks clients by total transferred amount.
- `getAccountStatementJdbc(String iban)` lists transfers for an IBAN with `INCOMING` / `OUTGOING` direction.
- `getAccountsWithoutTransfersJdbc()` uses `LEFT JOIN` to find accounts without transfers.
- `getClientBalanceSummaryJdbc()` calculates account count and total balance per client.
- `getActiveCreditsWithClientDetailsJdbc()` lists active credits with client details and payment account IBAN.
- `getAllTransfersWithAccountsJdbc()` lists all transfers with source and destination IBANs.
- `getTopAccountsBySentTransfersJdbc(int limit)` groups accounts by sent transfer count and amount.

These reports demonstrate `JOIN`, `LEFT JOIN`, `GROUP BY`, `COUNT`, `SUM`, `ORDER BY`, `LIMIT`, and `CASE`.

### CSV Audit

`AuditService` is a thread-safe Singleton. The write method is `synchronized`, and `audit.csv` is opened in append mode so previous audit entries are not overwritten.

Examples of audited actions:

- `open_account`
- `find_account_by_iban`
- `set_account_alias`
- `close_account`
- `get_accounts_for_client`
- `deposit`
- `withdraw`
- `transfer`
- `transfer_jdbc`
- `exchange`

CSV format:

```csv
action_name,timestamp
open_account,2026-06-02T15:20:31.123
deposit,2026-06-02T15:21:05.441
transfer_jdbc,2026-06-02T15:22:10.991
```

## Concepts Demonstrated

#### Object-Oriented Design

| Concept | Where Applied |
|---|---|
| **Multi-level inheritance** | `Person → Client → IndividualClient` (3 levels deep); `Transaction → Transfer` |
| **Abstract classes** | `Person`, `Client`, `Account`, `Transaction` — define contract, prevent direct instantiation |
| **Interfaces** | `Comparable<Client>` for natural ordering in `TreeSet` |
| **Polymorphism** | `List<Transaction>` processes `Deposit`, `Withdrawal`, `Transfer` uniformly in reports |
| **Encapsulation** | All fields `private`; state only mutated through validated, intentional methods |
| **Immutability** | `IBAN` and `Transaction` — `final` fields, no setters, safe for concurrent use |
| **Composition (HAS-A)** | `CorporateClient` contains `IndividualClient` as legal representative |

#### Software Engineering Principles

| Concept | Where Applied |
|---|---|
| **Singleton Pattern** | All six service classes — one controlled instance per JVM lifecycle |
| **Repository Pattern** | Full CRUD abstraction over JDBC; swappable without touching business logic |
| **Single Responsibility** | Each class has one reason to change — models model, services serve, repos persist |
| **Separation of Concerns** | Model knows nothing about persistence; services know nothing about SQL |
| **Defensive Programming** | Validation at construction time (`IBAN`, `Client`, transaction amounts) |
| **Domain-Driven Design (lite)** | Entities, Value Objects (`IBAN`), domain exceptions, and aggregate operations |

#### Java & Data Structures

| Concept | Where Applied |
|---|---|
| **`Comparable<T>`** | `Client` implements natural sort order for `TreeSet` membership |
| **`Comparator<T>`** | Custom sort strategies for reports (by date, amount, type) |
| **`List` / `Set` / `Map`** | Transaction history / client deduplication / O(1) IBAN lookup |
| **JDBC** | `PreparedStatement`, `ResultSet`, `Connection`, `commit` / `rollback` |
| **Thread-safety** | `AuditService` uses `synchronized` blocks for safe concurrent CSV writes |
| **Enumerations** | Typed state constants with compile-time safety across all lifecycle transitions |

---
## Technologies

<p>
  <img src="https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/JDBC-Raw%20SQL-336791?style=flat-square&logo=postgresql&logoColor=white" />
  <img src="https://img.shields.io/badge/Collections-List%20%7C%20Set%20%7C%20Map-007396?style=flat-square" />
  <img src="https://img.shields.io/badge/Pattern-Singleton%20%7C%20Repository-7B2FBE?style=flat-square" />
  <img src="https://img.shields.io/badge/Audit-CSV%20Logging-brightgreen?style=flat-square" />
</p>

| Technology | Purpose |
|---|---|
| **Java 17+** | Core language — no external dependencies |
| **Java Collections Framework** | `List`, `TreeSet`, `HashMap` with `Comparable` / `Comparator` |
| **JDBC** | Direct, framework-free database access with full transaction control |
| **SQL** | Schema design, CRUD, JOIN queries, parameterized statements |
| **CSV I/O** | Append-only, thread-safe audit trail |

---

## Conclusion

This project represents a complete implementation of a banking system using Java, combining solid OOP design with realistic business logic.

It demonstrates:

- clean and extensible architecture  
- proper use of object-oriented principles  
- real-world domain modeling  
- readiness for database integration  


<div align="center">

---

<br/>

**Banking Management System**

<br/>

[![Java](https://img.shields.io/badge/Made%20with-Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://java.com)

<br/>

</div>

