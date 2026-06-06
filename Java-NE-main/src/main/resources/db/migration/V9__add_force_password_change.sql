-- Supports the first-login mandatory password change policy for admin-created accounts.
-- Default is FALSE so existing users are unaffected.
ALTER TABLE users ADD COLUMN IF NOT EXISTS force_password_change BOOLEAN NOT NULL DEFAULT FALSE;
