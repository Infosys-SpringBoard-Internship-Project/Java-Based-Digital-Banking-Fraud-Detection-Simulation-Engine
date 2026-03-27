# FraudShield

AI-powered digital banking fraud detection and simulation engine built with Spring Boot (backend), Flask (ML inference), and PostgreSQL.

## Badges

![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14%2B-blue)
![Python](https://img.shields.io/badge/Python-3.x-yellow)
![Flask](https://img.shields.io/badge/Flask-API-black)
![License](https://img.shields.io/badge/License-MIT-green)

## Table of contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Local Setup](#local-setup)
- [Environment Variables](#environment-variables)
- [Run with Docker](#run-with-docker)
- [Render Deployment](#render-deployment)
- [API Modules](#api-modules)
- [Render Troubleshooting](#render-troubleshooting)
- [Team](#team)
- [License](#license)

## Overview

FraudShield evaluates banking transactions using a hybrid approach:

- Rule engine (`R01` to `R14`) for deterministic risk checks
- ML inference service for fraud probability scoring

The platform provides dashboard monitoring, alerting, auditing, simulation, and export workflows for fraud operations.

## Key Features

- Hybrid fraud detection (rules + ML)
- Role-based access (`SUPERADMIN`, `ADMIN`, `ANALYST`)
- Manual and simulated transaction validation
- Real-time alerting and fraud log views
- Audit trail and CSV exports
- Deploy-ready via Docker + Render (`render.yaml`)

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

- Java 17, Spring Boot 3
- Python 3, Flask, scikit-learn
- PostgreSQL (Supabase-compatible)
- Maven
- Docker
- Render

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

## Local Setup

1. Copy environment template:

```bash
cp .env.example .env.local
```

2. Update required values in `.env.local`:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `ML_API_URL`
- `ML_HEALTH_URL`

3. Start app + ML service:

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

Required:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `ML_API_URL`
- `ML_HEALTH_URL`

Optional:

- `MAIL_SENDER`
- `MAIL_PASSWORD`

Notes:

- ML auto-train is removed.
- The app uses existing model artifacts from `ml/models/`.

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

## Render Deployment

1. Push this repo to GitHub.
2. In Render: **New + -> Blueprint**.
3. Select this repository (reads `render.yaml`).
4. Set environment variables on app service:
   - `SPRING_DATASOURCE_URL`
   - `SPRING_DATASOURCE_USERNAME`
   - `SPRING_DATASOURCE_PASSWORD`
   - `ML_API_URL`
   - `ML_HEALTH_URL`

Recommended values for Supabase pooler + Render:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://aws-1-ap-south-1.pooler.supabase.com:6543/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=postgres.ktmcqxdhjqjsltacucbo
SPRING_DATASOURCE_PASSWORD=<your-db-password>
ML_API_URL=https://fraudshield-ml-pwd9.onrender.com/predict
ML_HEALTH_URL=https://fraudshield-ml-pwd9.onrender.com/health
```

Important:
- Always include `https://` in `ML_API_URL` and `ML_HEALTH_URL`.
- Keep username/password as separate env vars (do not embed in JDBC URL).

## API Modules

- Auth: `/auth/*`
- Transactions: `/transaction/*`
- Alerts: `/alerts/*`
- Simulation: `/simulation/*`
- System: `/system/*`
- Audit: `/audit/*`

## Render Troubleshooting

- `ML: DOWN` while ML health URL works: verify app env has full URL with `https://` and redeploy app.
- `Unable to commit against JDBC Connection`: recheck Supabase pooler host/port/user/password.
- `Authentication error ... no user`: confirm `SPRING_DATASOURCE_USERNAME` is exactly `postgres.<project-ref>` for pooler.
- App starts then fails during DB init: verify `SPRING_DATASOURCE_URL` uses the pooler endpoint and `sslmode=require`.

## Team

| Name | GitHub |
|---|---|
| Team Member 1 | [@username1](https://github.com/username1) |
| Team Member 2 | [@username2](https://github.com/username2) |
| Team Member 3 | [@username3](https://github.com/username3) |
| Team Member 4 | [@username4](https://github.com/username4) |
| Team Member 5 | [@username5](https://github.com/username5) |
| Team Member 6 | [@username6](https://github.com/username6) |

> Replace `username1..username6` with the actual GitHub usernames.

## License

- `LICENSE`
- `license.txt`
