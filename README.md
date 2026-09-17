# Spukhaus – Maskendesign-Verwaltung

Internes Tool für das [Spukhaus Aurich](https://dasspukhaus.de/), mit dem die Maskenbildner ihre Kreationen dokumentieren und verwalten können.

> **Status:** Projektgrundgerüst (MVP-Vorbereitung). Es ist noch keine fachliche Funktionalität implementiert.

## Idee

Jedes **Design** besteht aus einem Namen, einer Beschreibung und beliebig vielen Bildern (eines davon als Vorschaubild). Neue Designs werden von einem **Creator** angelegt und müssen von einem **Admin** bestätigt werden, bevor sie für andere sichtbar sind. Auf einer Übersichtsseite lassen sich alle bestätigten Designs durchsuchen (MVP: Suche nach Namen) und im Detail mit einer Bildergalerie ansehen.

### Rollen

| Rolle       | Rechte                                                                 |
|-------------|-------------------------------------------------------------------------|
| TechAdmin   | Systemeinstellungen ändern; im MVP gleiche Rechte wie Admin            |
| Admin       | Alle Rechte; muss neue Designs von Creators bestätigen                 |
| Creator     | Designs einsehen, neue Designs erstellen, eigene Designs bearbeiten    |
| User        | Alle bestätigten Designs einsehen                                      |

## Tech-Stack

- **Backend:** Spring Boot (Java 21)
- **Datenbank:** MariaDB
- **Frontend:** Angular (responsive: Desktop, Tablet, Mobile)
- **Android-App:** [Capacitor](https://capacitorjs.com/)-Build der Angular-App
- **Object Storage:** MinIO (Bilder)

## Projektstruktur

```
Spukhaus/
├── backend/         Spring Boot Backend
├── frontend/        Angular Frontend
├── Tools/Docker/    Docker Compose für MariaDB & MinIO (lokale Entwicklung)
└── .github/         CI/CD, Issue-/PR-Templates, Dependabot
```

## Quickstart

```bash
# 1. Infrastruktur (MariaDB + MinIO) starten
docker compose -f Tools/Docker/docker-compose.yml up -d

# 2. Backend starten (Profil "dev")
cd backend
./mvnw spring-boot:run

# 3. Frontend starten (in einem zweiten Terminal)
cd frontend
npm install
npm start
```

- Backend: http://localhost:8080 (Health-Check: `/actuator/health`)
- Frontend: http://localhost:4200
- MinIO-Console: http://localhost:9001

## Container-Images

Sowohl Backend als auch Frontend besitzen ein eigenes `Dockerfile` und lassen sich unabhängig bauen:

```bash
docker build -t spukhaus-backend ./backend
docker build -t spukhaus-frontend ./frontend
```

## Mitwirken

Siehe [CONTRIBUTING.md](CONTRIBUTING.md) für Details zu lokalem Setup, Branching und Tests.

## Lizenz

Dieses Projekt steht unter der [MIT-Lizenz](LICENSE).
