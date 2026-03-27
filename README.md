# FraudShield

FraudShield is a digital banking fraud detection and simulation engine built with Spring Boot and a Flask-based ML API.

## Overview

The system processes transactions using:
- Rule-based fraud checks (`R01` to `R14`)
- ML probability scoring from a separate inference service

It stores transaction decisions, raises alerts, tracks audit logs, and serves a dashboard.

## Key features

- Hybrid fraud detection (rules + ML)
- Role-based access (`SUPERADMIN`, `ADMIN`, `ANALYST`)
- Manual transaction validation and simulation
- Alerting and audit visibility
- CSV export and dashboard analytics
- Docker + Render deployment

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
.
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
│   └── data/
├── Dockerfile                             # Spring Boot image
├── ml/Dockerfile                          # ML image
├── render.yaml                            # Render blueprint
├── run_project.sh                         # Local launcher
├── stop_project.sh                        # Stop script
├── .env.example
├── .gitignore
├── LICENSE
├── license.txt
└── README.md
```

## Tech stack

- Java 17, Spring Boot 3
- PostgreSQL (Supabase)
- Python 3, Flask, scikit-learn
- Docker, Render

## Quick start (local)

1. Copy env template:

```bash
cp .env.example .env.local
```

2. Set required values in `.env.local`:
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `ML_API_URL`
- `ML_HEALTH_URL`

3. Start app + ML:

```bash
./run_project.sh
```

4. Open:
- `http://localhost:8080/pages/index.html`
- `http://localhost:8080/pages/admin-login.html`
- `http://localhost:8080/pages/dashboard.html`

5. Stop services:

```bash
./stop_project.sh
```

## Docker run

```bash
docker build -t fraudshield-app .
docker build -t fraudshield-ml ./ml
```

Run ML:

```bash
docker run --rm -p 5000:5000 fraudshield-ml
```

Run app:

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

1. Push repo to GitHub.
2. In Render: **New + -> Blueprint**.
3. Select this repo (uses `render.yaml`).
4. Configure env vars on app service:
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `ML_API_URL`
- `ML_HEALTH_URL`

## API overview

- Auth: `/auth/*`
- Transactions: `/transaction/*`
- Alerts: `/alerts/*`
- Simulation: `/simulation/*`
- System: `/system/*`
- Audit: `/audit/*`

## Repository hygiene

Ignored from Git by default:
- env files (except `.env.example`)
- build outputs (`target/`, jars, classes)
- logs and runtime outputs
- local caches/IDE files

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

- `LICENSE`
- `license.txt`
