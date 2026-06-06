-- =====================================================
-- V2: Customer and Meter Tables
-- =====================================================

CREATE TABLE IF NOT EXISTS customers (
    id              BIGSERIAL PRIMARY KEY,
    full_name       VARCHAR(100) NOT NULL,
    national_id     VARCHAR(20) UNIQUE NOT NULL,
    email           VARCHAR(150) UNIQUE NOT NULL,
    phone_number    VARCHAR(20) NOT NULL,
    address         VARCHAR(255) NOT NULL,
    district        VARCHAR(100),
    sector          VARCHAR(100),
    status          VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    user_id         BIGINT REFERENCES users(id),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS tariffs (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    description     VARCHAR(255),
    utility_type    VARCHAR(20) NOT NULL,
    tariff_type     VARCHAR(20) NOT NULL,
    flat_rate       NUMERIC(15,4),
    effective_date  DATE NOT NULL,
    expiry_date     DATE,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS meters (
    id                  BIGSERIAL PRIMARY KEY,
    meter_number        VARCHAR(50) UNIQUE NOT NULL,
    meter_type          VARCHAR(20) NOT NULL,
    installation_date   DATE NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    location            VARCHAR(255),
    initial_reading     NUMERIC(10,2) DEFAULT 0,
    customer_id         BIGINT NOT NULL REFERENCES customers(id),
    tariff_id           BIGINT REFERENCES tariffs(id),
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS meter_readings (
    id                  BIGSERIAL PRIMARY KEY,
    meter_id            BIGINT NOT NULL REFERENCES meters(id),
    previous_reading    NUMERIC(15,4) NOT NULL,
    current_reading     NUMERIC(15,4) NOT NULL,
    consumption         NUMERIC(15,4) NOT NULL,
    reading_date        DATE NOT NULL,
    reading_year        INT NOT NULL,
    reading_month       INT NOT NULL,
    notes               VARCHAR(255),
    reading_image_path  VARCHAR(255),
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    UNIQUE (meter_id, reading_year, reading_month)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_customers_email ON customers(email);
CREATE INDEX IF NOT EXISTS idx_customers_national_id ON customers(national_id);
CREATE INDEX IF NOT EXISTS idx_customers_status ON customers(status);
CREATE INDEX IF NOT EXISTS idx_meters_number ON meters(meter_number);
CREATE INDEX IF NOT EXISTS idx_meters_customer ON meters(customer_id);
CREATE INDEX IF NOT EXISTS idx_meters_status ON meters(status);
CREATE INDEX IF NOT EXISTS idx_readings_meter ON meter_readings(meter_id);
CREATE INDEX IF NOT EXISTS idx_readings_year_month ON meter_readings(reading_year, reading_month);
