# FraudShield

FraudShield is a digital banking fraud detection and simulation platform built during an Infosys internship project. It combines explainable rule-based checks with ML-assisted scoring to help operations teams detect suspicious transactions quickly and consistently.

## Project Purpose

Digital fraud monitoring often fails when systems rely only on static rules or only on black-box models. FraudShield addresses this by combining both approaches:

1. Rule-based detection for transparent fraud reasons.
2. ML probability scoring for adaptive risk intelligence.
3. Operational dashboards for analytics, alerts, and audit visibility.

## Core Capabilities

- Hybrid fraud detection (rules plus ML score)
- Role-based access for operational users
- Manual and simulation-driven transaction testing
- Fraud alerts with risk-level reasoning
- Dashboard analytics for trends and incident triage
- Health and monitoring checks for backend and ML services

## Technology Stack

- Backend: Java 17, Spring Boot 3, Maven
- ML Service: Python 3, Flask, scikit-learn
- Database: PostgreSQL
- Frontend: HTML, CSS, JavaScript

## Repository Structure

```text
.
├── src/main/java/com/example/infosys_project/
├── src/main/resources/
│   ├── application.properties
│   ├── db/migration/                # Flyway migrations
│   ├── schema.sql                   # Schema reference snapshot
│   └── static/                      # UI pages/scripts/styles
├── ml/
│   ├── api/flask_api.py
│   ├── train_model.py
│   ├── requirements.txt
│   ├── data/
│   └── models/
├── run_project.sh                   # Start local stack
├── stop_project.sh                  # Stop local stack
├── .env.example
├── pom.xml
└── README.md
```

## Prerequisites

1. Java 17 or later
2. Maven 3.8 or later
3. Python 3.10 or later
4. PostgreSQL 14 or later
5. curl

## Quick Start (Local)

1. Clone the repository.

```bash
git clone <repo-url>
cd fraud-project-source
```

2. Create local environment file.

```bash
cp .env.example .env.local
```

3. Update `.env.local` with your local database and optional mail credentials.

4. Start backend plus ML service.

```bash
./run_project.sh
```

5. Open application pages.

- http://localhost:8080/pages/index.html
- http://localhost:8080/pages/dashboard.html

6. Stop services.

```bash
./stop_project.sh
```

## Minimum Environment Variables

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/fraudshield
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
ML_API_URL=http://127.0.0.1:5000/predict
ML_HEALTH_URL=http://127.0.0.1:5000/health
```

Optional mail settings:

```env
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
SPRING_MAIL_USERNAME=<smtp-username>
SPRING_MAIL_PASSWORD=<smtp-password>
```

## Database and Schema Management

- Flyway migrations in `src/main/resources/db/migration` are the source of truth.
- `V1__add_rbac.sql` initializes the base schema for fresh local setup.
- `V2__password_reset_force_change.sql` applies incremental updates.
- `src/main/resources/schema.sql` is maintained as a reference snapshot.

## Verification Commands

Compile backend:

```bash
mvn -DskipTests compile
```

Run tests:

```bash
mvn test
```

Verify ML health:

```bash
curl http://127.0.0.1:5000/health
```

## Troubleshooting

1. If database connection fails, verify PostgreSQL is running and `.env.local` values are correct.
2. If ML service fails, inspect log files generated from `run_project.sh`.
3. If port 8080 is occupied, update local run configuration before starting.

## Contribution Guidelines

1. Create a feature branch from the active integration branch.
2. Keep commits focused and use clear commit messages.
3. Run compile and tests before opening a pull request.
4. Document configuration or migration changes in README and migration files.

## Internship Team

Infosys Internship Project Team:

| Member | GitHub |
|---|---|
| Team Member 1 | [@advikagarwal](https://github.com/advikagarwal) |
| Team Member 2 | [@Shakthisri16](https://github.com/Shakthisri16) |
| Team Member 3 | [@tarakeshwararao-S](https://github.com/tarakeshwararao-S) |
| Team Member 4 | [@nishika701](https://github.com/nishika701) |
| Team Member 5 | [@jaswanth82006](https://github.com/jaswanth82006) |
| Team Member 6 | [@GNavya15](https://github.com/GNavya15) |

If you want, member names and roles can also be added alongside GitHub handles.

## License

MIT. See [LICENSE](LICENSE).