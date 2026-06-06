-- =====================================================
-- V4: Billing Cycles, Bills, Bill Items
-- =====================================================

CREATE TABLE IF NOT EXISTS billing_cycles (
    id              BIGSERIAL PRIMARY KEY,
    billing_year    INT NOT NULL,
    billing_month   INT NOT NULL,
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    due_date        DATE NOT NULL,
    closed          BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    UNIQUE (billing_year, billing_month)
);

CREATE TABLE IF NOT EXISTS bills (
    id                      BIGSERIAL PRIMARY KEY,
    bill_number             VARCHAR(30) UNIQUE NOT NULL,
    customer_id             BIGINT NOT NULL REFERENCES customers(id),
    meter_id                BIGINT NOT NULL REFERENCES meters(id),
    billing_cycle_id        BIGINT NOT NULL REFERENCES billing_cycles(id),
    meter_reading_id        BIGINT REFERENCES meter_readings(id),
    utility_type            VARCHAR(20) NOT NULL,
    consumption_amount      NUMERIC(15,4) NOT NULL DEFAULT 0,
    service_charge_amount   NUMERIC(15,4) NOT NULL DEFAULT 0,
    tax_amount              NUMERIC(15,4) NOT NULL DEFAULT 0,
    penalty_amount          NUMERIC(15,4) NOT NULL DEFAULT 0,
    total_amount            NUMERIC(15,4) NOT NULL,
    paid_amount             NUMERIC(15,4) NOT NULL DEFAULT 0,
    balance_amount          NUMERIC(15,4) NOT NULL DEFAULT 0,
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    bill_date               DATE NOT NULL,
    due_date                DATE NOT NULL,
    approved_at             TIMESTAMP,
    approved_by             VARCHAR(100),
    paid_at                 TIMESTAMP,
    notes                   VARCHAR(500),
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP,
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS bill_items (
    id              BIGSERIAL PRIMARY KEY,
    bill_id         BIGINT NOT NULL REFERENCES bills(id) ON DELETE CASCADE,
    item_type       VARCHAR(30) NOT NULL,
    description     VARCHAR(150) NOT NULL,
    quantity        NUMERIC(15,4),
    unit_price      NUMERIC(15,4),
    amount          NUMERIC(15,4) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS payments (
    id                      BIGSERIAL PRIMARY KEY,
    receipt_number          VARCHAR(30) UNIQUE NOT NULL,
    bill_id                 BIGINT NOT NULL REFERENCES bills(id),
    customer_id             BIGINT NOT NULL REFERENCES customers(id),
    amount                  NUMERIC(15,4) NOT NULL,
    payment_method          VARCHAR(30) NOT NULL,
    payment_status          VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    payment_date            TIMESTAMP NOT NULL,
    transaction_reference   VARCHAR(100),
    paid_by                 VARCHAR(100),
    notes                   VARCHAR(500),
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP,
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_bills_number ON bills(bill_number);
CREATE INDEX IF NOT EXISTS idx_bills_customer ON bills(customer_id);
CREATE INDEX IF NOT EXISTS idx_bills_status ON bills(status);
CREATE INDEX IF NOT EXISTS idx_bills_cycle ON bills(billing_cycle_id);
CREATE INDEX IF NOT EXISTS idx_bills_due_date ON bills(due_date);
CREATE INDEX IF NOT EXISTS idx_payments_receipt ON payments(receipt_number);
CREATE INDEX IF NOT EXISTS idx_payments_bill ON payments(bill_id);
CREATE INDEX IF NOT EXISTS idx_payments_customer ON payments(customer_id);
CREATE INDEX IF NOT EXISTS idx_payments_date ON payments(payment_date);
