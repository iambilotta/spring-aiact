# spring-aiact end-to-end demo (60 seconds)

A minimal docker compose stack that runs the sample Spring Boot service plus a Caddy reverse
proxy that protects `/aiact/**` with basic auth. Use it to see the full pipeline from a single
HTTP call to an Article 12 audit record on disk to a verifiable HMAC chain.

## Prerequisites

- Docker Desktop or Docker Engine 24+
- `docker compose` v2 plugin

## Run

```bash
cd examples/docker-compose
docker compose up --build
```

Wait for the line `spring-aiact 0.x active | endpoints=/aiact ...` in the logs. About 30
seconds on a warm Maven cache, ~3 minutes on first build.

## Use

Score a candidate (open path, no auth):

```bash
curl -X POST http://localhost:8080/hiring/score \
  -H 'Content-Type: application/json' \
  -d '{"candidateId":"c-1","cvText":"hello there"}'
```

Read the audit log slice (basic auth `audit` / `audit-pass`):

```bash
curl -u audit:audit-pass \
  'http://localhost:8080/aiact/log/export?system=hiring-screener'
```

Verify the HMAC chain:

```bash
curl -u audit:audit-pass \
  'http://localhost:8080/aiact/log/verify?system=hiring-screener'
```

Read the head HMAC (use as a tamper canary):

```bash
curl -u audit:audit-pass \
  'http://localhost:8080/aiact/log/head?system=hiring-screener'
```

## Tamper test

```bash
docker compose exec sample sh -c \
  "sed -i 's/hiring-screener/tampered-screener/' /var/log/aiact/hiring-screener.ndjson"

curl -u audit:audit-pass \
  'http://localhost:8080/aiact/log/verify?system=hiring-screener'
# Expect "invalid" > 0 with the failed event ids listed.
```

## Tear down

```bash
docker compose down -v
```

The `-v` flag removes the persistent `aiact-logs` volume. Skip it if you want to inspect the
log on disk between runs.

## Production caveats

This demo cuts three corners that production must not:

1. **`AIACT_ENDPOINTS_ALLOW_WITHOUT_GUARD=true`** activates the unsafe permit-all in-process
   guard. Real deployments must register a custom `AiActEndpointGuard` bean. The Caddy basic
   auth in front is a thin demo layer, not a substitute.
2. **`AIACT_HMAC_SECRET=demo-secret-replace-in-prod`** is in the compose file. Real deployments
   load the secret from Vault / KMS, never from a checked-in YAML.
3. **No TLS**. Caddy can terminate TLS automatically via Let's Encrypt; the demo skips it for
   localhost.

See [`../../docs/PRODUCTION.md`](../../docs/PRODUCTION.md) for the full production checklist.
