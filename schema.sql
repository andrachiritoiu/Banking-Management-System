DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS credits;
DROP TABLE IF EXISTS cheques;
DROP TABLE IF EXISTS cards;
DROP TABLE IF EXISTS iban_aliases;
DROP TABLE IF EXISTS accounts;
DROP TABLE IF EXISTS employees;
DROP TABLE IF EXISTS clients;

CREATE TABLE clients (
    id INT PRIMARY KEY AUTO_INCREMENT,
    client_code VARCHAR(50) NOT NULL UNIQUE,
    client_type VARCHAR(30) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone_number VARCHAR(30),
    active BOOLEAN NOT NULL DEFAULT TRUE,

    first_name VARCHAR(100),
    last_name VARCHAR(100),
    cnp VARCHAR(20) UNIQUE,
    birth_date DATE,

    company_name VARCHAR(150),
    cui VARCHAR(30) UNIQUE,
    legal_representative_id INT,

    CONSTRAINT fk_clients_legal_representative
        FOREIGN KEY (legal_representative_id) REFERENCES clients(id),

    CONSTRAINT chk_clients_type
        CHECK (client_type IN ('INDIVIDUAL', 'CORPORATE')),

    CONSTRAINT chk_clients_individual_fields
        CHECK (
            client_type <> 'INDIVIDUAL'
            OR (
                first_name IS NOT NULL
                AND last_name IS NOT NULL
                AND cnp IS NOT NULL
                AND company_name IS NULL
                AND cui IS NULL
            )
        ),

    CONSTRAINT chk_clients_corporate_fields
        CHECK (
            client_type <> 'CORPORATE'
            OR (
                company_name IS NOT NULL
                AND cui IS NOT NULL
                AND legal_representative_id IS NOT NULL
                AND cnp IS NULL
            )
        )
);

CREATE TABLE employees (
    id INT PRIMARY KEY AUTO_INCREMENT,
    employee_code VARCHAR(50) NOT NULL UNIQUE,
    employee_type VARCHAR(30) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone_number VARCHAR(30),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    salary DECIMAL(15,2) NOT NULL,
    branch VARCHAR(100) NOT NULL,

    desk_number INT,
    specialization VARCHAR(100),

    CONSTRAINT chk_employees_type
        CHECK (employee_type IN ('BANK_TELLER', 'FINANCIAL_ADVISOR')),

    CONSTRAINT chk_employees_salary
        CHECK (salary >= 0),

    CONSTRAINT chk_employees_teller_fields
        CHECK (
            employee_type <> 'BANK_TELLER'
            OR (
                desk_number IS NOT NULL
                AND specialization IS NULL
            )
        ),

    CONSTRAINT chk_employees_advisor_fields
        CHECK (
            employee_type <> 'FINANCIAL_ADVISOR'
            OR (
                specialization IS NOT NULL
                AND desk_number IS NULL
            )
        )
);

