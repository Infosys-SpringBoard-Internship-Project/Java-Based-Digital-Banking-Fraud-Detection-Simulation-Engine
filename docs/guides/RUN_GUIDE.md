# Run Guide (Local)

## Prerequisites

- Java 17+
- Maven 3.8+ (or `mvnw`)
- Python 3.10+
- PostgreSQL/Supabase credentials

## 1) Configure environment

```bash
cp .env.example .env.local
```

Update required values in `.env.local`:
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `ML_API_URL`
- `ML_HEALTH_URL`

## 2) Start app + ML

```bash
chmod +x run_project.sh stop_project.sh ml/run_ml.sh
./run_project.sh
```

## 3) Open app

- `http://localhost:8080/pages/index.html`
- `http://localhost:8080/pages/admin-login.html`
- `http://localhost:8080/pages/dashboard.html`

## 4) Stop services

```bash
./stop_project.sh
```

## Optional: Docker local run

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
