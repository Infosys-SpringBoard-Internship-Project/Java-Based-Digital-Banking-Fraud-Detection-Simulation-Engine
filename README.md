# FraudShield

AI-powered digital banking fraud detection and simulation engine built with Spring Boot, Flask, and PostgreSQL.

## Live Demo

- App: `https://fraudshield-app.onrender.com`
- ML Service: `https://fraudshield-ml-pwd9.onrender.com`
- ML Health: `https://fraudshield-ml-pwd9.onrender.com/health`

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Environment Variables](#environment-variables)
- [Run with Docker](#run-with-docker)
- [Deploy on Render](#deploy-on-render)
- [API Modules](#api-modules)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [Team](#team)
- [License](#license)

## Overview

FraudShield evaluates transactions with a hybrid approach:

- Rule engine (`R01` to `R14`) for deterministic fraud checks
- ML inference API for fraud probability scoring

The platform supports operations teams with alerting, dashboards, audit logs, simulation workflows, and transaction exports.

## Features

- Hybrid fraud detection (rules + ML scoring)
- Role-based access (`SUPERADMIN`, `ADMIN`, `ANALYST`)
- Manual and simulated transaction validation
- Fraud alerts and risk-level classification
- Dashboard analytics and audit trail
- Deploy-ready setup using Docker and Render

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

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- Python 3.10+
- Docker (optional)

### Local Setup

1. Copy environment template:

```bash
cp .env.example .env.local
```

2. Update required values in `.env.local`.

3. Start the application and ML service:

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

### Required

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `ML_API_URL`
- `ML_HEALTH_URL`

### Optional

- `MAIL_SENDER`
- `MAIL_PASSWORD`

Notes:

- ML auto-train is removed.
- The application uses existing model artifacts from `ml/models/`.

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
2. In Render, create a **Blueprint** deployment using `render.yaml`.
3. Configure app environment variables.

Recommended configuration:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://aws-1-ap-south-1.pooler.supabase.com:6543/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=postgres.ktmcqxdhjqjsltacucbo
SPRING_DATASOURCE_PASSWORD=<your-db-password>
ML_API_URL=https://fraudshield-ml-pwd9.onrender.com/predict
ML_HEALTH_URL=https://fraudshield-ml-pwd9.onrender.com/health
```

Important:

- Always include `https://` in `ML_API_URL` and `ML_HEALTH_URL`.
- Keep username/password separate; do not embed credentials inside `SPRING_DATASOURCE_URL`.

## API Modules

- Auth: `/auth/*`
- Transactions: `/transaction/*`
- Alerts: `/alerts/*`
- Simulation: `/simulation/*`
- System: `/system/*`
- Audit: `/audit/*`

## Troubleshooting

- `ML: DOWN` while health endpoint is up: verify ML URLs in app env include protocol and redeploy app.
- `Unable to commit against JDBC Connection`: recheck Supabase pooler host/port/user/password.
- `Authentication error ... no user`: ensure `SPRING_DATASOURCE_USERNAME` matches pooler format (`postgres.<project-ref>`).
- DB init failures: confirm `sslmode=require` in datasource URL.

## Contributing

1. Create a feature branch.
2. Commit focused changes with clear messages.
3. Open a pull request to `develop` or `main` based on repo workflow.

## Team

| Name | GitHub |
|---|---|
| Team Member 1 | [@username1](https://github.com/username1) |
| Team Member 2 | [@username2](https://github.com/username2) |
| Team Member 3 | [@username3](https://github.com/username3) |
| Team Member 4 | [@username4](https://github.com/username4) |
| Team Member 5 | [@username5](https://github.com/username5) |
| Team Member 6 | [@username6](https://github.com/username6) |

> Replace `username1..username6` with actual GitHub usernames.

## License

- `LICENSE`
- `license.txt`
