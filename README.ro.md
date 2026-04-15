<div align="center">

# Banking Management System
### Programare Avansată pe Obiecte — Proiect 2026

<p>
  <i>
    Simulare completă a unui sistem bancar în Java — modelare OOP,<br/>
    arhitectură pe layere și persistență JDBC.
  </i>
</p>

<br/>

[![Java](https://img.shields.io/badge/Java-17+-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://java.com)
[![JDBC](https://img.shields.io/badge/JDBC-Raw%20SQL-336791?style=flat-square&logo=postgresql&logoColor=white)]()
[![Pattern](https://img.shields.io/badge/Pattern-Singleton%20%7C%20Repository-7B2FBE?style=flat-square)]()
[![Audit](https://img.shields.io/badge/Audit-CSV%20Logging-brightgreen?style=flat-square)]()

<br/>

**[Descriere](#descriere)** · **[Funcționalități](#funcționalități)** · **[Acțiuni sistem](#1-acțiuni--interogări-posibile-în-sistem)** · **[Tipuri de obiecte](#2-tipuri-de-obiecte-din-domeniu)** · **[Arhitectură](#arhitectură)** · **[Model de domeniu](#model-de-domeniu)** · **[Colecții](#colecții)** · **[Excepții](#excepții-custom)** · **[Persistență](#persistență-etapa-ii)** · **[Tehnologii](#tehnologii)**

</div>

---

## Descriere

**Banking Management System** este o aplicație Java care modelează un mediu bancar realist folosind principii avansate de Programare Orientată pe Obiecte.

Sistemul gestionează:

- clienți individuali și corporativi
- angajați bancari (casieri, consilieri financiari)
- conturi bancare (curente și de economii)
- carduri bancare
- tranzacții (depuneri, retrageri, transferuri)
- instrumente de plată (CEC-uri)
- extrase de cont și rapoarte financiare

Proiectul este dezvoltat în **două etape progresive**:

| Etapă | Domeniu | Livrabil principal |
|:---:|---|---|
| **I** | Modelare OOP completă cu date în memorie | Model de domeniu, servicii, excepții custom, colecții |
| **II** | Persistență și audit | JDBC cu tranzacții explicite, PreparedStatements, audit CSV |

---

## Funcționalități

### Gestionare Clienți
- Suport pentru clienți individuali (CNP) și corporativi (CUI)
- Logică de identificare și validare unică
- Clienții corporativi includ un reprezentant legal prin compoziție

### Gestionare Conturi
- Conturi curente și conturi de economii
- Ciclu de viață complet (deschidere, închidere, validare)
- Aplicarea regulilor de business (ex: sold nenul blochează închiderea)

### Tranzacții
- Operații de depunere, retragere și transfer
- Validare fonduri și stare cont
- Urmărirea istoricului tranzacțiilor per cont

### Sistem Carduri
- Emitere card asociat unui cont
- Gestionare stare: activ, blocat, expirat
- Validare stare card înainte de orice operație

### Raportare
- Generare extras de cont cu sold inițial, intrări, ieșiri, sold final
- Statistici agregate per client
- Sortare tranzacții după dată sau sumă

### Funcționalități Suplimentare
- Suport CEC ca instrument de plată alternativ
- `ClientPremium` cu limită de descoperit de cont (`overdraft`)
- Aplicare dobândă automată la conturile de economii
- `RaportService` cu statistici avansate (top conturi, total transferuri per client)

---

## Definirea Sistemului (Etapa I)

### 1. Acțiuni / Interogări posibile în sistem

1. **Adaugă client individual** — înregistrează un client nou cu CNP, date personale și validare unicitate
2. **Deschide cont** — deschide un cont curent sau de economii pentru un client existent
3. **Depune bani** — creditează un cont cu o sumă specificată, înregistrând tranzacția
4. **Retrage bani** — debitează un cont cu validare fonduri disponibile (aruncă `InsufficientFundsException` dacă soldul e insuficient)
5. **Efectuează transfer** — transferă o sumă între două conturi diferite, actualizând ambele solduri atomic
6. **Emite card** — generează un card nou asociat unui cont și unui client
7. **Blochează card** — schimbă starea cardului în `BLOCKED`, invalidând operațiile ulterioare
8. **Caută cont după IBAN** — returnează contul asociat unui IBAN dat sau aruncă `AccountNotFoundException`
9. **Afișează istoricul tranzacțiilor** — listează toate tranzacțiile unui cont, sortate descrescător după dată
10. **Aplică dobândă** — calculează și adaugă dobânda acumulată la toate conturile de economii active
11. **Afișează istoricul tranzacțiilor** — listează toate tranzacțiile unui cont sortate descrescător după `LocalDateTime`, cu tipul, suma și descrierea fiecăreia
12. **Aplică dobândă** — iterează toate conturile de economii active, calculează dobânda acumulată pe baza `rataDobanda` și creditează fiecare cont, înregistrând o tranzacție `Deposit` de tip dobândă
13. **Generează extras de cont** — produce un `AccountStatement` pentru un interval de date dat, cu sold inițial, total intrări, total ieșiri și sold final calculat
14. **Listează conturile unui client** — returnează toate conturile active ale unui client grupate după tip (`CURRENT` / `SAVINGS`), cu soldurile curente afișate

### 2. Tipuri de obiecte din domeniu

| # | Clasă | Descriere |
|---|---|---|
| 1 | `Person` | Clasă abstractă — baza ierarhiei de persoane |
| 2 | `IndividualClient` | Client persoană fizică, identificat prin CNP |
| 3 | `CorporateClient` | Client persoană juridică, CUI + reprezentant legal (compoziție) |
| 4 | `BankEmployee` / `BankTeller` / `FinancialAdvisor` | Angajați bancari — ierarhie separată |
| 5 | `Account` | Clasă abstractă — baza ierarhiei de conturi |
| 6 | `CurrentAccount` | Cont curent, suportă descoperit de cont pentru `PremiumClient` |
| 7 | `SavingsAccount` | Cont de economii cu rată de dobândă și limită de retrageri lunare |
| 8 | `IBAN` | Obiect valoare **imutabil** (`final`) cu validare format în constructor |
| 9 | `Transaction` / `Deposit` / `Withdrawal` / `Transfer` | Ierarhie tranzacții — abstractă + 3 subclase |
| 10 | `Card` | Card bancar cu mașină de stări: `ACTIVE → BLOCKED / EXPIRED` |
| 11 | `AccountStatement` | Extras de cont — model de raportare agregat |
| 12 | `Cheque` | Instrument de plată alternativ cu ciclu de viață propriu |

---

## Arhitectură

### Structura proiectului

```
src/
└── com/pao/proiect/banca/
    ├── Main.java
    ├── model/
    │   ├── person/
    │   │   ├── Person.java                  ← Abstractă, getRol()
    │   │   ├── Client.java                  ← Abstractă, implements Comparable<Client>
    │   │   ├── IndividualClient.java         ← CNP, date personale
    │   │   ├── CorporateClient.java          ← CUI + reprezentant legal (compoziție)
    │   │   ├── BankEmployee.java             ← Abstractă
    │   │   ├── BankTeller.java
    │   │   └── FinancialAdvisor.java
    │   ├── account/
    │   │   ├── Account.java                 ← Abstractă, implements Tranzactionabila
    │   │   ├── CurrentAccount.java
    │   │   └── SavingsAccount.java           ← rataDobanda, aplicaDobanda(), limită retrageri
    │   ├── transaction/
    │   │   ├── Transaction.java             ← Abstractă + IMUTABILĂ (câmpuri final)
    │   │   ├── Deposit.java
    │   │   ├── Withdrawal.java
    │   │   └── Transfer.java                ← contSursa + contDestinatie
    │   ├── IBAN.java                        ← Obiect valoare imutabil
    │   ├── Card.java                        ← Mașină de stări CardStatus
    │   ├── AccountStatement.java            ← Raport agregat
    │   ├── Cheque.java                      ← Instrument de plată cu ciclu de viață
    │   └── enums/
    │       ├── AccountType.java
    │       ├── TransactionType.java
    │       ├── CardStatus.java
    │       └── ChequeStatus.java
    ├── service/
    │   ├── ClientService.java               ← Singleton
    │   ├── AccountService.java              ← Singleton
    │   ├── CardService.java                 ← Singleton
    │   ├── TransactionService.java          ← Singleton
    │   ├── ReportService.java               ← Singleton
    │   └── AuditService.java                ← Singleton · Thread-safe · CSV
    ├── repository/                          ← Etapa II
    │   ├── Repository.java                  ← Interfață generică <T, ID>
    │   ├── ClientRepository.java
    │   ├── AccountRepository.java
    │   ├── CardRepository.java
    │   └── TransactionRepository.java
    ├── exception/
    │   ├── InsufficientFundsException.java
    │   ├── AccountClosedException.java
    │   ├── AccountNotFoundException.java
    │   ├── InvalidOperationException.java
    │   ├── WithdrawalLimitExceededException.java
    │   └── CardBlockedException.java
    └── util/
        ├── DatabaseConnection.java          ← Singleton JDBC
        ├── ConfigLoader.java                ← Citire db.properties
        └── IbanGenerator.java               ← Generare IBAN unic

resources/
    ├── db.properties
    └── schema.sql

README.md
```

Proiectul urmează o **arhitectură pe layere**:

- **Model Layer** — entitățile de domeniu și relațiile dintre ele
- **Service Layer** — logica de business și fluxurile aplicației (Singleton)
- **Repository Layer** *(Etapa II)* — interacțiunea cu baza de date prin JDBC
- **Exception Layer** — excepții custom care codifică regulile de business
- **Util Layer** — utilitare tehnice: conexiune DB, configurare, generare IBAN

---

## Model de domeniu

### Ierarhia Persoane

```
Person (abstract)                    ← getRol() abstract
├── Client (abstract)                ← implements Comparable<Client>
│   ├── IndividualClient             ← CNP, date personale
│   └── CorporateClient              ← CUI + IndividualClient reprezentant (compoziție)
└── BankEmployee (abstract)
    ├── BankTeller                   ← sucursală, operații ghișeu
    └── FinancialAdvisor             ← portofoliu clienți, rată comision
```

### Ierarhia Conturi

```
Account (abstract)                   ← implements Tranzactionabila
├── CurrentAccount                   ← overdraft pentru PremiumClient
└── SavingsAccount                   ← rataDobanda, limită retrageri lunare
```

### Ierarhia Tranzacții

```
Transaction (abstract + imutabilă)   ← id, suma, data (final), descriere
├── Deposit
├── Withdrawal
└── Transfer                         ← contSursa + contDestinatie
```

### Clase suplimentare

| Clasă | Tip | Detalii |
|---|---|---|
| `IBAN` | **Imutabilă** | `final String cod`, validare regex în constructor, fără setteri |
| `Card` | Entitate | Mașină de stări: `ACTIVE → BLOCKED`, `ACTIVE → EXPIRED` |
| `AccountStatement` | Model raport | Sold inițial, total intrări, total ieșiri, sold final, perioadă |
| `Cheque` | Instrument | Stări: `ISSUED → CASHED / CANCELLED / EXPIRED` |

### Enumerații

```java
enum AccountType     { CURRENT, SAVINGS }
enum TransactionType { DEPOSIT, WITHDRAWAL, TRANSFER }
enum CardStatus      { ACTIVE, BLOCKED, EXPIRED }
enum ChequeStatus    { ISSUED, CASHED, CANCELLED, EXPIRED }
```

---

## Colecții

Colecțiile sunt alese cu sens pentru domeniu, nu doar pentru a bifa cerințele:

| Colecție | Unde | Justificare |
|---|---|---|
| `TreeSet<Client>` | `ClientService` | Clienți unici, sortat natural după nume (`Comparable`) |
| `HashMap<String, Account>` | `AccountService` | Indexare O(1) după IBAN pentru căutări rapide |
| `HashMap<Client, List<Account>>` | `AccountService` | Grupare conturi per client |
| `List<Transaction>` | `TransactionService` | Istoric tranzacții, sortabil după dată (`Comparator`) |
| `HashMap<String, List<Transaction>>` | `TransactionService` | Istoric per IBAN |

Sortare demonstrată în două moduri:
- **`Comparable<Client>`** — ordine naturală după nume de familie în `TreeSet`
- **`Comparator<Transaction>`** — sortare descrescătoare după `LocalDateTime` în rapoarte

---

## Excepții Custom

Excepțiile codifică regulile de business, nu doar erorile tehnice:

| Excepție | Când se aruncă |
|---|---|
| `InsufficientFundsException` | Retragere sau transfer cu fonduri insuficiente |
| `AccountClosedException` | Operație pe un cont cu starea `CLOSED` |
| `AccountNotFoundException` | Căutare după IBAN inexistent |
| `InvalidOperationException` | Transfer către același cont, închidere cont cu sold nenul |
| `WithdrawalLimitExceededException` | Depășirea limitei de retrageri lunare la `SavingsAccount` |
| `CardBlockedException` | Operație cu un card în starea `BLOCKED` sau `EXPIRED` |

Toate excepțiile sunt aruncate în servicii și tratate fie în servicii, fie în `Main`.

---

## Servicii (Singleton)

Fiecare serviciu are constructor `private` și metodă statică `getInstance()`:

| Serviciu | Operații principale |
|---|---|
| `ClientService` | `addClient`, `removeClient`, `findByCNP`, `listAll` |
| `AccountService` | `openAccount`, `closeAccount`, `deposit`, `withdraw`, `transfer`, `findByIBAN` |
| `CardService` | `issueCard`, `blockCard`, `unblockCard`, `validateCard` |
| `TransactionService` | `getHistoryForAccount`, `getHistoryForClient`, `sortByDate` |
| `ReportService` | `generateStatement`, `topAccountsByBalance`, `totalTransfersPerClient`, `applyInterest` |
| `AuditService` | `log(actionName)` — thread-safe, scriere CSV în mod append |

---

## Reguli de Business

- Nu poți retrage mai mult decât soldul (excepție `InsufficientFundsException`)
- Nu poți transfera către același IBAN sursă (excepție `InvalidOperationException`)
- Nu poți închide un cont cu sold nenul (excepție `InvalidOperationException`)
- `CorporateClient` nu poate exista fără un reprezentant legal (`IndividualClient`)
- Cardul blocat/expirat invalidează orice tranzacție (excepție `CardBlockedException`)
- `SavingsAccount` permite maxim 2 retrageri pe lună (excepție `WithdrawalLimitExceededException`)
- Suma oricărei tranzacții trebuie să fie strict pozitivă — validată în constructor

---

## Persistență (Etapa II)

### Entități persistate

- `Client` (individual și corporativ)
- `Account` (curent și economii)
- `Card`
- `Transaction`


## Concepte Demonstrate

### Design Orientat pe Obiecte

| Concept | Unde este aplicat |
|---|---|
| **Moștenire pe 3 niveluri** | `Person → Client → IndividualClient`; `Person → BankEmployee → BankTeller` |
| **Clase abstracte** | `Person`, `Client`, `Account`, `Transaction` — definesc contract, previn instanțierea directă |
| **Interfețe** | `Comparable<Client>` pentru ordonare naturală în `TreeSet`; `Tranzactionabila` pentru conturi |
| **Polimorfism** | `List<Transaction>` procesează `Deposit`, `Withdrawal`, `Transfer` uniform în rapoarte |
| **Encapsulare** | Toate câmpurile `private`; starea se modifică doar prin metode validate |
| **Imutabilitate** | `IBAN` și `Transaction` — câmpuri `final`, fără setteri, sigure pentru utilizare concurentă |
| **Compoziție (HAS-A)** | `CorporateClient` conține `IndividualClient` ca reprezentant legal |

### Principii de Inginerie Software

| Concept | Unde este aplicat |
|---|---|
| **Singleton Pattern** | Toate cele 6 clase de serviciu — o instanță controlată per ciclu de viață JVM |
| **Repository Pattern** | Abstractizare CRUD completă peste JDBC; înlocuibilă fără a atinge logica de business |
| **Single Responsibility** | Modelele modelează, serviciile servesc, repository-urile persistă |
| **Separation of Concerns** | Modelul nu știe nimic despre persistență; serviciile nu știu nimic despre SQL |
| **Programare Defensivă** | Validare la construcție (`IBAN`, sume tranzacții) și în fiecare operație de serviciu |

### Java & Structuri de Date

| Concept | Unde este aplicat |
|---|---|
| **`Comparable<T>`** | `Client` implementează ordine naturală pentru `TreeSet` |
| **`Comparator<T>`** | Strategii de sortare custom pentru rapoarte (după dată, sumă, tip) |
| **`List` / `Set` / `Map`** | Istoric tranzacții / deduplicare clienți / lookup O(1) după IBAN |
| **JDBC** | `PreparedStatement`, `ResultSet`, `Connection`, `commit` / `rollback` |
| **Thread-safety** | `AuditService` folosește `synchronized` pentru scrieri CSV concurente sigure |
| **Enumerații** | Constante de stare cu siguranță la compilare pentru toate tranzițiile de ciclu de viață |

---

## Tehnologii

| Tehnologie | Scop |
|---|---|
| **Java 17+** | Limbaj de bază — fără dependențe externe |
| **Java Collections Framework** | `List`, `TreeSet`, `HashMap` cu `Comparable` / `Comparator` |
| **JDBC** | Acces direct la baza de date fără framework, cu control complet al tranzacțiilor |
| **SQL** | Design schemă, CRUD, interogări cu JOIN, PreparedStatements |
| **CSV I/O** | Jurnal de audit append-only, thread-safe |

---

## Instrucțiuni de Rulare

### Etapa I (fără bază de date)

```bash
# Compilare
javac -d out src/com/pao/proiect/banca/**/*.java src/com/pao/proiect/banca/Main.java

# Rulare
java -cp out com.pao.proiect.banca.Main
```

---

## Predare

| Etapă | Deadline | Branch Git |
|---|---|---|
| **Etapa I** | joi, 24 aprilie 2026, 23:59 | `proiect-etapa1` |
| **Etapa II** | joi, 5 iunie 2026, 23:59 | `proiect-etapa2` |

```bash
# Etapa I
git checkout -b proiect-etapa1
git add .
git commit -m "Proiect Etapa I: implementare completa"
git push origin proiect-etapa1

# Etapa II
git checkout -b proiect-etapa2
git add .
git commit -m "Proiect Etapa II: JDBC, audit, tranzactii"
git push origin proiect-etapa2
```

---

<div align="center">

**Banking Management System** · PAO 2026

[![Java](https://img.shields.io/badge/Made%20with-Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://java.com)

</div>
