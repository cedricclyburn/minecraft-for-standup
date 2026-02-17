# Quarkus Backend

Build:

```bash
mvn clean package -DskipTests
```

Run locally:

```bash
export MINECRAFT_API_KEY=change-me
mvn quarkus:dev
```

Key endpoints:

- `GET /health`
- `GET /jira/help`
- `GET /jira/status?item=...`
- `POST /jira/update`
- `POST /jira/standup`
