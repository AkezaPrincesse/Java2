-- =====================================================
-- V6: Default Roles, Permissions, Admin User, Settings
-- =====================================================

-- Permissions
INSERT INTO permissions (name, description, created_by) VALUES
('customer:read',   'View customers',            'SYSTEM'),
('customer:write',  'Create/update customers',   'SYSTEM'),
('meter:read',      'View meters',               'SYSTEM'),
('meter:write',     'Create/update meters',      'SYSTEM'),
('reading:write',   'Record meter readings',     'SYSTEM'),
('bill:read',       'View bills',                'SYSTEM'),
('bill:approve',    'Approve bills',             'SYSTEM'),
('payment:write',   'Process payments',          'SYSTEM'),
('report:read',     'View reports',              'SYSTEM'),
('admin:all',       'Full system administration','SYSTEM')
ON CONFLICT (name) DO NOTHING;

-- Roles
INSERT INTO roles (name, description, created_by) VALUES
('ROLE_ADMIN',    'System Administrator – full access',            'SYSTEM'),
('ROLE_OPERATOR', 'Field Operator – meter reading and management', 'SYSTEM'),
('ROLE_FINANCE',  'Finance Officer – billing and payments',        'SYSTEM'),
('ROLE_CUSTOMER', 'Customer – view own bills and payments',        'SYSTEM'),
('ROLE_MANAGER',  'Manager – view reports and manage resources',   'SYSTEM'),
('ROLE_USER',     'Default registered user',                      'SYSTEM')
ON CONFLICT (name) DO NOTHING;

-- Assign permissions to ROLE_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;

-- Assign permissions to ROLE_OPERATOR
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ROLE_OPERATOR'
AND p.name IN ('customer:read','meter:read','meter:write','reading:write','bill:read')
ON CONFLICT DO NOTHING;

-- Assign permissions to ROLE_FINANCE
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ROLE_FINANCE'
AND p.name IN ('customer:read','bill:read','bill:approve','payment:write','report:read')
ON CONFLICT DO NOTHING;

-- Assign permissions to ROLE_CUSTOMER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ROLE_CUSTOMER'
AND p.name IN ('bill:read')
ON CONFLICT DO NOTHING;

-- Assign permissions to ROLE_MANAGER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ROLE_MANAGER'
AND p.name IN ('customer:read','meter:read','bill:read','report:read')
ON CONFLICT DO NOTHING;

-- Default Admin User (password: Admin@1234! – BCrypt hashed)
INSERT INTO users (full_name, email, password, phone_number, enabled, account_non_locked, created_by)
VALUES (
    'System Administrator',
    'admin@wasac-reg.rw',
    '$2a$12$5NvuHF5FPdAk0.a8sFj6vOUbhfGTXE3tUBs2LKuLfXJBi4uqRe0Jm',
    '+250788000000',
    TRUE,
    TRUE,
    'SYSTEM'
) ON CONFLICT (email) DO NOTHING;

-- Assign ROLE_ADMIN to admin user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.email = 'admin@wasac-reg.rw' AND r.name = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;

-- Default System Settings
INSERT INTO system_settings (setting_key, setting_value, description, category, created_by) VALUES
('billing.grace_period_days',   '30',    'Grace period before penalties apply',        'BILLING',  'SYSTEM'),
('billing.late_penalty_rate',   '1.5',   'Monthly late payment penalty percentage',    'BILLING',  'SYSTEM'),
('billing.vat_rate',            '18',    'VAT rate percentage',                        'TAX',      'SYSTEM'),
('auth.max_login_attempts',     '5',     'Maximum failed login attempts before lock',  'SECURITY', 'SYSTEM'),
('auth.lock_duration_minutes',  '30',    'Account lock duration in minutes',           'SECURITY', 'SYSTEM'),
('notifications.email_enabled', 'true',  'Enable email notifications',                 'NOTIFY',   'SYSTEM'),
('system.company_name',         'WASAC & REG – Rwanda', 'Company display name',        'GENERAL',  'SYSTEM'),
('system.support_email',        'support@wasac-reg.rw', 'Support contact email',       'GENERAL',  'SYSTEM')
ON CONFLICT (setting_key) DO NOTHING;

-- Sample Water Tariff (FLAT)
INSERT INTO tariffs (name, description, utility_type, tariff_type, flat_rate, effective_date, active, created_by)
VALUES ('Water Standard Tariff', 'Standard water tariff – flat rate per m³',
        'WATER', 'FLAT', 250.00, '2024-01-01', TRUE, 'SYSTEM')
ON CONFLICT DO NOTHING;

-- Sample Electricity Tariff (TIERED)
INSERT INTO tariffs (name, description, utility_type, tariff_type, effective_date, active, created_by)
VALUES ('Electricity Tiered Tariff', 'Progressive electricity tariff by consumption tier',
        'ELECTRICITY', 'TIERED', '2024-01-01', TRUE, 'SYSTEM')
ON CONFLICT DO NOTHING;

-- Sample VAT Tax
INSERT INTO taxes (name, description, tax_type, utility_type, rate, active, applied_to_consumption, created_by)
VALUES
('Water VAT', 'VAT applied to water consumption', 'VAT', 'WATER', 18.0, TRUE, TRUE, 'SYSTEM'),
('Electricity VAT', 'VAT applied to electricity', 'VAT', 'ELECTRICITY', 18.0, TRUE, TRUE, 'SYSTEM')
ON CONFLICT DO NOTHING;

-- Sample Service Charges
INSERT INTO service_charges (name, description, charge_type, utility_type, amount, active, created_by)
VALUES
('Water Meter Maintenance', 'Monthly meter maintenance fee', 'MAINTENANCE', 'WATER', 500.00, TRUE, 'SYSTEM'),
('Electricity Service Fee', 'Monthly electricity service fee', 'FIXED', 'ELECTRICITY', 750.00, TRUE, 'SYSTEM')
ON CONFLICT DO NOTHING;

-- Sample Penalties
INSERT INTO penalties (name, description, utility_type, rate, grace_period_days, active, is_percentage, created_by)
VALUES
('Water Late Payment', 'Late payment penalty for water bills', 'WATER', 1.5, 30, TRUE, TRUE, 'SYSTEM'),
('Electricity Late Payment', 'Late payment penalty for electricity', 'ELECTRICITY', 2.0, 30, TRUE, TRUE, 'SYSTEM')
ON CONFLICT DO NOTHING;
