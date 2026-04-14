# FraudShield

AI-powered digital banking fraud detection and simulation platform built with Spring Boot, Flask, and PostgreSQL.

![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14%2B-blue)
![Python](https://img.shields.io/badge/Python-3.x-yellow)
![Flask](https://img.shields.io/badge/Flask-API-black)
![License](https://img.shields.io/badge/License-MIT-green)

## Local Endpoints

| Service | URL | Description |
|---------|-----|-------------|
| **Main Application** | `http://localhost:8080/pages/index.html` | Frontend landing page and Spring Boot API |
| **Dashboard** | `http://localhost:8080/pages/dashboard.html` | Fraud monitoring dashboard |
| **Swagger UI** | `http://localhost:8080/swagger-ui.html` | Interactive API documentation |
| **ML Health Check** | `http://127.0.0.1:5000/health` | Flask ML service health status |

## 🚀 Quick Start TL;DR

```bash
# 1. Clone and configure
git clone https://github.com/Infosys-SpringBoard-Internship-Project/Java-Based-Digital-Banking-Fraud-Detection-Simulation-Engine.git fraud-project-source
cd fraud-project-source
cp .env.example .env.local

# 2. Update .env.local with your database credentials

# 3. Run everything
./run_project.sh

# 4. Open the dashboard in your browser
# http://localhost:8080/pages/dashboard.html
```

## Table of Contents

- [Overview](#overview)
- [Problem Statement](#problem-statement)
- [Solution Approach](#solution-approach)
- [Key Features](#key-features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Database Schema](#database-schema)
- [Getting Started (Local)](#getting-started-local)
- [Environment Variables](#environment-variables)
- [Local Run Notes](#local-run-notes)
- [API Reference](#api-reference)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)
- [Security Notes](#security-notes)
- [Contributing](#contributing)
- [Team](#team)
- [License](#license)

## Overview

FraudShield helps operations teams detect suspicious digital banking transactions in near real time.

It combines:
- deterministic fraud rules (`R01` to `R14`) for explainable detection
- machine-learning scoring for probabilistic risk estimation

The platform includes dashboard analytics, alerting, audit trails, simulation workflows, and export utilities for investigation and compliance support.

## Problem Statement

Digital banking fraud is difficult to detect with only static thresholds or only black-box ML models.

Common operational issues include:
- delayed fraud identification
- low explainability for flagged transactions
- weak audit and investigation workflow
- fragmented tooling for monitoring, simulation, and alerting

## Solution Approach

FraudShield uses a hybrid pipeline:

1. Validate incoming transaction details.
2. Apply rule-based risk logic (`R01`-`R14`) to generate explainable signals.
3. Call Flask ML inference (`/predict`) to compute fraud probability.
4. Merge rule score + ML confidence for final risk classification.
5. Persist transaction, trigger alerts, and expose operational metrics.

This keeps detection both practical (explainable rules) and adaptive (ML-assisted scoring).

## Key Features

### Detection Engine
- **Hybrid fraud detection** - Combines rule-based logic (R01-R14) with ML probability scoring
- **Real-time validation** - Sub-second transaction processing with immediate risk assessment
- **Explainable results** - Each flagged transaction includes specific rule violations and risk reasons

### Access Control
- **Role-based access** - Three-tier authorization: SUPERADMIN, ADMIN, ANALYST
- **Data masking** - Automatic PII protection based on user role
- **Audit logging** - Comprehensive activity tracking for compliance

### Operations & Analytics
- **Dashboard analytics** - Visual fraud trends, transaction statistics, and system health
- **Risk-level alerting** - Configurable email notifications for high-risk transactions
- **CSV exports** - Transaction and audit log downloads for investigation
- **Fraud simulation** - Generate synthetic fraud scenarios for testing and training

### DevOps Ready

- **Environment-driven configuration** - Local services are controlled through documented environment variables
- **Health monitoring** - Dedicated endpoints for DB, ML, email, and API status
- **Swagger/OpenAPI docs** - Interactive API docs available at `/swagger-ui.html`

## Architecture

```text
┌─────────────────────────────────────────────────────────────┐
│                    Frontend Dashboard                       │
│              (HTML/CSS/JS + Chart.js + DataTables)          │
└────────────────────────┬────────────────────────────────────┘
                         │
                         v
┌─────────────────────────────────────────────────────────────┐
│              Spring Boot API (Port 8080)                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │  Auth        │  │  Transaction │  │  Simulation  │       │
│  │  Controller  │  │  Controller  │  │  Controller  │       │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘       │
│         │                  │                  │             │
│  ┌──────v──────────────────v──────────────────v────────┐    │
│  │           Fraud Detection Service                   │    │
│  │  (Rules Engine + ML Integration + Alert Manager)    │    │
│  └──────┬────────────────────────────────────┬─────────┘    │
└─────────┼────────────────────────────────────┼─────────────-┘
          │                                    │
          v                                    v
┌─────────────────────┐              ┌─────────────────────┐
│   PostgreSQL DB     │              │  Flask ML Service   │
│  (Supabase Pooler)  │              │    (Port 5000)      │
│                     │              │                     │
│  • Users & Auth     │              │  • /predict         │
│  • Transactions     │              │  • /health          │
│  • Alerts & Logs    │              │  • scikit-learn     │
└─────────────────────┘              └─────────────────────┘
```

## Tech Stack

- Backend: Java 17, Spring Boot 3
- ML Inference: Python 3, Flask, scikit-learn
- Database: PostgreSQL
- Build Tool: Maven
- Runtime: local Spring Boot app plus local Flask ML service

## Project Structure

```text
.
├── .mvn/wrapper/                                  # Maven wrapper metadata
├── src/main/java/com/example/infosys_project/   # Backend source
├── src/main/resources/
│   ├── application.properties
│   ├── schema.sql
│   ├── db/migration/
│   │   ├── V1__add_rbac.sql
│   │   ├── V2__password_reset_force_change.sql
│   │   └── V3__schema_alignment.sql
│   └── static/                                   # UI pages/scripts/styles
├── ml/
│   ├── api/flask_api.py
│   ├── train_model.py
│   ├── models/
│   ├── data/
│   └── requirements.txt
├── run_project.sh
├── stop_project.sh
├── .env.example
├── pom.xml
├── LICENSE
```

## Database Schema

The bootstrap schema lives in `src/main/resources/schema.sql`, and incremental changes are tracked in `src/main/resources/db/migration/`.

Core tables:

- `admin_users` for RBAC users, password rotation, and account lifecycle metadata.
- `transactions` for fraud evaluation inputs and final rule/ML risk results.
- `fraud_alerts` for investigator-facing alert records linked to transactions.
- `api_logs` and `audit_logs` for operational tracing and compliance history.
- `system_health` for the cached health snapshot surfaced in the admin dashboard.

Migration set:

- `V1__add_rbac.sql` adds RBAC fields and indexes for admin users.
- `V2__password_reset_force_change.sql` adds forced password rotation support.
- `V3__schema_alignment.sql` adds the missing admin metadata columns where needed, creates the `fraud_alerts -> transactions` foreign key, and seeds the default `system_health` row.

For a fresh database, `schema.sql` is sufficient. For an existing database, run the migrations in order.

## Getting Started (Local)

### Prerequisites

- Java 17+
- Maven 3.8+ or use the included `./mvnw`
- Python 3.10+

The launch scripts auto-detect:

- `python3` first, then `python`
- `./mvnw` first, then `mvn`

Optional overrides:

- `PYTHON_CMD=/path/to/python ./run_project.sh`
- `MAVEN_CMD=/path/to/mvn ./run_project.sh`

### Steps

1. Copy environment template:

```bash
cp .env.example .env.local
```

2. Configure `.env.local` values.

3. Start backend + ML service:

```bash
./run_project.sh
```

If your machine exposes Python as `python` instead of `python3`, the launcher will pick it automatically as long as it is Python 3.10+.

4. Open in browser:
- `http://localhost:8080/pages/index.html`
- `http://localhost:8080/pages/admin-login.html`
- `http://localhost:8080/pages/dashboard.html`

5. Stop services:

```bash
./stop_project.sh
```

## Environment Variables

| Variable | Required | Example | Purpose |
|---|---|---|---|
| `SPRING_DATASOURCE_URL` | Yes | `jdbc:postgresql://localhost:5432/fraud_db` | JDBC connection string for PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | Yes | `postgres` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | Yes | `<db-password>` | DB password |
| `ML_API_URL` | Yes | `http://127.0.0.1:5000/predict` | ML inference endpoint |
| `ML_HEALTH_URL` | Yes | `http://127.0.0.1:5000/health` | ML health endpoint |
| `MAIL_SENDER` | No | `alerts@example.com` | Sender email for alert notifications |
| `MAIL_PASSWORD` | No | `<app-password>` | App password for mail provider |

Notes:
- ML auto-train pipeline is removed.
- Application uses existing model artifacts under `ml/models/`.
- For local setup, the default ML service URL is `http://127.0.0.1:5000`.

## Local Run Notes

This repository is currently documented for local development only.

Local runtime expectations:

1. Provide the environment variables listed above.
2. Start PostgreSQL and create the target database before launching the app.
3. Run the Flask ML service separately or let `./run_project.sh` start it.
4. Verify `/system/health` and `/swagger-ui.html` after startup.

Important:

- Do not embed username/password inside `SPRING_DATASOURCE_URL`.
- Keep DB username/password in their dedicated env variables.
- Keep `.env.local` untracked and machine-specific.
- Use `.git/info/exclude` for local-only ignore rules in this workspace.

## API Reference

Base URL: `http://localhost:8080`

Swagger UI: `http://localhost:8080/swagger-ui.html` (local)

### Authentication

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `POST` | `/auth/login` | Authenticate user and get token | No |
| `POST` | `/auth/logout` | Invalidate session token | Yes |
| `GET` | `/auth/me` | Get current user profile | Yes |
| `POST` | `/auth/register` | Register new user (SUPERADMIN only) | Yes |
| `GET` | `/auth/users` | List all users | Yes (ADMIN+) |

### Transactions

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `POST` | `/transaction/validate` | Submit transaction for fraud detection | Optional |
| `GET` | `/transaction/all` | Get all transactions (with role-based masking) | Optional |
| `GET` | `/transaction/search` | Search transactions with filters | Optional |
| `GET` | `/transaction/{id}` | Get transaction by ID | Optional |
| `GET` | `/transaction/frauds` | Get all fraudulent transactions | Optional |
| `GET` | `/transaction/summary` | Get fraud statistics summary | Optional |
| `GET` | `/transaction/generate` | Generate random transaction (no save) | No |
| `GET` | `/transaction/autoValidate` | Generate + validate + save transaction | No |

### Alerts

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `GET` | `/alerts` | Get all alerts for current user | Yes |
| `GET` | `/alerts/unread` | Get unread alerts count | Yes |
| `PUT` | `/alerts/{id}/read` | Mark alert as read | Yes |
| `PUT` | `/alerts/read-all` | Mark all alerts as read | Yes |

### Simulation

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `POST` | `/simulation/start` | Start fraud simulation | Yes (ADMIN+) |
| `POST` | `/simulation/stop` | Stop running simulation | Yes (ADMIN+) |
| `GET` | `/simulation/status` | Get simulation status | Yes |

### System Health

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `GET` | `/system/health` | Get system health status (DB, ML, email) | No |
| `GET` | `/system/overview` | Get system metrics overview | Yes (ADMIN+) |
| `GET` | `/system/api-logs` | Get recent API request logs | Yes (ADMIN+) |

### Audit

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `GET` | `/audit/logs` | Get audit logs with filters | Yes (ADMIN+) |
| `GET` | `/audit/export-csv` | Export audit logs as CSV | Yes (ADMIN+) |

### Example Request

```bash
# Submit a transaction for fraud detection
curl -X POST http://localhost:8080/transaction/validate \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 5000.00,
    "merchantName": "Online Store",
    "location": "New York",
    "paymentMethod": "CREDIT_CARD"
  }'
```

### Example Response

```json
{
  "transaction": {
    "transactionId": "TXN123456",
    "amount": 5000.00,
    "status": "FRAUD",
    "riskScore": 85,
    "mlConfidence": 0.92,
    "fraudReasons": ["R03: Unusual amount for merchant", "R07: High ML fraud probability"]
  },
  "message": "Transaction flagged as potential fraud",
  "timestamp": "2024-03-27T10:30:00Z"
}
```

## Testing

### Run Unit Tests

```bash
./mvnw test
```

### Run Integration Tests

```bash
./mvnw verify
```

### Test ML Service

```bash
# Health check
curl http://localhost:5000/health

# Prediction test
curl -X POST http://localhost:5000/predict \
  -H "Content-Type: application/json" \
  -d '{"amount": 1000, "hour": 14, "merchant_category": "retail"}'
```

### Test Coverage

```bash
./mvnw clean test jacoco:report
```

View coverage report: `target/site/jacoco/index.html`

## Troubleshooting

- `ML: DOWN` while `/health` returns running:
  - verify `ML_API_URL` and `ML_HEALTH_URL` point to `http://127.0.0.1:5000`
  - start the ML service with `ml/run_ml.sh` or `./run_project.sh`

- `Unable to commit against JDBC Connection`:
  - verify PostgreSQL is running locally and accepting connections
  - recheck database name, host, port, username, and password

- `Authentication error ... no user`:
  - confirm the seeded admin user exists in `admin_users`

- App fails during DB initialization:
  - recheck datasource URL and credentials
  - verify the database exists before launching Spring Boot

## Security Notes

- Never commit `.env`, `.env.local`, or real credentials.
- Keep `src/main/resources/application.properties` limited to placeholders and non-secret defaults.
- Rotate DB and mail credentials if exposed.
- Use least-privilege DB credentials for any shared environment.

## Contributing

1. Create a feature branch.
2. Commit focused changes with clear commit messages.
3. Open a pull request to `develop` or `main` as per repository workflow.
4. Ensure schema/config changes are documented in README.

## Team

| Name | GitHub |
|---|---|
| Team Member 1 | [@advikagarwal](https://github.com/advikagarwal) |
| Team Member 2 | [@Shakthisri16](https://github.com/Shakthisri16) |
| Team Member 3 | [@tarakeshwararao-S](https://github.com/tarakeshwararao-S) |
| Team Member 4 | [@nishika701](https://github.com/nishika701) |
| Team Member 5 | [@jaswanth82006](https://github.com/jaswanth82006) |
| Team Member 6 | [@GNavya15](https://github.com/GNavya15) |

## License

- `LICENSE`
