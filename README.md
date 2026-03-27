![Java](https://img.shields.io/badge/Java-17-blue) ![Spring%20Boot](https://img.shields.io/badge/Spring%20Boot-3.4.1-green) ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14%2B-blue) ![Python](https://img.shields.io/badge/Python-3-yellow) ![Flask](https://img.shields.io/badge/Flask-black) ![scikit--learn](https://img.shields.io/badge/scikit--learn-orange) ![License](https://img.shields.io/badge/License-MIT-green) ![Status](https://img.shields.io/badge/Status-Active-brightgreen)

```text
███████╗██████╗  █████╗ ██╗   ██╗██████╗
██╔════╝██╔══██╗██╔══██╗██║   ██║██╔══██╗
█████╗  ██████╔╝███████║██║   ██║██║  ██║
██╔══╝  ██╔══██╗██╔══██║██║   ██║██║  ██║
██║     ██║  ██║██║  ██║╚██████╔╝██████╔╝
╚═╝     ╚═╝  ╚═╝╚═╝  ╚═╝ ╚═════╝ ╚═════╝
███████╗██╗  ██╗██╗███████╗██╗     ██████╗
██╔════╝██║  ██║██║██╔════╝██║     ██╔══██╗
███████╗███████║██║█████╗  ██║     ██║  ██║
╚════██║██╔══██║██║██╔══╝  ██║     ██║  ██║
███████║██║  ██║██║███████╗███████╗██████╔╝
╚══════╝╚═╝  ╚═╝╚═╝╚══════╝╚══════╝╚═════╝
```

> AI-Powered Digital Banking Fraud Detection and Simulation Engine

<p align="center">
  <em>Rule-based fraud scoring · Machine learning integration · Real-time dashboard · Admin authentication · Email alerts</em>
</p>

## Team

| Name | GitHub |
|---|---|
| Team Member 1 | `@username1` |
| Team Member 2 | `@username2` |
| Team Member 3 | `@username3` |
| Team Member 4 | `@username4` |
| Team Member 5 | `@username5` |
| Team Member 6 | `@username6` |

## Overview

FraudShield is a full-stack digital banking fraud detection platform built as an Infosys Springboard internship project. It accepts banking transactions that are either manually submitted or auto-generated, validates transaction fields, runs them through a 14-rule scoring engine, adds machine learning probability scoring, persists the result to PostgreSQL, and presents the outcome in a browser dashboard.

The system processes each transaction through a sequential pipeline: validation checks -> rule engine -> ML microservice -> single database write -> alert generation -> dashboard update. The ML layer is additive. If the Flask service on port `5000` is offline, the Spring Boot rule engine continues independently with graceful fallback and stores the transaction with `mlFraudProbability = 0.0`.

Fraud detection is a critical part of digital banking infrastructure, and this project demonstrates a practical layered design: deterministic rules combined with probabilistic ML scoring, IP intelligence with five risk tags, session-based admin authentication, a live operational dashboard, automated email alerting, and startup scripts that bring the stack up as a single local system.

## Live Demo

> This repository does not include a hosted public deployment. The intended demo flow is local and starts after running the application stack.

```text
Landing page:  http://localhost:8080/pages/index.html
Admin login:   http://localhost:8080/pages/admin-login.html
Dashboard:     http://localhost:8080/pages/dashboard.html
```

> If port `8080` is already occupied, `run_project.sh` automatically falls back to `8081` and then `8082`.

## Features

| Feature | Description |
|---------|-------------|
| 🔍 14-Rule Fraud Engine | Layered risk scoring from `0.0` to `10.0` with `LOW`, `MEDIUM`, `HIGH`, and `CRITICAL` verdicts |
| 🤖 ML Integration | Flask microservice with Random Forest inference and graceful offline fallback |
| 🌐 IP Intelligence | Five-tag classification: `CLEAN`, `VPN`, `PROXY`, `DATACENTER`, `TOR` |
| ⚡ Real-time Dashboard | Live feed, summary metrics, rule-hit breakdown, and system health indicator |
| 📋 Manual Transaction Input | Browser form for submitting transactions through the full validation and scoring pipeline |
| 🔐 Admin Authentication | BCrypt-hashed credentials, in-memory session tokens, and protected routes |
| 📧 Email Alerts | Automatic email notifications for `HIGH` and `CRITICAL` detections |
| 🔔 In-app Notifications | Alert bell with unread count, history panel, and mark-as-read actions |
| 📊 Analytics Views | Separate dashboard views for summary, transactions, manual input, and analytics |
| 📤 CSV Export | Export persisted transactions as CSV for model refresh or offline analysis |
| 🧪 Synthetic Generator | Seven fraud scenarios mixed with normal traffic for repeatable simulation |
| 🚀 Startup Scripts | Database readiness checks, ML health checks, and coordinated app startup |
| 🌍 Landing Page | Project overview page with pipeline and rule visualization |
| 📱 Responsive UI | Designed for desktop and laptop/tablet monitoring use cases |

## System Architecture

```text
┌─────────────────────────────────────────────────────────────┐
│                        BROWSER CLIENT                       │
│   /pages/index.html  /pages/admin-login.html  /pages/...    │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTP REST / Static Files
                           ▼
┌─────────────────────────────────────────────────────────────┐ 
│                    SPRING BOOT (port 8080)                  │
│                                                             │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────────┐    │
│  │SessionFilter│  │AuthController│  │AlertController   │    │
│  │(auth guard) │  │/auth/**      │  │/alerts/**        │    │
│  └─────────────┘  └──────────────┘  └──────────────────┘    │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              TransactionController                   │   │
│  │ /generate /autoValidate /validate /all /summary      │   │
│  │ /frauds /by-risk /by-ip-tag /export-csv              │   │
│  └─────────────────────┬────────────────────────────────┘   │
│                        │                                    │
│  ┌─────────────────────▼─────────────────────────────────┐  │
│  │                TransactionService                     │  │
│  │ 1. Autofill  -> 2. Validate -> 3. Rule Engine         │  │
│  │ 4. ML Score  -> 5. ML Override -> 6. Save Once        │  │
│  │ 7. Save Alert -> 8. Send Email -> 9. Response         │  │
│  └────────┬────────────────────────┬─────────────────────┘  │
│           │                        │                        │
│  ┌────────▼────────┐      ┌────────▼──────────────┐         │
│  │ FraudDetector   │      │ EmailAlertService     │         │
│  │ 14 rules        │      │ JavaMailSender        │         │
│  │ Score 0.0-10.0  │      │ HIGH + CRITICAL mail  │         │
│  └─────────────────┘      └───────────────────────┘         │
└───────────────────────────┬─────────────────────────────────┘
                            │
          ┌─────────────────┼─────────────────┐
          │                 │                 │
          ▼                 ▼                 ▼
┌──────────────┐  ┌──────────────────┐  ┌──────────────┐
│  PostgreSQL  │  │  Flask ML API    │  │  Gmail SMTP  │
│  port 5432   │  │  port 5000       │  │  port 587    │
│              │  │                  │  │              │
│ transactions │  │ /predict         │  │ Alert emails │
│ admin_users  │  │ RandomForest     │  │ to admin     │
│ fraud_alerts │  │ + rule companion │  │              │
└──────────────┘  └──────────────────┘  └──────────────┘
```

The system follows a strict single-save pattern. Validation, rule scoring, ML scoring, and final risk assignment happen in memory before persistence. Every stored transaction therefore carries a final fraud verdict, a risk level, a risk score, and the ML probability without requiring post-save correction.

## Tech Stack

| Layer | Technology | Version / Notes |
|-------|-----------|-----------------|
| Backend Framework | Spring Boot | `3.4.1` |
| Language | Java | `17 LTS` |
| Database | PostgreSQL | `14+` |
| ORM | Spring Data JPA | Hibernate via `spring-boot-starter-data-jpa` |
| ML Language | Python | `3.x` |
| ML Framework | scikit-learn | Random Forest inference artifacts |
| ML API | Flask | Runs on port `5000` |
| Password Hashing | Spring Security | `BCryptPasswordEncoder` only |
| Email | Spring Mail | Gmail SMTP |
| Frontend | HTML / CSS / JavaScript | Static pages served by Spring Boot |
| Fonts | Google Fonts | Barlow Condensed, Barlow, Space Mono |
| Build Tool | Maven | `mvnw` wrapper included |

## Project Structure

```text
FraudShield/
│
├── .mvn/
│   └── wrapper/
│       └── maven-wrapper.properties
│
├── src/main/java/com/example/infosys_project/
│   ├── JavaBasedDigitalBankingFraudDetectionAndSimulationEngineApplication.java
│   ├── controller/
│   │   ├── AlertController.java
│   │   ├── AuthController.java
│   │   ├── DashboardController.java
│   │   └── TransactionController.java
│   ├── detection/
│   │   └── FraudDetector.java
│   ├── dto/
│   │   └── ValidationResponse.java
│   ├── generator/
│   │   └── TransactionGenerator.java
│   ├── model/
│   │   ├── AdminUser.java
│   │   ├── FraudAlert.java
│   │   └── TransactionModel.java
│   ├── repository/
│   │   ├── AdminRepository.java
│   │   ├── FraudAlertRepository.java
│   │   └── TransactionRepository.java
│   ├── security/
│   │   └── SessionFilter.java
│   ├── service/
│   │   ├── AuthService.java
│   │   ├── EmailAlertService.java
│   │   └── TransactionService.java
│   └── validation/
│       └── TransactionValidator.java
│
├── src/main/resources/
│   ├── application.properties
│   ├── schema.sql
│   └── static/
│       ├── pages/
│       │   ├── admin-login.html
│       │   ├── dashboard.html
│       │   ├── dashboard-analytics.html
│       │   ├── dashboard-manual.html
│       │   ├── dashboard-transactions.html
│       │   └── index.html
│       ├── scripts/
│       │   ├── dashboard/dashboard.js
│       │   ├── home.js
│       │   └── login-page.js
│       └── styles/
│           ├── dashboard/dashboard.css
│           ├── home.css
│           └── login-page.css
│
├── ml/
│   ├── api/
│   │   └── flask_api.py
│   ├── data/
│   │   └── transactions.csv
│   ├── models/
│   │   ├── encoders.json
│   │   ├── fraud_model.pkl
│   │   └── rule_model.pkl
│   ├── requirements.txt
│   └── run_ml.sh
│
├── mvnw
├── mvnw.cmd
├── pom.xml
├── run_project.sh
├── stop_project.sh
├── LICENSE
└── README.md
```

> This repository snapshot includes trained ML artifacts and encoded lookup data. The training script that produced those artifacts is not committed in the current branch.

## Getting Started

### Prerequisites

- [ ] Java 17+
- [ ] Maven 3.8+ or the included `./mvnw`
- [ ] PostgreSQL 14+
- [ ] Python 3.8+
- [ ] Git

### 1. Clone the repository

```bash
git clone https://github.com/YOUR_USERNAME/fraudshield.git
cd fraudshield
```

### 2. Set up PostgreSQL

```sql
CREATE DATABASE fraud_db;
```

> `schema.sql` and JPA bootstrapping create the required tables and indexes on first startup.

### 3. Configure `application.properties`

Update `src/main/resources/application.properties` before launching the project:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/fraud_db
spring.datasource.username=postgres
spring.datasource.password=your_db_password

spring.mail.username=yourgmail@gmail.com
spring.mail.password=your_gmail_app_password
admin.alert.email=youremail@gmail.com
```

> The current repository reads these values directly from `application.properties`. Exporting shell variables alone will not override the placeholders unless you externalize the Spring configuration separately.

> For Gmail, use an App Password rather than your regular account password. Google path: `Security -> 2-Step Verification -> App Passwords`.

### 4. Option A: Use the startup script

```bash
chmod +x run_project.sh
./run_project.sh
```

The launcher performs the following steps:

- checks PostgreSQL connectivity
- creates the target database if it does not already exist
- starts the ML API if `localhost:5000` is not already healthy
- starts Spring Boot on `8080`, or `8081` / `8082` if needed
- cleans up the ML child process on exit when it launched that process itself

### 5. Option B: Manual startup

```bash
# Terminal 1 - Flask ML API
cd ml
chmod +x run_ml.sh
./run_ml.sh
```

```bash
# Terminal 2 - Spring Boot API + UI
./mvnw spring-boot:run
```

### 6. Create the first admin account

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Admin","email":"admin@fraudshield.com","password":"yourpassword"}'
```

> This endpoint is one-time-only. After the first admin user exists, further calls return `403 Forbidden`.

### 7. Open the application in a browser

```text
Landing page:  http://localhost:8080/pages/index.html
Admin login:   http://localhost:8080/pages/admin-login.html
Dashboard:     http://localhost:8080/pages/dashboard.html
```

### 8. Generate sample data

```bash
for i in $(seq 1 100); do
  curl -s http://localhost:8080/transaction/autoValidate \
    -H "Authorization: Bearer YOUR_TOKEN" > /dev/null
done
```

### 9. Export transactions for ML analysis

```bash
curl http://localhost:8080/transaction/export-csv \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -o ml/data/transactions.csv
```

> The repository already contains trained model artifacts under `ml/models/`. Exported CSV data can be used for retraining in your separate team training workflow.

### 10. Stop the stack

```bash
chmod +x stop_project.sh
./stop_project.sh
```

## Configuration

```properties
# Database connection.
spring.datasource.url=jdbc:postgresql://localhost:5432/fraud_db
spring.datasource.username=postgres
spring.datasource.password=your_db_password
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA and Hibernate.
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.sql.init.mode=always

# Pretty-print JSON responses.
spring.jackson.serialization.indent-output=true

# Server.
server.port=8080

# Static files.
spring.web.resources.static-locations=classpath:/static/

# Email (Gmail SMTP).
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=
spring.mail.password=
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# Alert recipient override.
admin.alert.email=
```

> Session lifetime is currently fixed in code at `8` hours inside `AuthService`. It is not externalized as a Spring property in this repository snapshot.

> If `admin.alert.email` is left blank, the email service falls back to the first active admin user stored in the database.

## API Reference

### Transaction Endpoints

| Method | Endpoint | Auth Required | Description |
|--------|----------|---------------|-------------|
| `GET` | `/transaction/generate` | Yes | Generate a random transaction without saving it |
| `GET` | `/transaction/autoValidate` | Yes | Generate, validate, score, persist, and return a full result |
| `POST` | `/transaction/validate` | Yes | Submit a manual transaction JSON payload |
| `GET` | `/transaction/all` | Yes | Fetch all persisted transactions |
| `GET` | `/transaction/{id}` | Yes | Fetch one transaction by UUID |
| `GET` | `/transaction/frauds` | Yes | Return only transactions marked as fraud |
| `GET` | `/transaction/by-risk/{level}` | Yes | Filter by `LOW`, `MEDIUM`, `HIGH`, or `CRITICAL` |
| `GET` | `/transaction/by-ip-tag/{tag}` | Yes | Filter by `CLEAN`, `VPN`, `PROXY`, `TOR`, or `DATACENTER` |
| `GET` | `/transaction/summary` | Yes | Summary metrics, totals, fraud rate, and rule breakdown |
| `GET` | `/transaction/system-status` | Yes | Spring/ML service status for the dashboard badge |
| `GET` | `/transaction/export-csv` | Yes | Download all transactions as CSV |

### Auth Endpoints

| Method | Endpoint | Auth Required | Description |
|--------|----------|---------------|-------------|
| `POST` | `/auth/register` | No | Create the first admin account only |
| `POST` | `/auth/login` | No | Authenticate admin and return a session token |
| `POST` | `/auth/logout` | Yes | Invalidate the current session |
| `GET` | `/auth/me` | Yes | Return the current admin profile |
| `PUT` | `/auth/update-credentials` | Yes | Update admin email and/or password |

### Alert Endpoints

| Method | Endpoint | Auth Required | Description |
|--------|----------|---------------|-------------|
| `GET` | `/alerts` | Yes | Fetch the newest 20 alerts |
| `GET` | `/alerts/unread` | Yes | Fetch unread alerts only |
| `GET` | `/alerts/count` | Yes | Return unread alert count as JSON |
| `PUT` | `/alerts/{id}/read` | Yes | Mark a single alert as read |
| `PUT` | `/alerts/read-all` | Yes | Mark all alerts as read |

> Protected API calls accept the header `Authorization: Bearer {token}`.

> Browser sessions also use the `fraud_session` HttpOnly cookie that is set by `POST /auth/login`.

## Fraud Detection Rules

The rule engine applies additive scoring across 14 primary rules, with several rules containing threshold variants. Scores are capped at `10.0`, rounded to one decimal place, and then converted into the final risk band.

| Rule | Name | Trigger | Score Added |
|------|------|---------|-------------|
| `R01` | High Amount | `amount >= ₹50,000` | `+2.5` |
| `R01` | Critical Amount | `amount >= ₹1,00,000` | `+4.0` |
| `R02` | Odd Hours | Current server time between `1 AM` and `4:59 AM` | `+2.0` |
| `R03` | Balance Drain | `type = debit` and `amount >= 90% of balance` | `+3.5` |
| `R04` | Frequent Txns | `txn_count_last_1hr >= 4` | `+1.5` |
| `R04` | Rapid Fire | `txn_count_last_1hr >= 8` | `+3.0` |
| `R05` | Crypto Merchant | `merchantCategory = crypto` | `+3.0` |
| `R05` | Gambling Merchant | `merchantCategory = gambling` | `+2.5` |
| `R05` | Dark Web Merchant | `merchantCategory = darkweb` | `+5.0` |
| `R06` | Location Jump | `isNewLocation = true` and `distance > 500 km` | `+2.0` |
| `R06` | Impossible Travel | `isNewLocation = true` and `distance > 1000 km` | `+3.5` |
| `R07` | New Device | `isNewDevice = true` | `+1.5` |
| `R08a` | VPN / Proxy Flag | `isVpnOrProxy = true` | `+1.5` |
| `R08b` | IP Location Mismatch | `ipMatchesLocation = false` | `+2.0` |
| `R08c` | Commercial VPN | `ipRiskTag = VPN` | `+1.0` |
| `R08c` | Anonymous Proxy | `ipRiskTag = PROXY` | `+2.0` |
| `R08c` | Datacenter IP | `ipRiskTag = DATACENTER` | `+2.5` |
| `R08c` | TOR Network | `ipRiskTag = TOR` | `+3.5` |
| `R09` | International Txn | `isInternational = true` and `currency != INR` | `+2.0` |
| `R10` | New Receiver | `isFirstTimeReceiver = true` and `amount >= ₹20,000` | `+2.0` |
| `R11` | Amount Spike | `amount >= 5x avgTxnAmount30Days` | `+2.5` |
| `R12` | New Account Large Transfer | `accountAgeDays < 30` and `amount >= ₹10,000` | `+2.5` |
| `R13` | High Daily Volume | `txn_count_last_24hr >= 20` | `+2.0` |
| `R14` | Round Amount Structuring | `amount >= ₹10,000` and amount is a round thousand | `+1.0` |

| Score Range | Risk Level | isFraud | Action |
|-------------|------------|---------|--------|
| `0.0 - 2.2` | `LOW` | `false` | Save only |
| `2.3 - 4.5` | `MEDIUM` | `false` | Save and flag for review in dashboard analytics |
| `4.6 - 7.1` | `HIGH` | `true` | Save, create alert, send email |
| `7.2 - 10.0` | `CRITICAL` | `true` | Save, create alert, send email, show urgent dashboard notification |

## Machine Learning

### Model

- Primary classifier: `RandomForestClassifier`
- Companion explainer model: multi-output Random Forest for rule-flag reconstruction
- Library: `scikit-learn`
- Estimators: `200`
- Max depth: `10`
- Class weight: `balanced`
- Random state: `42`
- Model artifacts: `ml/models/fraud_model.pkl`, `ml/models/rule_model.pkl`, `ml/models/encoders.json`
- Training data source: transactions exported from the Java pipeline as CSV

> These hyperparameters were recovered from the committed model artifacts. The original training script is not present in the current repository snapshot.

### Features Used

```text
amount
balance
txn_count_last_1hr
txn_count_last_24hr
avg_txn_amount_30days
distance_from_last_txn_km
account_age_days
is_new_location
is_new_device
is_vpn_or_proxy
ip_matches_location
is_international
is_first_time_receiver
merchant_category (encoded)
transaction_mode (encoded)
location (encoded)
ip_risk_tag (encoded)
```

### Integration

Spring Boot calls `POST http://localhost:5000/predict` for every transaction after rule-based scoring. The Flask service returns a probability and an ML risk interpretation:

```json
{
  "fraud_probability": 0.87,
  "ml_risk_level": "CRITICAL",
  "is_fraud_ml": true,
  "fraud_reason": "R01:HighAmount(=52000) | R10:NewReceiverHighAmount(52000)",
  "fired_rules": {
    "r01": 1,
    "r02": 0,
    "r03": 0,
    "r04": 0,
    "r05": 0,
    "r06": 0,
    "r07": 0,
    "r08": 0,
    "r09": 0,
    "r10": 1,
    "r11": 0,
    "r12": 0,
    "r13": 0,
    "r14": 0
  }
}
```

Flask risk thresholds are:

- `CRITICAL` when `fraud_probability >= 0.80`
- `HIGH` when `fraud_probability >= 0.60`
- `MEDIUM` when `fraud_probability >= 0.40`
- `LOW` otherwise

> If Flask is offline or inference fails, Spring Boot records `mlFraudProbability = 0.0` and continues with the rule-engine verdict unchanged.

### ML Override Logic

If `mlFraudProbability >= 0.75` and the rule engine did not mark the transaction as fraud, Spring Boot upgrades the final verdict to:

- `isFraud = true`
- `riskLevel = HIGH`
- `riskScore = max(existingScore, 5.5)`
- fraud reason appended with `R-ML:MLFlagged(prob=...)`

The ML layer never downgrades a rule-detected fraud.

## 👥 Team

This project was built as a team during the Infosys Springboard internship program.

| Name | Role | GitHub |
|------|------|--------|
| [Team Member 1] | Backend, Fraud Engine, ML Integration | `@username` |
| [Team Member 2] | Frontend Dashboard, UI/UX, Auth Pages | `@username` |
| [Team Member 3] | Database, API Design, Testing | `@username` |
| [Team Member 4] | ML Model, Flask API, Data Pipeline | `@username` |

> Replace the placeholder names and GitHub usernames with your actual team details before publishing.

## 🙏 Acknowledgements

- **Infosys Springboard** - for providing the internship opportunity and project framework
- **Spring Boot** - for the Java web application framework
- **scikit-learn** - for the machine learning tooling used in the inference service
- **PostgreSQL** - for the relational data layer
- This project was built for educational and demonstration purposes as part of the Infosys internship program

## License

```text
MIT License — see LICENSE file for details.
Built for Infosys Springboard Internship 2026.
```
