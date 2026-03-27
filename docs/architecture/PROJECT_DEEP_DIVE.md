# FraudShield Project Deep Dive

This document explains the project in detail for demos, interviews, panel discussions, and technical handover.

## 1) What this project solves

FraudShield detects suspicious digital banking transactions using a hybrid model:
- deterministic rule engine (R01-R14)
- machine-learning probability scoring

The system then:
- classifies risk (`NORMAL`, `MEDIUM`, `HIGH`, `CRITICAL`)
- stores transactions and alerts
- supports dashboard analytics, simulation, and exports
- tracks user actions and API logs for auditability

## 2) High-level architecture

### Backend (Spring Boot)
- Serves REST APIs and static dashboard pages.
- Handles authentication, session management, role-based authorization.
- Executes rule-based fraud logic + ML score integration.
- Persists data to PostgreSQL.

### ML service (Flask)
- Exposes `/health` and `/predict`.
- Loads trained artifacts (`fraud_model.pkl`, `rule_model.pkl`, `encoders.json`).
- Returns fraud probability, ML risk level, and fired rule flags.

### Data layer (PostgreSQL/Supabase)
Main tables:
- `transactions`
- `fraud_alerts`
- `admin_users`
- `api_logs`
- `audit_logs`
- `system_health`

### UI
- HTML/CSS/JS pages under `src/main/resources/static/pages/`.
- Calls Spring APIs directly.

## 3) Core request flow (transaction validation)

1. Client submits transaction to `POST /transaction/validate`.
2. `TransactionService.processTransaction(...)` normalizes default fields.
3. `TransactionValidator` validates required fields and format.
4. `FraudDetector.checkFraud(...)` applies rules `R01-R14`, computes score and risk.
5. Service calls ML API (`ml.api.url`) and gets `fraud_probability`.
6. If ML score is high (>= 0.75), transaction can be escalated to fraud/high risk.
7. Transaction is saved to DB.
8. If risk is high/critical, email alert can be sent.
9. Fraud transactions create a `fraud_alerts` row.
10. Controller returns a structured `ValidationResponse`.

## 4) Fraud logic in detail

## 4.1 Rule engine
Rules include:
- large amount thresholds
- odd-hour behavior
- balance-drain patterns
- transaction velocity spikes
- high-risk merchants (crypto/gambling/darkweb)
- location jump/impossible travel
- new device signals
- VPN/proxy/TOR/IP mismatch signals
- international anomalies
- first-time receiver high amount
- amount spike vs 30-day baseline
- new account large transfer
- high daily volume
- round amount structuring

Output from rules:
- `riskScore` (0 to 10, capped)
- `riskLevel`
- `isFraud`
- `fraudReason` string with rule tags

## 4.2 ML scoring
`TransactionService` sends engineered features to Flask `/predict`:
- amount/balance
- velocity features
- account age
- location/device booleans
- network and geo fields
- merchant/mode categorical features

Flask returns:
- `fraud_probability`
- `ml_risk_level`
- `is_fraud_ml`
- `fraud_reason`
- `fired_rules`

Backend merges this with rules and escalates risk if needed.

## 5) Security and access control

## 5.1 Session model
- Login generates token stored in memory in `AuthService`.
- Token accepted via `Authorization: Bearer <token>` or `fraud_session` cookie.
- Session expiry: 8 hours.

## 5.2 Request guard
`SessionFilter` enforces:
- public vs protected routes
- page-level role checks
- API write restrictions for ANALYST role

## 5.3 Role model
Roles:
- `SUPERADMIN`
- `ADMIN`
- `ANALYST`

Permission helper: `RoleChecker`.

## 5.4 Data masking
For ANALYST views, sensitive fields are masked via `DataMaskingUtil`:
- account holder name
- mobile number
- sender/receiver account
- IP address

## 6) Observability and audit

## 6.1 API logs
`ApiLogInterceptor` captures:
- endpoint
- method
- status code
- response time
- IP
- user email if authenticated

## 6.2 Audit logs
`AuditService` records important actions:
- login/logout
- user creation/deletion/update
- simulation start/stop
- CSV exports
- credential changes

## 6.3 Health monitoring
`HealthMonitorService` updates system status every minute:
- DB status
- ML status
- email status
- tx processing rate
- active sessions
- error count in last hour

## 7) Background jobs

- Auto transaction generation (`AutoTransactionGenerationService`) on random intervals.
- ML auto-retrain scheduler (`HealthMonitorService.maybeRetrainMl`) with thresholds and intervals.
- Retention cleanup (`TransactionRetentionService`) every 6 hours; deletes transactions older than 14 days.

