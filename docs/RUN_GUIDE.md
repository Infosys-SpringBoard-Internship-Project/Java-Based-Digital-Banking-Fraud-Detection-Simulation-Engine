# Run Guide (From Zero to Full Working Setup)

This guide starts from a fresh machine and takes you to a fully running FraudShield environment.

## 1) Install prerequisites

### Required software
- Git
- Java 17+
- Maven 3.8+
- Python 3.10+
- Docker (optional, only for container-based local run)

Check versions:

```bash
git --version
java -version
mvn -version
python3 --version
docker --version
```

## 2) Get source code

If cloning from GitHub:

```bash
git clone https://github.com/<your-username>/<your-repo>.git
cd <your-repo>
```

If you already have the folder, go to project root:

```bash
cd fraud-project-source
```

## 3) Create local environment file

```bash
cp .env.example .env.local
```

Edit `.env.local` and fill real values:
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `ML_API_URL` (keep local default unless changed)
- `ML_HEALTH_URL` (keep local default unless changed)

Optional:
- `MAIL_SENDER`
- `MAIL_PASSWORD`
- `ML_AUTOTRAIN_ENABLED=true/false`

## 4) Understand what the launcher script does

`./run_project.sh` will:
- load `.env.local` (or `.env`)
- validate Supabase JDBC URL format
- start ML API via `ml/run_ml.sh` if not already running
- start Spring Boot app on `8080` (or fallback port if occupied)

## 5) Run project (recommended local mode)

```bash
chmod +x run_project.sh stop_project.sh ml/run_ml.sh
./run_project.sh
```

Expected output includes:
- ML API health confirmation
- Spring Boot startup logs
- local URLs

## 6) Open app in browser

- Home: `http://localhost:8080/pages/index.html`
- Login: `http://localhost:8080/pages/admin-login.html`
- Dashboard: `http://localhost:8080/pages/dashboard.html`

If port changed by script, use shown port in terminal.

## 7) First-time account setup

If no admin exists:
- Open login/registration path from UI
- Create initial superadmin
- Login and continue

## 8) Stop all local services

```bash
./stop_project.sh
```

## 9) Local Docker run (optional, Render-like)

Build images:

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

## 10) Quick verification checklist

- `GET /system/health` returns data
- `GET /transaction/system-status` returns Spring and ML status
- Login works
- Dashboard loads transactions
- `GET /alerts/count` returns JSON count

## 11) Common local issues

### Maven not found
- Install Maven or restore wrapper files.

### Python venv fails
- Ensure `python3` exists and has `venv` module.

### DB auth fails
- Recheck JDBC URL, username, password.
- Confirm DB is reachable and SSL enabled.

### ML unavailable
- Check `ml/run_ml.sh` logs and dependency install.
- Verify local port 5000 is free.

### Port 8080 already in use
- Script automatically falls back (8081/8082).

## 12) Suggested day-to-day workflow

1. Pull latest code
2. Update `.env.local` only if needed
3. Run `./run_project.sh`
4. Develop and test
5. Stop with `./stop_project.sh`
6. Commit and push
