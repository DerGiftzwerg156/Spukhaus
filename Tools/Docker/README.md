# Docker Compose (lokale Entwicklung)

Startet MariaDB und MinIO für die lokale Backend-Entwicklung (`dev`-Profil).

```bash
docker compose -f Tools/Docker/docker-compose.yml up -d
```

| Dienst  | Port(s)     | Zugangsdaten                          |
|---------|-------------|----------------------------------------|
| MariaDB | 3306        | `spukhaus` / `spukhaus` (DB `spukhaus`) |
| MinIO   | 9000, 9001  | `spukhaus` / `spukhaus123` (Console: 9001) |

Zum Stoppen:

```bash
docker compose -f Tools/Docker/docker-compose.yml down
```

Mit `-v` zusätzlich die Volumes (Datenbankinhalt, Objektspeicher) löschen.
