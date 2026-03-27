-- FraudShield RBAC Database Migration
-- Phase 1: Role-Based Access Control
-- Run this migration before starting the application with new features

-- 1. Add new columns to admin_users table
ALTER TABLE admin_users 
ADD COLUMN IF NOT EXISTS role VARCHAR(20) DEFAULT 'ANALYST',
ADD COLUMN IF NOT EXISTS email_alerts_enabled BOOLEAN DEFAULT true,
ADD COLUMN IF NOT EXISTS created_by VARCHAR(255),
ADD COLUMN IF NOT EXISTS can_be_deleted BOOLEAN DEFAULT true;

-- 2. Update existing admin to SUPERADMIN (first user becomes superadmin)
UPDATE admin_users 
SET role = 'SUPERADMIN', 
    can_be_deleted = false,
    email_alerts_enabled = true
WHERE id = (SELECT id FROM admin_users ORDER BY created_at ASC LIMIT 1);

-- 3. Set default values for existing users
UPDATE admin_users 
SET email_alerts_enabled = true
WHERE email_alerts_enabled IS NULL;

UPDATE admin_users 
SET can_be_deleted = true
WHERE can_be_deleted IS NULL AND role != 'SUPERADMIN';

-- 4. Add indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_admin_users_role ON admin_users(role);
CREATE INDEX IF NOT EXISTS idx_admin_users_created_by ON admin_users(created_by);

-- Verify migration
SELECT id, name, email, role, email_alerts_enabled, can_be_deleted, created_at 
FROM admin_users 
ORDER BY created_at ASC;
