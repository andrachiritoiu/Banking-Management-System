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

```text
src/
└── com/pao/proiect/banca/
    ├── model/        # Domain entities
    ├── service/      # Business logic (Singleton services)
    ├── repository/   # JDBC layer (Stage II)
    ├── exception/    # Custom exceptions
    ├── util/         # Utilities (DB connection, config)
    └── Main.java     # Application entry point
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

All functionalities are demonstrated in the `Main` class through a complete execution scenario.

---

## Services Layer

The application uses the **Singleton pattern** for service classes.

### Main Services

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

```
src/
└── com/pao/proiect/banca/
    ├── model/
    │   ├── person/
    │   │   ├── Person.java               ← Abstract base
    │   │   ├── Client.java               ← Abstract client
    │   │   ├── IndividualClient.java     ← CNP, personal data
    │   │   ├── CorporateClient.java      ← CUI + legal rep (composition)
    │   │   ├── BankEmployee.java         ← Abstract employee
    │   │   ├── BankTeller.java
    │   │   └── FinancialAdvisor.java
    │   ├── account/
    │   │   ├── Account.java              ← Abstract, holds IBAN
    │   │   ├── CurrentAccount.java
    │   │   └── SavingsAccount.java       ← Interest rate + withdrawal cap
    │   ├── transaction/
    │   │   ├── Transaction.java          ← Immutable abstract base
    │   │   ├── Deposit.java
    │   │   ├── Withdrawal.java
    │   │   └── Transfer.java             ← Atomic two-account operation
    │   ├── IBAN.java                     ← Immutable value object
    │   ├── Card.java                     ← State machine: ACTIVE→BLOCKED/EXPIRED
    │   ├── AccountStatement.java         ← Aggregated report model
    │   ├── Cheque.java                   ← Payment instrument with lifecycle
    │   └── enums/
    │       ├── AccountType.java
    │       ├── TransactionType.java
    │       ├── CardStatus.java
    │       └── ChequeStatus.java
    ├── service/
    │   ├── ClientService.java            ← Singleton
    │   ├── AccountService.java           ← Singleton
    │   ├── CardService.java              ← Singleton
    │   ├── TransactionService.java       ← Singleton
    │   ├── ReportService.java            ← Singleton
    │   └── AuditService.java             ← Singleton · Thread-safe CSV writer
    ├── repository/
    │   ├── ClientRepository.java         ← Full CRUD
    │   ├── AccountRepository.java        ← Full CRUD + JOINs
    │   ├── CardRepository.java           ← Full CRUD
    │   └── TransactionRepository.java    ← Full CRUD + history queries
    ├── exception/
    │   ├── InsufficientFundsException.java
    │   ├── AccountClosedException.java
    │   ├── InvalidOperationException.java
    │   ├── WithdrawalLimitExceededException.java
    │   └── CardBlockedException.java
    ├── util/
    │   ├── DBConnection.java             ← Connection management
    │   └── Config.java                  ← Config loading
    └── Main.java
```

## Persistence (Stage II)

### Entities persisted
- Client  
- Account  
- Card  
- Transaction  

### Features
- Full CRUD operations  
- JDBC with `PreparedStatement`  
- Explicit transactions (`commit` / `rollback`)  
- JOIN-based queries  
- CSV audit logging  

---

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

