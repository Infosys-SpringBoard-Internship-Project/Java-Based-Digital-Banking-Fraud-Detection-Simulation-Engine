# Render Deployment Guide

This project deploys as two Render web services:
- `fraudshield-app` (Spring Boot)
- `fraudshield-ml` (Flask ML API)

## 1) Push repository to GitHub

```bash
git add .
git commit -m "Prepare deployment"
git push origin develop
```

## 2) Create Blueprint in Render

1. Open Render dashboard.
2. Click **New + -> Blueprint**.
3. Select this repository.
4. Render reads `render.yaml` and creates both services.

## 3) Configure environment variables

Set on `fraudshield-app`:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `ML_API_URL=https://<ml-service>.onrender.com/predict`
- `ML_HEALTH_URL=https://<ml-service>.onrender.com/health`
- `ML_AUTOTRAIN_ENABLED=false`
- `ML_AUTOTRAIN_INTERVAL_MINUTES=2880`
- `MAIL_SENDER` (optional)
- `MAIL_PASSWORD` (optional)

## 4) Deployment order

1. Deploy `fraudshield-ml` first.
2. Verify ML endpoint:
   - `https://<ml-service>.onrender.com/health`
3. Deploy `fraudshield-app`.

## 5) Verify application

- `https://<app-service>.onrender.com/pages/index.html`
- `https://<app-service>.onrender.com/pages/admin-login.html`
- `https://<app-service>.onrender.com/system/health`

## 6) Common issues

- DB connection failure: check datasource URL/credentials and SSL mode.
- ML unavailable: confirm `ML_API_URL`/`ML_HEALTH_URL` use deployed ML URL.
- Slow startup: free tier cold starts are normal.
