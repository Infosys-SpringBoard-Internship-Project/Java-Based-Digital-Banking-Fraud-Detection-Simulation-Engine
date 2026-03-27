# FraudShield

AI-powered digital banking fraud detection and simulation engine with a Spring Boot application and a Flask-based ML inference service.

## Why this repository is now GitHub + Render ready

- Deployment uses two Docker web services via `render.yaml`:
  - `fraudshield-app` (Spring Boot)
  - `fraudshield-ml` (Flask ML API)
- Docker runtime now respects Render `PORT` for both services.
- Repository now excludes generated artifacts, logs, cache folders, and local dependency folders from Git.
- Docs are organized under `docs/`.

## Tech stack

- Backend: Java 17, Spring Boot 3
- Database: PostgreSQL (Supabase)
- ML service: Python 3, Flask, scikit-learn
- Frontend: HTML/CSS/JS (served by Spring Boot static resources)
- Deployment: Render (Docker)

## Project structure

```text
fraud-project-source/
├── src/                          # Spring Boot app source
│   └── main/
│       ├── java/com/example/infosys_project/
│       └── resources/
│           ├── application.properties
│           ├── db/migration/
│           └── static/
├── ml/                           # ML inference and training
│   ├── api/flask_api.py
│   ├── train_model.py
│   ├── requirements.txt
│   ├── models/
│   ├── data/
│   └── tests/
├── e2e-tests/                    # Playwright tests
├── docs/
│   ├── PROJECT_DEEP_DIVE.md
│   ├── RENDER_DEPLOY_GUIDE.md
│   ├── RUN_GUIDE.md
│   └── TEST_RESULTS.md
├── Dockerfile                    # Spring Boot image
├── ml/Dockerfile                 # ML service image
├── render.yaml                   # Render blueprint
├── run_project.sh                # Local launcher (app + ML)
├── stop_project.sh               # Local stop script
├── .env.example
├── .gitignore
├── license.txt
└── README.md
```

## Files/folders you should NOT upload to GitHub

These are already handled by `.gitignore`:

- Secrets/env: `.env`, `.env.local`, `.env.*` (except `.env.example`)
- Build output: `target/`, `*.jar`, `*.class`
- Logs/runtime files: `*.log`, `*.out`, `*.pid`, `run.out`
- Python local cache/venv: `.venv/`, `__pycache__/`, `.pytest_cache/`
- E2E local dependencies: `e2e-tests/node_modules/`, Playwright report folders
- IDE/system files: `.idea/`, `.vscode/`, `.settings/`, `.classpath`, `.project`

## What you must update before running locally

1. Copy env template:

```bash
cp .env.example .env.local
```

2. Update these required values in `.env.local`:
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `ML_API_URL` (local default: `http://127.0.0.1:5000/predict`)
- `ML_HEALTH_URL` (local default: `http://127.0.0.1:5000/health`)

3. Optional but recommended:
- `MAIL_SENDER`, `MAIL_PASSWORD`
- `ML_AUTOTRAIN_ENABLED` (set `false` if you do not want periodic retraining locally)

## Run locally (current setup)

### Prerequisites

- Java 17+
- Maven 3.8+ (or working `mvnw` wrapper)
- Python 3.10+
- Supabase/Postgres credentials

### Start app + ML together

```bash
./run_project.sh
```

### Stop all local services

```bash
./stop_project.sh
```

### Local URLs

- Home: `http://localhost:8080/pages/index.html`
- Login: `http://localhost:8080/pages/admin-login.html`
- Dashboard: `http://localhost:8080/pages/dashboard.html`

## Run locally with Docker (same model as Render)

From project root:

```bash
docker build -t fraudshield-app .
docker build -t fraudshield-ml ./ml
```

Run ML service:

```bash
docker run --rm -p 5000:5000 fraudshield-ml
```

Run app service (pass env vars):

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

### Recommended: Blueprint deploy

1. Push repository to GitHub.
2. In Render: **New + -> Blueprint**.
3. Select this repository; Render reads `render.yaml` and creates both services.
4. Set required env vars on `fraudshield-app`:
   - `SPRING_DATASOURCE_URL`
   - `SPRING_DATASOURCE_USERNAME`
   - `SPRING_DATASOURCE_PASSWORD`
   - `ML_API_URL` = `https://<your-ml-service>.onrender.com/predict`
   - `ML_HEALTH_URL` = `https://<your-ml-service>.onrender.com/health`

## Detailed docs

- Project explanation and interview Q&A: `docs/PROJECT_DEEP_DIVE.md`
- Render step-by-step deployment: `docs/RENDER_DEPLOY_GUIDE.md`
- Run from zero to full local setup: `docs/RUN_GUIDE.md`
- E2E test notes: `docs/TEST_RESULTS.md`

## API highlights

- Auth: `/auth/*`
- Transactions: `/transaction/*`
- Alerts: `/alerts/*`
- Simulation: `/simulation/*`
- System: `/system/*`
- Audit: `/audit/*`

## Security notes

- Never commit `.env` or real credentials.
- Rotate secrets immediately if accidentally exposed.
- Keep secret API keys server-side only.

## What a strong GitHub project README should include

- Project name and one-line value proposition
- Problem statement and solution overview
- Feature list
- Tech stack and versions
- Folder structure
- Local setup and run instructions
- Environment variable guide
- Deployment guide
- API usage examples/endpoints
- Security practices
- Team/contributors
- License

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

License text is available in `license.txt`.
