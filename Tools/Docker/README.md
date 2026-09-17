# Docker Compose

## Nur Infrastruktur (lokale Backend-/Frontend-Entwicklung)

Startet MariaDB und MinIO (inkl. automatischer Bucket-Anlage) für die lokale Entwicklung mit
`./mvnw spring-boot:run` / `npm start` (`dev`-Profil):

```bash
docker compose -f Tools/Docker/docker-compose.yml up -d mariadb minio minio-init
```

| Dienst  | Port(s)     | Zugangsdaten                          |
|---------|-------------|----------------------------------------|
| MariaDB | 3306        | `spukhaus` / `spukhaus` (DB `spukhaus`) |
| MinIO   | 9000, 9001  | `spukhaus` / `spukhaus123` (Console: 9001) |

## Gesamter Stack (inkl. Backend & Frontend)

```bash
JWT_SECRET='<mind-32-zeichen>' BOOTSTRAP_TECH_ADMIN_PASSWORD='<ein-sicheres-passwort>' \
  docker compose -f Tools/Docker/docker-compose.yml up -d --build
```

Baut zusätzlich `backend` (Port 8080) und `frontend` (Port 8081, nginx proxied `/api` intern zum
Backend-Container). `JWT_SECRET` und `BOOTSTRAP_TECH_ADMIN_PASSWORD` sind Pflichtvariablen ohne
Default – der Start bricht sonst mit einer entsprechenden Fehlermeldung ab.

Zum Stoppen:

```bash
docker compose -f Tools/Docker/docker-compose.yml down
```

Mit `-v` zusätzlich die Volumes (Datenbankinhalt, Objektspeicher) löschen.