## 8) Endpoint map (major APIs)

## 8.1 Authentication `/auth`
- `POST /auth/login`
- `POST /auth/logout`
- `GET /auth/me`
- `GET /auth/bootstrap-status`
- `GET /auth/must-change-password`
- `POST /auth/register`
- `POST /auth/forgot-password`
- `PUT /auth/update-credentials`
- `GET /auth/users`
- `POST /auth/create-user`
- `PUT /auth/users/{id}`
- `DELETE /auth/users/{id}`
- `PUT /auth/toggle-alerts`

## 8.2 Transactions `/transaction`
- `GET /transaction/generate`
- `GET /transaction/autoValidate`
- `POST /transaction/validate`
- `GET /transaction/all`
- `GET /transaction/search`
- `GET /transaction/{id}`
- `GET /transaction/detail/{id}`
- `GET /transaction/frauds`
- `GET /transaction/by-risk/{level}`
- `GET /transaction/by-ip-tag/{tag}`
- `GET /transaction/summary`
- `GET /transaction/analytics`
- `GET /transaction/system-status`
- `GET /transaction/export-csv`
- `GET /transaction/export-csv/search`

## 8.3 Alerts `/alerts`
- `GET /alerts`
- `GET /alerts/unread`
- `GET /alerts/count`
- `PUT /alerts/{id}/read`
- `PUT /alerts/read-all`

## 8.4 Simulation `/simulation`
- `POST /simulation/start`
- `POST /simulation/stop`
- `GET /simulation/status`

## 8.5 Audit `/audit`
- `GET /audit/logs`
- `GET /audit/export-csv`

## 8.6 System `/system`
- `GET /system/health`
- `GET /system/api-logs`
- `GET /system/overview`
- `GET /system/auto-generation`
- `POST /system/auto-generation/start`
- `POST /system/auto-generation/stop`

## 9) Why these technologies were used

- Spring Boot: rapid REST API and production conventions.
- PostgreSQL/Supabase: reliable relational data and SQL analytics.
- Flask + scikit-learn: lightweight model serving and retraining workflow.
- Docker + Render: simple two-service cloud deployment.
- Playwright tests: realistic browser-level validation.

## 10) End-to-end working flow (business)

1. Admin logs in.
2. Transactions come from manual input, auto-generation, or simulation.
3. Rule engine + ML evaluate each transaction.
4. Risk and fraud decision stored.
5. High risk triggers alert and optional mail notifications.
6. Dashboard visualizes metrics, trends, and distributions.
7. Teams export transactions/audit logs for compliance and review.

## 11) Common internship panel questions and strong answers

### Q1) Why combine rules and ML?
Rules provide explainability and deterministic control. ML captures non-linear patterns and improves recall. Hybrid improves reliability and interpretability.

### Q2) How do you avoid full dependency on ML service?
Backend rule engine is primary and always available. If ML API is down, backend falls back safely and still processes transactions.

### Q3) How do you enforce role-based access?
`SessionFilter` + `RoleChecker` enforce endpoint/page permissions. ANALYST is read-only for most write APIs.

### Q4) How do you support compliance/audit requirements?
Two levels: request-level API logs and business-action audit logs with actor, action, target, timestamp, and metadata.

### Q5) How do you protect sensitive user data?
For ANALYST role, fields are masked before response; raw PII is not exposed in those views.

### Q6) How do you monitor system health?
Scheduled health snapshots track DB, ML, email, active sessions, API error count, and transaction throughput.

### Q7) How is model retraining handled?
Auto-train scheduler exports recent transaction data to CSV, runs Python training, validates artifacts, then confirms ML health.

### Q8) What are current limitations?
In-memory sessions are single-instance friendly but not distributed. For large-scale production, move sessions to Redis/JWT and add centralized secrets management and observability stack.

### Q9) How does deployment work on Render?
Two Docker web services defined in `render.yaml`: ML service and app service. App calls ML via configured service URL.

### Q10) If you had 2 more weeks, what would you improve?
- Replace in-memory session store with Redis or stateless JWT.
- Add OpenAPI docs and stricter API contracts.
- Add CI pipeline with automated integration tests.
- Add model version registry and drift monitoring.

## 12) Demo script you can use in panel

1. Login as admin.
2. Open dashboard and system health panel.
3. Submit a manual suspicious transaction.
4. Show result risk/fraud reason and new alert.
5. Open audit logs and show tracked action.
6. Run a short simulation and show status updates.
7. Export filtered CSV and explain governance value.
