DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS account_statement_transactions;
DROP TABLE IF EXISTS account_statements;
DROP TABLE IF EXISTS exchange_transactions;
DROP TABLE IF EXISTS transfer_transactions;
DROP TABLE IF EXISTS withdrawal_transactions;
DROP TABLE IF EXISTS deposit_transactions;
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS credits;
DROP TABLE IF EXISTS cheques;
DROP TABLE IF EXISTS cards;
DROP TABLE IF EXISTS iban_aliases;
DROP TABLE IF EXISTS current_accounts;
DROP TABLE IF EXISTS savings_accounts;
DROP TABLE IF EXISTS accounts;
DROP TABLE IF EXISTS bank_tellers;
DROP TABLE IF EXISTS financial_advisors;
DROP TABLE IF EXISTS corporate_clients;
DROP TABLE IF EXISTS individual_clients;
DROP TABLE IF EXISTS employees;
DROP TABLE IF EXISTS clients;
DROP TABLE IF EXISTS persons;

-- Person hierarchy
CREATE TABLE persons (
    id INT PRIMARY KEY AUTO_INCREMENT,
    person_type VARCHAR(30) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone_number VARCHAR(30) NOT NULL,

    CONSTRAINT chk_persons_type
        CHECK (person_type IN ('CLIENT', 'EMPLOYEE'))
);

