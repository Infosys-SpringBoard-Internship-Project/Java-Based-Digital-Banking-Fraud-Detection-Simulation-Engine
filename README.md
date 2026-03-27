# FraudShield

AI-powered digital banking fraud detection and simulation platform built with Spring Boot, Flask, and PostgreSQL.

![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14%2B-blue)
![Python](https://img.shields.io/badge/Python-3.x-yellow)
![Flask](https://img.shields.io/badge/Flask-API-black)
![License](https://img.shields.io/badge/License-MIT-green)

## 🌐 Live Demo

| Service | URL | Description |
|---------|-----|-------------|
| **Main Application** | [fraudshield-app.onrender.com](https://fraudshield-app.onrender.com) | Frontend dashboard and Spring Boot API |
| **ML Service** | [fraudshield-ml-pwd9.onrender.com](https://fraudshield-ml-pwd9.onrender.com) | Flask ML inference endpoint |
| **ML Health Check** | [fraudshield-ml-pwd9.onrender.com/health](https://fraudshield-ml-pwd9.onrender.com/health) | ML service health status |

## 🚀 Quick Start TL;DR

```bash
# 1. Clone and configure
git clone <repo-url>
cd fraud-project-source
cp .env.example .env.local

# 2. Update .env.local with your database credentials

# 3. Run everything
./run_project.sh

# 4. Open dashboard
open http://localhost:8080/pages/dashboard.html
```

## Table of Contents

- [Overview](#overview)
- [Problem Statement](#problem-statement)
- [Solution Approach](#solution-approach)
- [Key Features](#key-features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started (Local)](#getting-started-local)
- [Environment Variables](#environment-variables)
- [Run with Docker](#run-with-docker)
- [Deploy on Render](#deploy-on-render)
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

## ✨ Key Features

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
- **Docker containerization** - Multi-stage builds for both Java and Python services
- **Health monitoring** - Dedicated endpoints for DB, ML, email, and API status
- **Production deployment** - Render blueprint for zero-config cloud deployment

## 🏗️ Architecture

```text
┌─────────────────────────────────────────────────────────────┐
│                    Frontend Dashboard                        │
│              (HTML/CSS/JS + Chart.js + DataTables)          │
└────────────────────────┬────────────────────────────────────┘
                         │
                         v
┌─────────────────────────────────────────────────────────────┐
│              Spring Boot API (Port 8080)                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │  Auth        │  │  Transaction │  │  Simulation  │     │
│  │  Controller  │  │  Controller  │  │  Controller  │     │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘     │
│         │                  │                  │              │
│  ┌──────v──────────────────v──────────────────v────────┐   │
│  │           Fraud Detection Service                    │   │
│  │  (Rules Engine + ML Integration + Alert Manager)    │   │
│  └──────┬────────────────────────────────────┬─────────┘   │
└─────────┼────────────────────────────────────┼─────────────┘
          │                                     │
          v                                     v
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
- Database: PostgreSQL (Supabase pooler)
- Build Tool: Maven
- Containers: Docker
- Hosting: Render

## Project Structure

```text
.
├── src/main/java/com/example/infosys_project/   # Backend source
├── src/main/resources/
│   ├── application.properties
│   ├── schema.sql
│   ├── db/migration/
│   └── static/                                   # UI pages/scripts/styles
├── ml/
│   ├── api/flask_api.py
│   ├── train_model.py
│   ├── models/
│   ├── data/
│   └── requirements.txt
├── Dockerfile
├── ml/Dockerfile
├── render.yaml
├── run_project.sh
├── stop_project.sh
├── .env.example
├── .gitignore
├── pom.xml
├── LICENSE
```

## Getting Started (Local)

### Prerequisites

- Java 17+
- Maven 3.8+
- Python 3.10+
- Docker (optional)

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
| `SPRING_DATASOURCE_URL` | Yes | `jdbc:postgresql://aws-1-ap-south-1.pooler.supabase.com:6543/postgres?sslmode=require` | JDBC connection string for PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | Yes | `postgres.ktmcqxdhjqjsltacucbo` | DB username (Supabase pooler format) |
| `SPRING_DATASOURCE_PASSWORD` | Yes | `<db-password>` | DB password |
| `ML_API_URL` | Yes | `https://fraudshield-ml-pwd9.onrender.com/predict` | ML inference endpoint |
| `ML_HEALTH_URL` | Yes | `https://fraudshield-ml-pwd9.onrender.com/health` | ML health endpoint |
| `MAIL_SENDER` | No | `alerts@example.com` | Sender email for alert notifications |
| `MAIL_PASSWORD` | No | `<app-password>` | App password for mail provider |

Notes:
- ML auto-train pipeline is removed.
- Application uses existing model artifacts under `ml/models/`.
- Always include protocol (`https://`) in ML URLs.

## 🐳 Run with Docker

### Build Images

```bash
docker build -t fraudshield-app .
docker build -t fraudshield-ml ./ml
```

### Run Services Individually

Run ML service:

```bash
docker run --rm -p 5000:5000 fraudshield-ml
```

Run app service:

```bash
docker run --rm -p 8080:8080 \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://<host>:5432/postgres?sslmode=require" \
  -e SPRING_DATASOURCE_USERNAME="postgres" \
  -e SPRING_DATASOURCE_PASSWORD="<password>" \
  -e ML_API_URL="http://host.docker.internal:5000/predict" \
  -e ML_HEALTH_URL="http://host.docker.internal:5000/health" \
  fraudshield-app
```

### Docker Compose (Recommended)

Create `docker-compose.yml`:

```yaml
version: '3.8'

services:
  ml-service:
    build: ./ml
    ports:
      - "5000:5000"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:5000/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  app-service:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/postgres?sslmode=require
      - SPRING_DATASOURCE_USERNAME=postgres
      - SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
      - ML_API_URL=http://ml-service:5000/predict
      - ML_HEALTH_URL=http://ml-service:5000/health
    depends_on:
      ml-service:
        condition: service_healthy
```

Run with:

```bash
docker-compose up -d
```

## Deploy on Render

1. Push repository to GitHub.
2. In Render, create **Blueprint** deployment from `render.yaml`.
3. Configure app environment variables.
4. Deploy ML service, then app service.

Recommended Render env values:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://aws-1-ap-south-1.pooler.supabase.com:6543/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=postgres.ktmcqxdhjqjsltacucbo
SPRING_DATASOURCE_PASSWORD=<your-db-password>
ML_API_URL=https://fraudshield-ml-pwd9.onrender.com/predict
ML_HEALTH_URL=https://fraudshield-ml-pwd9.onrender.com/health
```

Important:
- Do not embed username/password inside `SPRING_DATASOURCE_URL`.
- Keep DB username/password in their dedicated env variables.
- If ML URL changes, update both `ML_API_URL` and `ML_HEALTH_URL`.

## 🔌 API Reference

Base URL: `https://fraudshield-app.onrender.com` (production) or `http://localhost:8080` (local)

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
curl -X POST https://fraudshield-app.onrender.com/transaction/validate \
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

## 🧪 Testing

### Run Unit Tests

```bash
mvn test
```

### Run Integration Tests

```bash
mvn verify
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
mvn clean test jacoco:report
```

View coverage report: `target/site/jacoco/index.html`

## Troubleshooting

- `ML: DOWN` while `/health` returns running:
  - verify `ML_API_URL` and `ML_HEALTH_URL` include `https://`
  - redeploy `fraudshield-app`

- `Unable to commit against JDBC Connection`:
  - verify Supabase pooler host/port/username/password
  - ensure `sslmode=require`

- `Authentication error ... no user`:
  - confirm `SPRING_DATASOURCE_USERNAME` format is `postgres.<project-ref>`

- App fails during DB initialization:
  - recheck datasource URL and credentials
  - verify pooler connectivity from Render

## Security Notes

- Never commit `.env`, `.env.local`, or production secrets.
- Rotate DB and mail credentials if exposed.
- Use least-privilege DB credentials in production.
- Prefer secret managers and protected CI/CD variables.

## Contributing

1. Create a feature branch.
2. Commit focused changes with clear commit messages.
3. Open a pull request to `develop` or `main` as per repository workflow.
4. Ensure deployment/config changes are documented in README.

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
