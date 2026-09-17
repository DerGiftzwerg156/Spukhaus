# Contributing

Danke für dein Interesse, am Spukhaus-Tool mitzuwirken!

## Projektstruktur

```
Spukhaus/
├── backend/         Spring Boot Backend (Java 21)
├── frontend/        Angular Frontend (Web + Capacitor/Android)
├── Tools/Docker/    Docker Compose für lokale Entwicklung (MariaDB, MinIO)
└── .github/         CI/CD-Workflows, Issue-/PR-Templates, Dependabot
```

## Voraussetzungen

- Java 21 (Backend)
- Node.js 22+ und npm (Frontend)
- Docker & Docker Compose (lokale Datenbank/MinIO, Container-Builds)

## Lokale Entwicklung

1. Infrastruktur starten:
   ```bash
   docker compose -f Tools/Docker/docker-compose.yml up -d
   ```
2. Backend starten (Profil `dev` ist Standard). Beim allerersten Start muss ein initiales
   TechAdmin-Passwort gesetzt werden, sonst kann sich niemand anmelden:
   ```bash
   cd backend
   BOOTSTRAP_TECH_ADMIN_PASSWORD='<ein-sicheres-passwort>' ./mvnw spring-boot:run
   ```
3. Frontend starten:
   ```bash
   cd frontend
   npm install
   npm start
   ```

## Branching & Commits

- Neue Branches von `main` abzweigen, sprechender Name (`feature/...`, `fix/...`).
- Commit-Messages möglichst im [Conventional Commits](https://www.conventionalcommits.org/)-Stil (`feat:`, `fix:`, `chore:`, ...).
- Pull Requests gegen `main`, das PR-Template ausfüllen.

## Tests

- Backend: `./mvnw test` (im Verzeichnis `backend`)
- Frontend: `npm test` (im Verzeichnis `frontend`)

Beide Test-Suiten laufen automatisch in der CI (GitHub Actions) für jeden Pull Request.

## Code-Stil

- Backend: Standard Java-/Spring-Konventionen, kein unnötiger Boilerplate.
- Frontend: Angular Style Guide, Tailwind-Utility-Klassen statt eigenem CSS wo möglich.

## Fragen / Vorschläge

Bitte über ein GitHub Issue (Bug Report oder Feature Request Template) einbringen.
