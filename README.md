# FraudShield

FraudShield is a digital-banking fraud detection platform with:
- Spring Boot backend APIs
- Flask ML inference service
- PostgreSQL storage
- Web dashboard for fraud operations and analytics

## Stack

- Java 17, Spring Boot 3.x
- PostgreSQL
- Python 3, Flask, scikit-learn
- Maven

## Project Layout

```text
.
├── src/main/java/com/example/infosys_project/
├── src/main/resources/
│   ├── application.properties
│   ├── db/migration/                # Flyway migrations
│   ├── schema.sql                   # Reference schema snapshot
│   └── static/                      # UI pages/scripts/styles
├── ml/
│   ├── api/flask_api.py
│   ├── train_model.py
│   ├── models/
│   ├── data/
│   └── run_ml.sh
├── run_project.sh                   # Start full local stack
├── stop_project.sh                  # Stop local processes
├── .env.example
├── pom.xml
└── README.md
```

## Prerequisites

1. Java 17+
2. Maven 3.8+
3. Python 3
4. PostgreSQL 14+
5. curl

## Local Setup

1. Clone and open project:

```bash
git clone <repo-url>
cd fraud-project-source
```

2. Create local environment file:

```bash
cp .env.example .env.local
```

3. Update `.env.local` with your local DB credentials if needed.

4. Start all services:

```bash
./run_project.sh
```

5. Open UI:

- `http://localhost:8080/pages/index.html`
- `http://localhost:8080/pages/dashboard.html`

6. Stop services:

```bash
./stop_project.sh
```

## Environment Variables

Minimum required values:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/fraudshield
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
ML_API_URL=http://127.0.0.1:5000/predict
ML_HEALTH_URL=http://127.0.0.1:5000/health
```

Optional mail settings:

```env
SPRING_MAIL_USERNAME=<smtp-username>
SPRING_MAIL_PASSWORD=<smtp-password>
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
```

## Schema and Migrations

- Database changes are managed by Flyway in `src/main/resources/db/migration`.
- `V1__add_rbac.sql` now contains a full initial schema for clean setup.
- `V2__password_reset_force_change.sql` applies incremental updates.
- `schema.sql` is maintained as a reference snapshot.

## Run Checks

Compile backend:

```bash
mvn -DskipTests compile
```

Run tests:

```bash
mvn test
```

Check ML health:

```bash
curl http://127.0.0.1:5000/health
```

## Troubleshooting

1. If DB connection fails, verify PostgreSQL is running and `.env.local` credentials are correct.
2. If ML fails to start, inspect `ml/ml_api.log`.
3. If port 8080 is busy, `run_project.sh` falls back to 8081 or 8082.

## GitHub Hygiene

Before pushing:

1. Do not commit `.env.local`.
2. Do not commit logs or cache files.
3. Run `mvn -DskipTests compile` (and tests if available).
4. Keep migration files idempotent.

## License

MIT. See `LICENSE`.
