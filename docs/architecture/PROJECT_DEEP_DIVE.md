# Project Deep Dive

## What this project does

FraudShield detects suspicious digital banking transactions using:
- deterministic rule checks (R01-R14)
- machine-learning fraud probability

It stores decisions, raises alerts, logs audit trails, and powers dashboard analytics.

## Core flow

1. Request reaches `POST /transaction/validate`
2. Input validation runs in backend
3. Rule engine computes risk score/reason
4. ML API (`/predict`) returns fraud probability
5. Final risk decision is persisted
6. Alert is created for high-risk fraud cases

## Major API modules

- Auth: `/auth/*`
- Transactions: `/transaction/*`
- Alerts: `/alerts/*`
- Simulation: `/simulation/*`
- System: `/system/*`
- Audit: `/audit/*`

## Security model

- Session token via login
- Role-based access: `SUPERADMIN`, `ADMIN`, `ANALYST`
- Protected APIs validated by session filter
- Sensitive fields masked for analyst views

## Why hybrid rules + ML

- Rules give explainability and deterministic governance
- ML catches non-linear fraud patterns
- Combined approach improves practical detection reliability

## Common interview questions

1. Why not only ML?
   - Rules provide explainability and fallback when ML is unavailable.
2. How do you avoid service dependency risk?
   - Backend works with graceful fallback when ML API is down.
3. How is auditability handled?
   - API logs + business action audit logs are persisted.
4. How is deployment structured?
   - Two Docker services on Render defined in `render.yaml`.
