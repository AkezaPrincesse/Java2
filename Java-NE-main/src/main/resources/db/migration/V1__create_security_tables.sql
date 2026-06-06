-- =====================================================
-- V1: Security Tables (Users, Roles, Permissions)
-- =====================================================

CREATE TABLE IF NOT EXISTS permissions (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) UNIQUE NOT NULL,
    description     VARCHAR(255),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS roles (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(50) UNIQUE NOT NULL,
    description     VARCHAR(255),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id         BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id   BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS users (
    id                          BIGSERIAL PRIMARY KEY,
    full_name                   VARCHAR(100) NOT NULL,
    email                       VARCHAR(150) UNIQUE NOT NULL,
    password                    VARCHAR(255) NOT NULL,
    phone_number                VARCHAR(20),
    enabled                     BOOLEAN NOT NULL DEFAULT FALSE,
    account_non_locked          BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_expired         BOOLEAN NOT NULL DEFAULT TRUE,
    credentials_non_expired     BOOLEAN NOT NULL DEFAULT TRUE,
    failed_login_attempts       INT NOT NULL DEFAULT 0,
    lock_time                   TIMESTAMP,
    last_login                  TIMESTAMP,
    created_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP,
    created_by                  VARCHAR(100),
    updated_by                  VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id         BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id              BIGSERIAL PRIMARY KEY,
    token           VARCHAR(500) UNIQUE NOT NULL,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expiry_date     TIMESTAMP NOT NULL,
    revoked         BOOLEAN NOT NULL DEFAULT FALSE,
    ip_address      VARCHAR(50),
    user_agent      VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS verification_tokens (
    id              BIGSERIAL PRIMARY KEY,
    token           VARCHAR(255) UNIQUE NOT NULL,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expiry_date     TIMESTAMP NOT NULL,
    used            BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id              BIGSERIAL PRIMARY KEY,
    token           VARCHAR(255) UNIQUE NOT NULL,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expiry_date     TIMESTAMP NOT NULL,
    used            BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS otp_codes (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(10) NOT NULL,
    email           VARCHAR(150) NOT NULL,
    purpose         VARCHAR(50) NOT NULL,
    generated_at    TIMESTAMP NOT NULL,
    expiry_at       TIMESTAMP NOT NULL,
    used            BOOLEAN NOT NULL DEFAULT FALSE,
    attempt_count   INT NOT NULL DEFAULT 0
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_otp_email_purpose ON otp_codes(email, purpose);
CREATE INDEX IF NOT EXISTS idx_verification_token ON verification_tokens(token);
CREATE INDEX IF NOT EXISTS idx_reset_token ON password_reset_tokens(token);
