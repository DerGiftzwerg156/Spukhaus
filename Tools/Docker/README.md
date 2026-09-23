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

## Produktion (VPS, https://spukhaus.nexacode.de)

`docker-compose.prod.yml` ist für den VPS gedacht, auf dem Spukhaus zusammen mit weiteren
Projekten (z. B. der Stempeluhr) läuft. TLS terminiert ein gemeinsamer Reverse-Proxy (Caddy),
dessen Konfiguration im Stempeluhr-Repo unter `deploy/reverse-proxy/` liegt. Der Stack öffnet
**keine** Host-Ports: MariaDB, MinIO und Backend sind nur intern erreichbar, das Frontend tritt
zusätzlich dem externen Docker-Netzwerk `web-proxy` (Alias `spukhaus-web`) bei.

Voraussetzungen: Reverse-Proxy läuft, Netzwerk existiert (`docker network create web-proxy`),
DNS-Eintrag `spukhaus.nexacode.de` zeigt auf den VPS.

```bash
cp Tools/Docker/.env.example Tools/Docker/.env
# alle Werte in Tools/Docker/.env ersetzen, z. B. mit: openssl rand -base64 48
docker compose -f Tools/Docker/docker-compose.prod.yml --env-file Tools/Docker/.env up -d --build
docker compose -f Tools/Docker/docker-compose.prod.yml --env-file Tools/Docker/.env logs -f backend
```

Aktualisieren: `git pull` und denselben `up -d --build`-Befehl erneut ausführen. Daten liegen in
den Volumes `spukhaus_mariadb-data` und `spukhaus_minio-data` – niemals `down -v` verwenden.

> Die obige `docker-compose.yml` ist nur für die lokale Entwicklung gedacht (feste
> Standard-Passwörter, offene Ports 3306/9000/9001/8080) und darf nicht auf dem VPS laufen.
