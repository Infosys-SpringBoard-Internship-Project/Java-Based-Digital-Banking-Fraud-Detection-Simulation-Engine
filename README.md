# FraudShield

FraudShield is a hybrid fraud-detection platform for digital banking that combines a Spring Boot backend, a Flask ML inference service, and a web dashboard for operations, simulation, and audit visibility.

## Table of contents

- [Overview](#overview)
- [Key features](#key-features)
- [Architecture](#architecture)
- [Project structure](#project-structure)
- [Tech stack](#tech-stack)
- [Getting started (local)](#getting-started-local)
- [Environment variables](#environment-variables)
- [Running with Docker](#running-with-docker)
- [Deploying on Render](#deploying-on-render)
- [API overview](#api-overview)
- [Documentation](#documentation)
- [Repository hygiene (.gitignore)](#repository-hygiene-gitignore)
- [Team](#team)
- [License](#license)

## Overview

The platform evaluates transactions using two layers:
- Rule engine (`R01` to `R14`) for deterministic, explainable checks.
- ML model scoring for probabilistic fraud detection.

It then stores results, raises alerts, supports CSV export, records audit/API logs, and provides dashboard analytics.

## Key features

- Hybrid fraud detection (rules + ML fallback/upgrade flow)
- Role-based access (`SUPERADMIN`, `ADMIN`, `ANALYST`)
- Manual validation + simulated traffic generation
- Alerts and optional email notifications
- Audit logs and API request logs
- Transaction analytics and export endpoints
- Render-ready Docker deployment for app + ML service

## Architecture

```text
Dashboard (HTML/CSS/JS)
        |
        v
Spring Boot API + Session/Auth + Rule Engine
        |
        +----> PostgreSQL (Supabase)
        |
        +----> Flask ML API (/health, /predict)
```

## Project structure

```text
fraud-project-source/
├── src/                                   # Spring Boot source
│   └── main/
│       ├── java/com/example/infosys_project/
│       └── resources/
│           ├── application.properties
│           ├── db/migration/
│           └── static/
├── ml/                                    # ML service and training
│   ├── api/flask_api.py
│   ├── train_model.py
│   ├── requirements.txt
│   ├── models/
│   ├── data/
│   └── tests/
├── e2e-tests/                             # Playwright tests
├── docs/
│   ├── architecture/PROJECT_DEEP_DIVE.md
│   └── guides/
│       ├── RENDER_DEPLOY_GUIDE.md
│       └── RUN_GUIDE.md
├── Dockerfile                             # Spring Boot image
├── ml/Dockerfile                          # ML image
├── render.yaml                            # Render blueprint (2 services)
├── run_project.sh                         # Local launcher (app + ML)
├── stop_project.sh                        # Local stop script
├── .env.example
├── .gitignore
├── LICENSE
├── license.txt
└── README.md
```

## Tech stack

- Backend: Java 17, Spring Boot 3
- Database: PostgreSQL (Supabase)
- ML service: Python 3, Flask, scikit-learn
- Frontend: HTML/CSS/JS static pages
- E2E tests: Playwright
- Deployment: Docker + Render

## Getting started (local)

### 1) Prerequisites

- Java 17+
- Maven 3.8+ (or `mvnw`)
- Python 3.10+
- PostgreSQL/Supabase credentials

### 2) Configure environment

```bash
cp .env.example .env.local
```

Update `.env.local` with real values.

### 3) Start full stack

```bash
./run_project.sh
```

### 4) Access app

- Home: `http://localhost:8080/pages/index.html`
- Login: `http://localhost:8080/pages/admin-login.html`
- Dashboard: `http://localhost:8080/pages/dashboard.html`

### 5) Stop services

```bash
./stop_project.sh
```

## Environment variables

Required (backend):
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `ML_API_URL`
- `ML_HEALTH_URL`

Optional:
- `MAIL_SENDER`
- `MAIL_PASSWORD`
- `ML_AUTOTRAIN_ENABLED`
- `ML_AUTOTRAIN_INTERVAL_MINUTES`
- `ML_AUTOTRAIN_BATCH_SIZE`
- `ML_AUTOTRAIN_MIN_RECORDS`

## Running with Docker

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

## Deploying on Render

Use Blueprint deployment from `render.yaml`.

Quick flow:
1. Push repository to GitHub.
2. In Render, choose **New + -> Blueprint**.
3. Select this repository.
4. Set app env vars (`SPRING_DATASOURCE_*`, `ML_API_URL`, `ML_HEALTH_URL`).

Full detailed guide: `docs/guides/RENDER_DEPLOY_GUIDE.md`.

## API overview

- Auth: `/auth/*`
- Transactions: `/transaction/*`
- Alerts: `/alerts/*`
- Simulation: `/simulation/*`
- System: `/system/*`
- Audit: `/audit/*`

For full flow and endpoint explanation, see `docs/architecture/PROJECT_DEEP_DIVE.md`.

## Documentation

- Full architecture and interview Q&A: `docs/architecture/PROJECT_DEEP_DIVE.md`
- Render deployment guide: `docs/guides/RENDER_DEPLOY_GUIDE.md`
- Run guide from zero: `docs/guides/RUN_GUIDE.md`

## Repository hygiene (.gitignore)

This repo excludes non-source and local-only artifacts, including:
- env files (`.env`, `.env.local`, `.env.*` except `.env.example`)
- build outputs (`target/`, `*.jar`, `*.class`)
- logs/runtime outputs (`*.log`, `*.out`, `*.pid`)
- local Python caches/venv
- local node modules and Playwright artifacts
- IDE/system files

## Team

| Name | GitHub |
|---|---|
| Team Member 1 | `@username1` |
| Team Member 2 | `@username2` |
| Team Member 3 | `@username3` |
| Team Member 4 | `@username4` |
| Team Member 5 | `@username5` |
| Team Member 6 | `@username6` |

## License

- Standard license file: `LICENSE`
- Project-requested license text copy: `license.txt`
