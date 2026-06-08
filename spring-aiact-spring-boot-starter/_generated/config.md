# Configuration properties — spring-aiact-spring-boot-starter (as-is)

Auto-generated from `application*.properties`. Comments above each key are surfaced as 'role'; usages are the Java files that reference `${key}` via `@Value` or any string interpolation. Keys defined in `application.properties` are the canonical shape; per-profile overrides appear in their own column.

**Env parity model**: the canonical `application.properties` is **prod-safe by default**; every value is either a literal default or `${ENV_VAR:default}`. Staging and prod ship NO properties file — they inject the env-vars via Terraform / Cloud Run / Secret Manager. The only profile file is `application-local.properties`, activated by `-Dspring-boot.run.profiles=local`. The 'Profile overrides' section below audits each override against the canonical to make the local-vs-deploy delta explicit.

**Total keys**: 0

## All keys (flat table)

| Key | Used by |
|---|---|

## Detail
