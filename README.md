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

## Security and Compliance (Month 3)

Implemented:

- Audit logging via `audit_log` table plus trigger-based DB audit events.
- API key authentication (`X-API-Key-Name` + `X-API-Key`) and JWT issuance (`POST /api/v1/auth/token`).
- Encryption at rest for selected sensitive columns using AES-GCM JPA converter.
- TLS 1.3-ready server config via environment-driven SSL settings.
- Row-Level Security migration scaffolding with tenant (`organization_id`) columns and policies.

Environment variables:

- `APP_SECURITY_ENABLED=true|false`
- `APP_SECURITY_JWT_SECRET=<32+ byte secret>`
- `APP_SECURITY_JWT_TTL_SECONDS=3600`
- `APP_SECURITY_ENCRYPTION_KEY=<32-byte key or Azure Key Vault-injected value>`
- `APP_SECURITY_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173`
- `SERVER_SSL_ENABLED=true|false`
- `SERVER_SSL_KEY_STORE=/path/to/keystore.p12`
- `SERVER_SSL_KEY_STORE_PASSWORD=<password>`
- `SERVER_SSL_KEY_STORE_TYPE=PKCS12`

Example authenticated request:

```bash
curl -X POST http://localhost:8080/api/v1/ingestion/readings \
	-H "Content-Type: application/json" \
	-H "X-API-Key-Name: dev-default" \
	-H "X-API-Key: sg_dev_sk_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6" \
	-d '{"chillerUnitExternalId":"UNIT-001","timestamp":"2026-04-14T08:00:00Z","dataSource":"BMS"}'
```

