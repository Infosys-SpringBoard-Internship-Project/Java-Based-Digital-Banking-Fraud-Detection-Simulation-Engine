ALTER TABLE admin_users
ADD COLUMN IF NOT EXISTS role VARCHAR(50) NOT NULL DEFAULT 'ANALYST',
ADD COLUMN IF NOT EXISTS email_alerts_enabled BOOLEAN NOT NULL DEFAULT TRUE,
ADD COLUMN IF NOT EXISTS created_by VARCHAR(255),
ADD COLUMN IF NOT EXISTS can_be_deleted BOOLEAN NOT NULL DEFAULT TRUE,
ADD COLUMN IF NOT EXISTS last_login TIMESTAMP NULL,
ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE,
ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_admin_users_created_by ON admin_users (created_by);

ALTER TABLE fraud_alerts
DROP CONSTRAINT IF EXISTS fk_fraud_alerts_transaction;

ALTER TABLE fraud_alerts
ADD CONSTRAINT fk_fraud_alerts_transaction
FOREIGN KEY (transaction_id) REFERENCES transactions (transaction_id)
ON DELETE CASCADE;

INSERT INTO system_health (
    id,
    last_update,
    db_status,
    ml_status,
    email_status,
    txn_processing_rate,
    active_sessions,
    error_count_1hr
)
SELECT 1, CURRENT_TIMESTAMP, 'UNKNOWN', 'UNKNOWN', 'UNKNOWN', 0, 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM system_health WHERE id = 1
);