CREATE TABLE clients (
    id INT PRIMARY KEY,
    client_code VARCHAR(50) NOT NULL UNIQUE,
    client_type VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_clients_person
        FOREIGN KEY (id) REFERENCES persons(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_clients_type
        CHECK (client_type IN ('INDIVIDUAL', 'CORPORATE'))
);

CREATE TABLE individual_clients (
    client_id INT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    cnp VARCHAR(13) NOT NULL UNIQUE,
    birth_date DATE NOT NULL,

    CONSTRAINT fk_individual_clients_client
        FOREIGN KEY (client_id) REFERENCES clients(id)
        ON DELETE CASCADE
);

CREATE TABLE corporate_clients (
    client_id INT PRIMARY KEY,
    company_name VARCHAR(150) NOT NULL,
    cui VARCHAR(30) NOT NULL UNIQUE,
    legal_representative_id INT NOT NULL,

    CONSTRAINT fk_corporate_clients_client
        FOREIGN KEY (client_id) REFERENCES clients(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_corporate_clients_legal_representative
        FOREIGN KEY (legal_representative_id) REFERENCES individual_clients(client_id)
);

CREATE TABLE employees (
    id INT PRIMARY KEY,
    employee_code VARCHAR(50) NOT NULL UNIQUE,
    employee_type VARCHAR(30) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    salary DECIMAL(15,2) NOT NULL,
    branch VARCHAR(100) NOT NULL,

    CONSTRAINT fk_employees_person
        FOREIGN KEY (id) REFERENCES persons(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_employees_type
        CHECK (employee_type IN ('BANK_TELLER', 'FINANCIAL_ADVISOR')),

    CONSTRAINT chk_employees_salary
        CHECK (salary >= 0)
);

CREATE TABLE financial_advisors (
    employee_id INT PRIMARY KEY,
    specialization VARCHAR(100) NOT NULL,

    CONSTRAINT fk_financial_advisors_employee
        FOREIGN KEY (employee_id) REFERENCES employees(id)
        ON DELETE CASCADE
);

CREATE TABLE bank_tellers (
    employee_id INT PRIMARY KEY,
    desk_number INT NOT NULL,

    CONSTRAINT fk_bank_tellers_employee
        FOREIGN KEY (employee_id) REFERENCES employees(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_bank_tellers_desk_number
        CHECK (desk_number > 0)
);

-- Account hierarchy
CREATE TABLE accounts (
    id INT PRIMARY KEY,
    iban VARCHAR(34) NOT NULL UNIQUE,
    account_type VARCHAR(30) NOT NULL,
    balance DECIMAL(15,2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    opening_date DATE NOT NULL,
    client_id INT NOT NULL,

    CONSTRAINT fk_accounts_client
        FOREIGN KEY (client_id) REFERENCES clients(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_accounts_type
        CHECK (account_type IN ('CURRENT', 'SAVINGS')),

    CONSTRAINT chk_accounts_currency
        CHECK (currency IN ('RON', 'EUR', 'USD', 'GBP')),

    CONSTRAINT chk_accounts_balance
        CHECK (balance >= 0)
);

CREATE TABLE current_accounts (
    account_id INT PRIMARY KEY,
    monthly_fee DECIMAL(15,2) NOT NULL,

    CONSTRAINT fk_current_accounts_account
        FOREIGN KEY (account_id) REFERENCES accounts(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_current_accounts_monthly_fee
        CHECK (monthly_fee >= 0)
);

CREATE TABLE savings_accounts (
    account_id INT PRIMARY KEY,
    interest_rate DECIMAL(5,2) NOT NULL,
    withdrawals_this_month INT NOT NULL DEFAULT 0,

    CONSTRAINT fk_savings_accounts_account
        FOREIGN KEY (account_id) REFERENCES accounts(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_savings_accounts_interest_rate
        CHECK (interest_rate >= 0),

    CONSTRAINT chk_savings_accounts_withdrawals
        CHECK (withdrawals_this_month >= 0)
);

CREATE TABLE iban_aliases (
    alias VARCHAR(50) PRIMARY KEY,
    account_id INT NOT NULL,

    CONSTRAINT fk_iban_aliases_account
        FOREIGN KEY (account_id) REFERENCES accounts(id)
        ON DELETE CASCADE
);

-- Transaction hierarchy
CREATE TABLE transactions (
    id INT PRIMARY KEY,
    transaction_type VARCHAR(30) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    timestamp DATETIME NOT NULL,
    description VARCHAR(255) NOT NULL DEFAULT '',

    CONSTRAINT chk_transactions_type
        CHECK (transaction_type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER', 'EXCHANGE')),

    CONSTRAINT chk_transactions_amount
        CHECK (amount > 0)
);

CREATE TABLE deposit_transactions (
    transaction_id INT PRIMARY KEY,
    destination_account_id INT NOT NULL,

    CONSTRAINT fk_deposit_transactions_transaction
        FOREIGN KEY (transaction_id) REFERENCES transactions(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_deposit_transactions_destination_account
        FOREIGN KEY (destination_account_id) REFERENCES accounts(id)
);

CREATE TABLE withdrawal_transactions (
    transaction_id INT PRIMARY KEY,
    source_account_id INT NOT NULL,

    CONSTRAINT fk_withdrawal_transactions_transaction
        FOREIGN KEY (transaction_id) REFERENCES transactions(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_withdrawal_transactions_source_account
        FOREIGN KEY (source_account_id) REFERENCES accounts(id)
);

CREATE TABLE transfer_transactions (
    transaction_id INT PRIMARY KEY,
    source_account_id INT NOT NULL,
    destination_account_id INT NOT NULL,

    CONSTRAINT fk_transfer_transactions_transaction
        FOREIGN KEY (transaction_id) REFERENCES transactions(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_transfer_transactions_source_account
        FOREIGN KEY (source_account_id) REFERENCES accounts(id),

    CONSTRAINT fk_transfer_transactions_destination_account
        FOREIGN KEY (destination_account_id) REFERENCES accounts(id),

    CONSTRAINT chk_transfer_transactions_different_accounts
        CHECK (source_account_id <> destination_account_id)
);

CREATE TABLE exchange_transactions (
    transaction_id INT PRIMARY KEY,
    source_account_id INT NOT NULL,
    destination_account_id INT NOT NULL,
    destination_amount DECIMAL(15,2) NOT NULL,
    from_currency VARCHAR(10) NOT NULL,
    to_currency VARCHAR(10) NOT NULL,
    exchange_rate DECIMAL(15,6) NOT NULL,

    CONSTRAINT fk_exchange_transactions_transaction
        FOREIGN KEY (transaction_id) REFERENCES transactions(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_exchange_transactions_source_account
        FOREIGN KEY (source_account_id) REFERENCES accounts(id),

    CONSTRAINT fk_exchange_transactions_destination_account
        FOREIGN KEY (destination_account_id) REFERENCES accounts(id),

    CONSTRAINT chk_exchange_transactions_destination_amount
        CHECK (destination_amount > 0),

    CONSTRAINT chk_exchange_transactions_rate
        CHECK (exchange_rate > 0),

    CONSTRAINT chk_exchange_transactions_currency
        CHECK (
            from_currency IN ('RON', 'EUR', 'USD', 'GBP')
            AND to_currency IN ('RON', 'EUR', 'USD', 'GBP')
            AND from_currency <> to_currency
        ),

    CONSTRAINT chk_exchange_transactions_different_accounts
        CHECK (source_account_id <> destination_account_id)
);

-- Card
CREATE TABLE cards (
    card_number VARCHAR(16) PRIMARY KEY,
    cvv VARCHAR(3) NOT NULL,
    expiration_date DATE NOT NULL,
    contactless BOOLEAN NOT NULL,
    status VARCHAR(30) NOT NULL,
    account_id INT NOT NULL,

    CONSTRAINT fk_cards_account
        FOREIGN KEY (account_id) REFERENCES accounts(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_cards_number
        CHECK (CHAR_LENGTH(card_number) = 16),

    CONSTRAINT chk_cards_cvv
        CHECK (CHAR_LENGTH(cvv) = 3),

    CONSTRAINT chk_cards_status
        CHECK (status IN ('ACTIVE', 'BLOCKED', 'EXPIRED'))
);

-- AccountStatement
CREATE TABLE account_statements (
    id INT PRIMARY KEY AUTO_INCREMENT,
    account_id INT NOT NULL,
    generated_at DATE NOT NULL,
    total_inflows DECIMAL(15,2) NOT NULL,
    total_outflows DECIMAL(15,2) NOT NULL,
    initial_balance DECIMAL(15,2) NOT NULL,
    final_balance DECIMAL(15,2) NOT NULL,

    CONSTRAINT fk_account_statements_account
        FOREIGN KEY (account_id) REFERENCES accounts(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_account_statements_totals
        CHECK (
            total_inflows >= 0
            AND total_outflows >= 0
            AND initial_balance >= 0
            AND final_balance >= 0
        )
);

CREATE TABLE account_statement_transactions (
    statement_id INT NOT NULL,
    transaction_id INT NOT NULL,

    PRIMARY KEY (statement_id, transaction_id),

    CONSTRAINT fk_statement_transactions_statement
        FOREIGN KEY (statement_id) REFERENCES account_statements(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_statement_transactions_transaction
        FOREIGN KEY (transaction_id) REFERENCES transactions(id)
        ON DELETE CASCADE
);

-- Cheque
CREATE TABLE cheques (
    series VARCHAR(12) PRIMARY KEY,
    issuer_account_id INT NOT NULL,
    beneficiary_client_id INT NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    issue_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL,

    CONSTRAINT fk_cheques_issuer_account
        FOREIGN KEY (issuer_account_id) REFERENCES accounts(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_cheques_beneficiary_client
        FOREIGN KEY (beneficiary_client_id) REFERENCES clients(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_cheques_amount
        CHECK (amount > 0),

    CONSTRAINT chk_cheques_dates
        CHECK (expiry_date > issue_date),

    CONSTRAINT chk_cheques_status
        CHECK (status IN ('ISSUED', 'CASHED', 'CANCELLED', 'EXPIRED'))
);

-- Credit
CREATE TABLE credits (
    id INT PRIMARY KEY AUTO_INCREMENT,
    borrower_id INT NOT NULL,
    target_account_id INT NOT NULL,
    credit_type VARCHAR(30) NOT NULL,
    principal_amount DECIMAL(15,2) NOT NULL,
    annual_interest_rate DECIMAL(5,2) NOT NULL,
    duration_in_months INT NOT NULL,
    start_date DATE,
    remaining_amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(30) NOT NULL,

    CONSTRAINT fk_credits_borrower
        FOREIGN KEY (borrower_id) REFERENCES clients(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_credits_target_account
        FOREIGN KEY (target_account_id) REFERENCES accounts(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_credits_type
        CHECK (credit_type IN ('PERSONAL', 'MORTGAGE', 'BUSINESS')),

    CONSTRAINT chk_credits_status
        CHECK (status IN ('PENDING', 'ACTIVE', 'PAID', 'REJECTED', 'DEFAULTED')),

    CONSTRAINT chk_credits_amounts
        CHECK (
            principal_amount > 0
            AND annual_interest_rate >= 0
            AND duration_in_months > 0
            AND remaining_amount >= 0
        ),

    CONSTRAINT chk_credits_start_date
        CHECK (
            status IN ('PENDING', 'REJECTED')
            OR start_date IS NOT NULL
        )
);

-- Audit
CREATE TABLE audit_logs (
    id INT PRIMARY KEY AUTO_INCREMENT,
    action_name VARCHAR(100) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
