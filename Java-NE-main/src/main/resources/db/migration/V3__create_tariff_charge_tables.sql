-- =====================================================
-- V3: Tariff Versions, Service Charges, Taxes, Penalties
-- =====================================================

CREATE TABLE IF NOT EXISTS tariff_versions (
    id              BIGSERIAL PRIMARY KEY,
    tariff_id       BIGINT NOT NULL REFERENCES tariffs(id) ON DELETE CASCADE,
    tier_order      INT NOT NULL,
    min_units       NUMERIC(15,4),
    max_units       NUMERIC(15,4),
    rate_per_unit   NUMERIC(15,4) NOT NULL,
    effective_date  DATE NOT NULL,
    expiry_date     DATE,
    description     VARCHAR(255),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS service_charges (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    description     VARCHAR(255),
    charge_type     VARCHAR(30) NOT NULL,
    utility_type    VARCHAR(20) NOT NULL,
    amount          NUMERIC(15,4) NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS taxes (
    id                      BIGSERIAL PRIMARY KEY,
    name                    VARCHAR(100) NOT NULL,
    description             VARCHAR(255),
    tax_type                VARCHAR(30) NOT NULL,
    utility_type            VARCHAR(20) NOT NULL,
    rate                    NUMERIC(7,4) NOT NULL,
    active                  BOOLEAN NOT NULL DEFAULT TRUE,
    applied_to_consumption  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP,
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS penalties (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(100) NOT NULL,
    description         VARCHAR(255),
    utility_type        VARCHAR(20) NOT NULL,
    rate                NUMERIC(7,4) NOT NULL,
    grace_period_days   INT NOT NULL DEFAULT 30,
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    is_percentage       BOOLEAN NOT NULL DEFAULT TRUE,
    fixed_amount        NUMERIC(15,4),
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_tariff_versions_tariff ON tariff_versions(tariff_id);
CREATE INDEX IF NOT EXISTS idx_service_charges_type ON service_charges(utility_type, active);
CREATE INDEX IF NOT EXISTS idx_taxes_type ON taxes(utility_type, active);
CREATE INDEX IF NOT EXISTS idx_penalties_type ON penalties(utility_type, active);