CREATE TABLE accounts (
    id INT PRIMARY KEY AUTO_INCREMENT,
    iban VARCHAR(34) NOT NULL UNIQUE,
    balance DECIMAL(15,2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    account_type VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    opening_date DATE NOT NULL,
    client_id INT NOT NULL,

    monthly_fee DECIMAL(15,2),
    interest_rate DECIMAL(5,2),
    withdrawals_this_month INT,

    CONSTRAINT fk_accounts_client
        FOREIGN KEY (client_id) REFERENCES clients(id),

    CONSTRAINT chk_accounts_type
        CHECK (account_type IN ('CURRENT', 'SAVINGS')),

    CONSTRAINT chk_accounts_currency
        CHECK (currency IN ('RON', 'EUR', 'USD', 'GBP')),

    CONSTRAINT chk_accounts_balance
        CHECK (balance >= 0),

    CONSTRAINT chk_accounts_current_fields
        CHECK (
            account_type <> 'CURRENT'
            OR (
                monthly_fee IS NOT NULL
                AND monthly_fee >= 0
                AND interest_rate IS NULL
                AND withdrawals_this_month IS NULL
            )
        ),

    CONSTRAINT chk_accounts_savings_fields
        CHECK (
            account_type <> 'SAVINGS'
            OR (
                interest_rate IS NOT NULL
                AND interest_rate >= 0
                AND withdrawals_this_month IS NOT NULL
                AND withdrawals_this_month >= 0
                AND monthly_fee IS NULL
            )
        )
);

CREATE TABLE iban_aliases (
    alias VARCHAR(50) PRIMARY KEY,
    account_id INT NOT NULL,

    CONSTRAINT fk_iban_aliases_account
        FOREIGN KEY (account_id) REFERENCES accounts(id)
        ON DELETE CASCADE
);

CREATE TABLE cards (
    id INT PRIMARY KEY AUTO_INCREMENT,
    card_number VARCHAR(30) NOT NULL UNIQUE,
    cvv VARCHAR(10) NOT NULL,
    expiration_date DATE NOT NULL,
    contactless BOOLEAN NOT NULL,
    status VARCHAR(30) NOT NULL,
    account_id INT NOT NULL,

    CONSTRAINT fk_cards_account
        FOREIGN KEY (account_id) REFERENCES accounts(id),

    CONSTRAINT chk_cards_status
        CHECK (status IN ('ACTIVE', 'BLOCKED', 'EXPIRED'))
);

CREATE TABLE cheques (
    id INT PRIMARY KEY AUTO_INCREMENT,
    series VARCHAR(50) NOT NULL UNIQUE,
    issuer_account_id INT NOT NULL,
    beneficiary_client_id INT NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    issue_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL,

    CONSTRAINT fk_cheques_issuer_account
        FOREIGN KEY (issuer_account_id) REFERENCES accounts(id),

    CONSTRAINT fk_cheques_beneficiary_client
        FOREIGN KEY (beneficiary_client_id) REFERENCES clients(id),

    CONSTRAINT chk_cheques_amount
        CHECK (amount > 0),

    CONSTRAINT chk_cheques_dates
        CHECK (expiry_date > issue_date),

    CONSTRAINT chk_cheques_status
        CHECK (status IN ('ISSUED', 'CASHED', 'CANCELLED', 'EXPIRED'))
);

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
        FOREIGN KEY (borrower_id) REFERENCES clients(id),

    CONSTRAINT fk_credits_target_account
        FOREIGN KEY (target_account_id) REFERENCES accounts(id),

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

CREATE TABLE transactions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    transaction_type VARCHAR(30) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    timestamp DATETIME NOT NULL,
    description VARCHAR(255),

    source_account_id INT,
    destination_account_id INT,

    source_amount DECIMAL(15,2),
    destination_amount DECIMAL(15,2),
    from_currency VARCHAR(10),
    to_currency VARCHAR(10),
    exchange_rate DECIMAL(15,6),

    CONSTRAINT fk_transactions_source_account
        FOREIGN KEY (source_account_id) REFERENCES accounts(id),

    CONSTRAINT fk_transactions_destination_account
        FOREIGN KEY (destination_account_id) REFERENCES accounts(id),

    CONSTRAINT chk_transactions_type
        CHECK (transaction_type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER', 'EXCHANGE')),

    CONSTRAINT chk_transactions_amount
        CHECK (amount > 0),

    CONSTRAINT chk_transactions_deposit_fields
        CHECK (
            transaction_type <> 'DEPOSIT'
            OR (
                source_account_id IS NULL
                AND destination_account_id IS NOT NULL
            )
        ),

    CONSTRAINT chk_transactions_withdrawal_fields
        CHECK (
            transaction_type <> 'WITHDRAWAL'
            OR (
                source_account_id IS NOT NULL
                AND destination_account_id IS NULL
            )
        ),

    CONSTRAINT chk_transactions_transfer_fields
        CHECK (
            transaction_type <> 'TRANSFER'
            OR (
                source_account_id IS NOT NULL
                AND destination_account_id IS NOT NULL
                AND source_account_id <> destination_account_id
            )
        ),

    CONSTRAINT chk_transactions_exchange_fields
        CHECK (
            transaction_type <> 'EXCHANGE'
            OR (
                source_account_id IS NOT NULL
                AND destination_account_id IS NOT NULL
                AND source_account_id <> destination_account_id
                AND source_amount IS NOT NULL
                AND source_amount > 0
                AND destination_amount IS NOT NULL
                AND destination_amount > 0
                AND from_currency IS NOT NULL
                AND to_currency IS NOT NULL
                AND from_currency <> to_currency
                AND exchange_rate IS NOT NULL
                AND exchange_rate > 0
            )
        )
);

CREATE TABLE audit_logs (
    id INT PRIMARY KEY AUTO_INCREMENT,
    action_name VARCHAR(100) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
