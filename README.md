# ClickKart Config Server

Centralized configuration for every ClickKart microservice, served from
[clickkart-config-repository](https://github.com/kripals1199/clickkart-config-repository) over
Spring Cloud Config. Item **#2** in the ClickKart build order (Eureka → **Config Server** →
Gateway → Auth → …).

- **Port:** `8888`
- **Registers with Eureka:** yes
- **Datastore:** none — it is a read-through cache over a git repository

## What it does

Serves `GET /{application}/{profile}/{label}` to every config-client service at startup. The
`label` segment maps directly to a **git branch** in the config repository, which is how this
platform separates environments:

| Requested label | Config repo branch | Used by |
|---|---|---|
| `dev` | `dev` | Local development |
| `test` | `test` | CI / automated test |
| `qa` | `qa` | QA / staging |
| `prod` | `prod` | Production |

Clients don't pass the label explicitly — each service's own `application.properties` sets
`spring.cloud.config.label=${SPRING_PROFILES_ACTIVE:dev}`, so the active profile and the config
branch can never drift apart.

## Why it is not a config client of itself

This service configures itself from `application-<profile>.properties` baked into its image,
not from Config Server — it *is* Config Server. The same applies to Eureka Server. Any other
arrangement is a circular bootstrap dependency.

## Configuration

Self-configured; see `src/main/resources/application-{dev,test,qa,prod}.properties`.

| Variable | Required in | Notes |
|---|---|---|
| `CONFIG_SERVER_USERNAME` | test/qa/prod | HTTP Basic user that clients authenticate as |
| `CONFIG_SERVER_PASSWORD` | test/qa/prod | No default outside dev — startup fails without it |
| `EUREKA_DASHBOARD_USERNAME` | test/qa/prod | Credentials for registering with Eureka |
| `EUREKA_DASHBOARD_PASSWORD` | test/qa/prod | |
| `EUREKA_SERVER_HOST` | prod | No fallback in prod — topology must be deliberate |
| `SERVER_PORT` | — | Defaults to `8888` |

## Running locally

Requires Eureka Server to be up first.

```bash
# From the platform repo, which brings up the whole stack in dependency order:
docker compose -f docker-compose.dev-infra.yml -f docker-compose.app-tier.yml up -d
```

Verify it is serving real config:

```bash
curl -u admin:<password> http://localhost:8888/clickkart-auth-service/dev/dev
```

The response's `label` and `version` fields confirm which branch and commit were resolved.

## Build

```bash
mvn -B verify
```

CI runs the same command on every push — see `.github/workflows/ci.yml`.

## Related

- [clickkart-platform](https://github.com/kripals1199/clickkart-platform) — architecture, local setup
- [clickkart-config-repository](https://github.com/kripals1199/clickkart-config-repository) — the config it serves
