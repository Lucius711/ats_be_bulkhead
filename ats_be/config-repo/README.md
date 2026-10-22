# ATS Config Repository

This directory is the local native backend for the ATS Config Server.

Run the server from the repository root:

```bash
cp .env.example .env
mvn -pl infras/config-server spring-boot:run
```

A client can import the configuration from `http://localhost:8888` with:

```yaml
spring:
  config:
    import: optional:configserver:${CONFIG_SERVER_URL:http://localhost:8888}
```

Keep real credentials in `.env` or the runtime secret manager, not in this directory.
