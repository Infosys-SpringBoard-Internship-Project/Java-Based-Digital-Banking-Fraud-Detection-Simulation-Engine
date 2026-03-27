# FraudShield

AI-powered digital banking fraud detection and simulation platform built with Spring Boot, Flask, and PostgreSQL.

![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14%2B-blue)
![Python](https://img.shields.io/badge/Python-3.x-yellow)
![Flask](https://img.shields.io/badge/Flask-API-black)
![License](https://img.shields.io/badge/License-MIT-green)

## Live Links

- App: `https://fraudshield-app.onrender.com`
- ML Service: `https://fraudshield-ml-pwd9.onrender.com`
- ML Health: `https://fraudshield-ml-pwd9.onrender.com/health`

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
- [API Modules](#api-modules)
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

- Hybrid fraud detection (rules + ML)
- Role-based access (`SUPERADMIN`, `ADMIN`, `ANALYST`)
- Manual and simulated transaction validation
- Risk-level based alerting and fraud reasons
- System health visibility (DB/ML/email/API metrics)
- Audit logs and CSV exports for compliance workflows
- Production-ready deployment using Docker + Render blueprint

## Architecture

```text
Frontend Dashboard (HTML/CSS/JS)
          |
          v
Spring Boot API (Auth + Rules + Services)
      |                        |
      v                        v
PostgreSQL (Supabase)      Flask ML API (/health, /predict)
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
└── license.txt
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

## Run with Docker

Build images:

```bash
docker build -t fraudshield-app .
docker build -t fraudshield-ml ./ml
```

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

## API Modules

- Auth: `/auth/*`
- Transactions: `/transaction/*`
- Alerts: `/alerts/*`
- Simulation: `/simulation/*`
- System: `/system/*`
- Audit: `/audit/*`

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
- `license.txt`
