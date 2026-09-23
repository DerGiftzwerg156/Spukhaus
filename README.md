# Spukhaus – Maskendesign-Verwaltung

Internes Tool für das [Spukhaus Aurich](https://dasspukhaus.de/), mit dem die Maskenbildner ihre Kreationen dokumentieren und verwalten können.

> **Status:** MVP implementiert – Rechtesystem, Design-Versionierung, Admin-Freigabe, Fork-Workflow und Bild-Upload sind funktionsfähig.

## Idee

Jedes **Design** besteht aus einem Namen, einer Beschreibung und beliebig vielen Bildern (eines davon als Vorschaubild, standardmäßig das zuerst hochgeladene). Neue Designs werden von einem **Creator** angelegt und müssen von einem **Admin** bestätigt werden, bevor sie für andere sichtbar sind. Auf einer Übersichtsseite lassen sich alle bestätigten Designs durchsuchen (MVP: Suche nach Namen) und im Detail mit einer Bildergalerie ansehen.

Designs sind **versioniert**: Bearbeitet ein Creator ein bereits veröffentlichtes Design, entsteht eine neue Entwurfsversion, während die zuletzt bestätigte Version für alle anderen weiterhin sichtbar bleibt. Ein Admin kann eine eingereichte Version bestätigen (sie wird veröffentlicht) oder mit einem Kommentar ablehnen (der Creator kann sie danach überarbeiten und erneut einreichen).

Creator können veröffentlichte Designs anderer **forken**: Es entsteht ein neues, eigenes Design mit den Inhalten des Originals als Ausgangspunkt, das auf das Ursprungsdesign verweist. Am Ursprungsdesign werden alle veröffentlichten Forks aufgelistet.

### Rollen

| Rolle       | Rechte                                                                 |
|-------------|-------------------------------------------------------------------------|
| TechAdmin   | Systemeinstellungen ändern; im MVP gleiche Rechte wie Admin            |
| Admin       | Alle Rechte; muss neue/geänderte Designs von Creators bestätigen oder ablehnen |
| Creator     | Alle Designs einsehen, neue Designs erstellen, eigene Designs bearbeiten, fremde veröffentlichte Designs forken |
| User        | Alle bestätigten Designs einsehen                                      |

Benutzerkonten werden ausschließlich von Admin/TechAdmin angelegt (keine Selbstregistrierung). Beim Anlegen bzw. bei einem Passwort-Reset wird ein einmaliges temporäres Passwort angezeigt; beim ersten Login muss das eigene Passwort gesetzt werden.

## Tech-Stack

- **Backend:** Spring Boot 4 (Java 21), Spring Security (JWT), Flyway
- **Datenbank:** MariaDB
- **Frontend:** Angular 19 (Standalone Components, Signals), Tailwind CSS 4 – responsive für Desktop, Tablet, Mobile
- **Android-App:** [Capacitor](https://capacitorjs.com/)-Build der Angular-App (vorbereitet, noch nicht umgesetzt)
- **Object Storage:** MinIO (Bilder, keine Dateigrößenbegrenzung; Vorschaubilder werden serverseitig zu einem komprimierten Thumbnail verarbeitet)

## Projektstruktur

```
Spukhaus/
├── backend/         Spring Boot Backend
├── frontend/        Angular Frontend
├── Tools/Docker/    Docker Compose für den gesamten Stack (lokale Entwicklung & einfaches Deployment)
└── .github/         CI/CD, Issue-/PR-Templates, Dependabot
```

## Quickstart (lokale Entwicklung)

```bash
# 1. Infrastruktur (MariaDB + MinIO) starten
docker compose -f Tools/Docker/docker-compose.yml up -d mariadb minio

# 2. Backend starten (Profil "dev")
#    Ohne ein initiales TechAdmin-Passwort kann sich niemand anmelden!
cd backend
BOOTSTRAP_TECH_ADMIN_PASSWORD='<ein-sicheres-passwort>' ./mvnw spring-boot:run

# 3. Frontend starten (in einem zweiten Terminal)
cd frontend
npm install
npm start
```

- Backend: http://localhost:8080 (Health-Check: `/actuator/health`)
- Frontend: http://localhost:4200 (Angular-Dev-Server proxied `/api` automatisch zum Backend, siehe `frontend/proxy.conf.json`)
- MinIO-Console: http://localhost:9001

Login mit Benutzername `techadmin` (konfigurierbar über `BOOTSTRAP_TECH_ADMIN_USERNAME`) und dem oben gesetzten Passwort. Nach dem ersten Login muss das Passwort geändert werden; danach können über *Benutzer* weitere Accounts angelegt werden.

### Wichtige Umgebungsvariablen (Backend)

| Variable | Zweck | Default |
|----------|-------|---------|
| `JWT_SECRET` | Signaturschlüssel für Login-Tokens (**in Produktion unbedingt setzen**, mind. 32 Zeichen) | unsicherer Dev-Default |
| `BOOTSTRAP_TECH_ADMIN_USERNAME` | Benutzername des initial angelegten TechAdmin | `techadmin` |
| `BOOTSTRAP_TECH_ADMIN_PASSWORD` | Passwort des initial angelegten TechAdmin (nur beim allerersten Start, solange keine Benutzer existieren) | – (kein Bootstrap ohne Wert) |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | MariaDB-Zugangsdaten | siehe `application.yml` |
| `MINIO_URL`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET` | Object-Storage-Zugangsdaten | siehe `application.yml` |

## Gesamten Stack mit Docker Compose starten

```bash
JWT_SECRET='<mind-32-zeichen>' BOOTSTRAP_TECH_ADMIN_PASSWORD='<ein-sicheres-passwort>' \
  docker compose -f Tools/Docker/docker-compose.yml up -d --build
```

Startet MariaDB, MinIO, Backend und Frontend zusammen. Frontend: http://localhost:8081, Backend: http://localhost:8080.

Für das Produktions-Deployment auf dem VPS (https://spukhaus.nexacode.de, hinter einem gemeinsamen Reverse-Proxy) gibt es `Tools/Docker/docker-compose.prod.yml`, siehe [Tools/Docker/README.md](Tools/Docker/README.md#produktion-vps-httpsspukhausnexacodede).

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
