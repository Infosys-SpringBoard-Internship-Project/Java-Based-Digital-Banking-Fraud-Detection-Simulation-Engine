-- Initial FraudShield schema.

CREATE TABLE IF NOT EXISTS admin_users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'ANALYST',
    email_alerts_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(255),
    can_be_deleted BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_admin_users_email ON admin_users (email);
CREATE INDEX IF NOT EXISTS idx_admin_users_active ON admin_users (is_active);
CREATE INDEX IF NOT EXISTS idx_admin_users_role ON admin_users (role);
CREATE INDEX IF NOT EXISTS idx_admin_users_created_by ON admin_users (created_by);

CREATE TABLE IF NOT EXISTS transactions (
    transaction_id VARCHAR(36) PRIMARY KEY,
    utr_number VARCHAR(32) NOT NULL UNIQUE,
    timestamp TIMESTAMP NOT NULL,
    account_holder_name VARCHAR(255) NOT NULL,
    mobile_number VARCHAR(20) NOT NULL,
    sender_account VARCHAR(34) NOT NULL,
    receiver_account VARCHAR(34) NOT NULL,
    bank_name VARCHAR(120) NOT NULL,
    account_age_days INTEGER NOT NULL DEFAULT 0,
    type VARCHAR(50) NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    balance DOUBLE PRECISION NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    merchant_category VARCHAR(120),
    merchant_id VARCHAR(120),
    transaction_mode VARCHAR(50),
    location VARCHAR(255),
    previous_location VARCHAR(255),
    is_new_location BOOLEAN NOT NULL DEFAULT FALSE,
    distance_from_last_txn_km DOUBLE PRECISION NOT NULL DEFAULT 0,
    device VARCHAR(120),
    is_new_device BOOLEAN NOT NULL DEFAULT FALSE,
    ip_address VARCHAR(64),
    is_vpn_or_proxy BOOLEAN NOT NULL DEFAULT FALSE,
    ip_country VARCHAR(120),
    ip_matches_location BOOLEAN NOT NULL DEFAULT TRUE,
    ip_risk_tag VARCHAR(50) NOT NULL DEFAULT 'CLEAN',
    txn_count_last_1hr INTEGER NOT NULL DEFAULT 0,
    txn_count_last_24hr INTEGER NOT NULL DEFAULT 0,
    avg_txn_amount_30days DOUBLE PRECISION NOT NULL DEFAULT 0,
    is_international BOOLEAN NOT NULL DEFAULT FALSE,
    is_first_time_receiver BOOLEAN NOT NULL DEFAULT FALSE,
    is_fraud BOOLEAN NOT NULL DEFAULT FALSE,
    fraud_reason VARCHAR(1000),
    risk_score DOUBLE PRECISION NOT NULL DEFAULT 0,
    risk_level VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    ml_fraud_probability DOUBLE PRECISION NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_transactions_timestamp ON transactions (timestamp);
CREATE INDEX IF NOT EXISTS idx_transactions_risk_level ON transactions (risk_level);
CREATE INDEX IF NOT EXISTS idx_transactions_is_fraud ON transactions (is_fraud);
CREATE INDEX IF NOT EXISTS idx_transactions_utr_number ON transactions (utr_number);

CREATE TABLE IF NOT EXISTS fraud_alerts (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(36) NOT NULL,
    account_holder_name VARCHAR(255) NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    risk_score DOUBLE PRECISION NOT NULL,
    fraud_reason VARCHAR(1000),
    location VARCHAR(255),
    ip_risk_tag VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_read BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_fraud_alerts_created_at ON fraud_alerts (created_at);
CREATE INDEX IF NOT EXISTS idx_fraud_alerts_is_read ON fraud_alerts (is_read);
CREATE INDEX IF NOT EXISTS idx_fraud_alerts_transaction_id ON fraud_alerts (transaction_id);

CREATE TABLE IF NOT EXISTS api_logs (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    method VARCHAR(10) NOT NULL,
    status_code INTEGER,
    response_time_ms BIGINT,
    user_email VARCHAR(255),
    ip_address VARCHAR(64),
    error_message VARCHAR(1000)
);

CREATE INDEX IF NOT EXISTS idx_api_logs_timestamp ON api_logs (timestamp);
CREATE INDEX IF NOT EXISTS idx_api_logs_endpoint ON api_logs (endpoint);
CREATE INDEX IF NOT EXISTS idx_api_logs_status_code ON api_logs (status_code);

CREATE TABLE IF NOT EXISTS system_health (
    id BIGINT PRIMARY KEY,
    last_update TIMESTAMP,
    db_status VARCHAR(20),
    ml_status VARCHAR(20),
    email_status VARCHAR(20),
    txn_processing_rate DOUBLE PRECISION,
    active_sessions INTEGER,
    error_count_1hr INTEGER
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    user_role VARCHAR(50),
    action_type VARCHAR(80) NOT NULL,
    target_entity VARCHAR(80),
    target_id VARCHAR(255),
    details VARCHAR(2000),
    ip_address VARCHAR(64),
    user_agent VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_timestamp ON audit_logs (timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_logs_user_email ON audit_logs (user_email);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action_type ON audit_logs (action_type);
