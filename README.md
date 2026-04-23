# IoT Based Environmental Monitoring System with AI Enhanced Pollution Forecasting

This project is a full-stack environmental monitoring system that collects city-level environmental readings, stores historical data in MySQL, and forecasts future pollution levels with a Python AI module.

## Current Architecture

`Data source/API -> Spring Boot backend -> MySQL -> Python forecasting -> React dashboard`

The system currently includes:

- Spring Boot backend for collection, persistence, and REST APIs
- MySQL storage for historical environmental readings
- Python forecasting module for future pollution estimates
- React dashboard for real-time readings, history, forecast, and warning display

## Tech Stack

- Backend: Java 17, Spring Boot, Spring Data JPA
- Database: MySQL
- AI module: Python
- Frontend: React, Axios, Recharts

## Project Structure

- `src/main/java`: backend application code
- `src/main/resources/application.properties`: backend configuration
- `predict.py`: Python forecasting script
- `frontend`: React dashboard

## Environment Variables

Set these before running the backend:

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`
- `OPENWEATHER_API_KEY`

Example PowerShell session:

```powershell
$env:DB_HOST="localhost"
$env:DB_PORT="3306"
$env:DB_NAME="env_monitoring"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="1234"
$env:OPENWEATHER_API_KEY="your_api_key_here"
```

## Backend Setup

1. Make sure MySQL is running and the `env_monitoring` database exists.
2. Set the environment variables above.
3. Start the backend:

```powershell
.\mvnw.cmd spring-boot:run
```

The backend runs on `http://localhost:8082`.

## Frontend Setup

1. Open a second terminal.
2. Start the React app:

```powershell
cd frontend
npm.cmd start
```

The frontend runs on `http://localhost:3000`.

## Main API Endpoints

- `GET /api/data?city=beijing`
  Collects and stores a fresh reading.

- `POST /api/data/collect?city=beijing`
  Explicitly collects and stores a reading.

- `GET /api/data/latest?city=beijing`
  Returns the latest stored reading.

- `GET /api/data/history?city=beijing`
  Returns stored historical readings for the city.

- `GET /api/data/forecast?city=beijing&horizon=6`
  Returns forecast points, warning level, and summary statistics.

- `GET /api/data/predict?city=beijing`
  Returns the next forecasted pollution value.

## Verification

Backend tests:

```powershell
.\mvnw.cmd test
```

Frontend production build:

```powershell
cd frontend
npm.cmd run build
```

## Notes

- If `OPENWEATHER_API_KEY` is not set, the backend falls back to simulated pollution values.
- Historical storage is essential for the forecasting workflow.
- The current forecasting model is a lightweight historical-series predictor suitable for a project prototype and demonstration.

## Hosting Plan

Recommended deployment:

- Backend: Railway
- Database: Railway MySQL
- Frontend: Vercel

For a free online demo, the backend can also run on Railway with the `railway`
profile. That profile uses an embedded H2 database stored at `/tmp/env_monitoring`
inside the Railway container. This avoids needing a paid Railway MySQL resource,
but data can be lost when the container is rebuilt or restarted.

### Backend Deployment on Railway

This repository now includes:

- `Dockerfile`
- `.dockerignore`
- `railway.json`

Railway should deploy the backend from the root of the repository using Docker.

Set these Railway environment variables:

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`
- `OPENWEATHER_API_KEY`

For the free H2 demo mode, set these instead:

- `SPRING_PROFILES_ACTIVE=railway`
- `OPENWEATHER_API_KEY=your_api_key_here`

Important:

- Railway provides a dynamic `PORT` variable automatically.
- The backend already supports this through `server.port=${PORT:8082}`.

### Frontend Deployment on Vercel

Deploy the `frontend` folder as a separate Vercel project.

Set this Vercel environment variable:

- `REACT_APP_API_BASE_URL=https://your-backend-url/api/data`

This repository now includes:

- `frontend/.env.example`
- `frontend/vercel.json`

### Suggested Deployment Order

1. Push this project to GitHub.
2. Create a Railway project from the repository root.
3. Add a Railway MySQL database.
4. Copy the Railway MySQL credentials into the backend environment variables.
5. Set `OPENWEATHER_API_KEY` in Railway.
6. Deploy the backend and copy the public backend URL.
7. Create a Vercel project from the `frontend` folder.
8. Set `REACT_APP_API_BASE_URL` in Vercel using the Railway backend URL.
9. Deploy the frontend.
