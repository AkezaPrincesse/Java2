-- =====================================================
-- V5: Notifications, Audit Logs, Email Logs, Files, Settings
-- =====================================================

CREATE TABLE IF NOT EXISTS notifications (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT REFERENCES users(id),
    customer_id     BIGINT REFERENCES customers(id),
    type            VARCHAR(30) NOT NULL,
    title           VARCHAR(255) NOT NULL,
    message         TEXT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'UNREAD',
    email_sent      BOOLEAN NOT NULL DEFAULT FALSE,
    reference_link  VARCHAR(500),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    performed_by    VARCHAR(100) NOT NULL,
    performed_at    TIMESTAMP NOT NULL,
    action          VARCHAR(30) NOT NULL,
    entity_name     VARCHAR(100) NOT NULL,
    entity_id       VARCHAR(50),
    old_values      TEXT,
    new_values      TEXT,
    ip_address      VARCHAR(50),
    user_agent      VARCHAR(255),
    description     VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS email_logs (
    id              BIGSERIAL PRIMARY KEY,
    to_email        VARCHAR(150) NOT NULL,
    subject         VARCHAR(255) NOT NULL,
    template_name   VARCHAR(50) NOT NULL,
    sent            BOOLEAN NOT NULL,
    error_message   TEXT,
    sent_at         TIMESTAMP NOT NULL,
    reference_id    VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS file_uploads (
    id              BIGSERIAL PRIMARY KEY,
    file_name       VARCHAR(255) NOT NULL,
    original_name   VARCHAR(255) NOT NULL,
    content_type    VARCHAR(100) NOT NULL,
    file_size       BIGINT NOT NULL,
    file_path       VARCHAR(500) NOT NULL,
    entity_type     VARCHAR(50),
    entity_id       VARCHAR(50),
    deleted         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS system_settings (
    id              BIGSERIAL PRIMARY KEY,
    setting_key     VARCHAR(100) UNIQUE NOT NULL,
    setting_value   TEXT NOT NULL,
    description     VARCHAR(255),
    category        VARCHAR(50),
    editable        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_customer ON notifications(customer_id);
CREATE INDEX IF NOT EXISTS idx_notifications_status ON notifications(status);
CREATE INDEX IF NOT EXISTS idx_audit_logs_user ON audit_logs(performed_by);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs(entity_name, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_audit_logs_date ON audit_logs(performed_at);
CREATE INDEX IF NOT EXISTS idx_email_logs_email ON email_logs(to_email);
CREATE INDEX IF NOT EXISTS idx_settings_key ON system_settings(setting_key);
