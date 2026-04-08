# Structra AI – Predictive Facility Operations

**Problem:** Commercial buildings waste 30% of energy due to inefficient HVAC scheduling. Equipment failures are handled reactively (break → fix → $50k emergency repair). Facility managers rely on clipboards.

**Solution:** A software-only AI overlay that connects to existing Building Management Systems (BMS) to predict equipment failures 2 weeks in advance and optimize energy consumption without new hardware.

## MVP: Chiller Guard

Ingests temperature, pressure, and energy data via BMS APIs. Detects anomalies (Isolation Forest / LSTM) and sends prioritized alerts to facility managers: *"Unit 4 bearing likely to fail in 14 days. Schedule maintenance to save $12k."*

## Tech Stack

- **Backend:** Java 21 + Spring Boot 3
- **Database:** TimescaleDB (PostgreSQL extension for time-series data)
- **Infrastructure:** Azure (AKS for Kubernetes orchestration)
- **Containerization:** Docker
- **IaC:** Terraform
- **CI/CD:** GitHub Actions + ArgoCD
- **Compliance:** SOC2-ready architecture (Audit logging, encryption at rest/in transit)
- **AI Services:** Python + FastAPI (inference, anomaly detection)

## Getting Started

```bash
# Prerequisites: Docker Desktop running

# Start database
docker compose up -d

# Run app stack in containers (no local Java required)
docker compose --profile app up -d --build

# Or use devcontainer for cloud-friendly development
# (GitHub Codespaces / Cursor "Reopen in Container")
```

Database: `localhost:5432` (structra / structra_dev_change_me)
Spring Boot: `http://localhost:8080/api/health`
FastAPI: `http://localhost:8000/health`

## Target Market

Property management companies with 5–20 commercial buildings.

