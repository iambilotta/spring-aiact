# spring-aiact sample app

A minimal, runnable Spring Boot service that shows the whole `spring-aiact` story end to end: **one annotated high-risk class**, and from it a build-time Annex IV technical file + Article 47 Declaration of Conformity, plus a runtime Article 12 tamper-evident audit chain you can verify, tamper, and re-verify with `curl`.

This is the canonical **5-minute quickstart**. Everything below was captured from a real run; copy-paste it and you will see the same.

> The single annotated class is [`HiringScreener.java`](src/main/java/com/iambilotta/spring/aiact/sample/HiringScreener.java) (a deliberately trivial CV-scoring placeholder, `score = length / 1000`). The annotation set is the entire point; the math is not. Config is [`application.yaml`](src/main/resources/application.yaml) (port 8081, `allow-without-guard: true` for local runs only).

## 1. Build it (generates the dossier)

From the repository root:

```bash
mvn -q -DskipTests install        # build + install the library modules
mvn -q -pl spring-aiact-sample aiact:generate
```

The Maven plugin reads the annotations off `HiringScreener` and writes the dossier to `spring-aiact-sample/target/generated-docs/`:

```
hiring-screener-technical-file.md          # Annex IV technical file (9 sections)
hiring-screener-doc.pdf                     # Article 47 Declaration of Conformity (signature placeholder)
hiring-screener-dataset-cv-corpus-2025.md   # Article 10 per-dataset datasheet
```

Remove a required companion annotation and re-run: the build fails naming the missing one (that is `mvn -pl spring-aiact-sample verify`, which also runs `aiact:verify`).

## 2. Run it

```bash
mvn -pl spring-aiact-sample spring-boot:run
```

Wait for `Started SampleApplication`. The service listens on **http://localhost:8081**.

## 3. Generate audit records

Score three candidates. Each `@AiActLog` method call appends one Article 12 event to the chain.

```bash
curl -s -X POST http://localhost:8081/hiring/score \
  -H 'Content-Type: application/json' \
  -d '{"candidateId":"cand-1","cvText":"'"$(printf 'x%.0s' {1..700})"'"}'
# {"score":0.7,"label":"shortlist"}

curl -s -X POST http://localhost:8081/hiring/score \
  -H 'Content-Type: application/json' \
  -d '{"candidateId":"cand-2","cvText":"short cv"}'
# {"score":0.008,"label":"reject"}

curl -s -X POST http://localhost:8081/hiring/score \
  -H 'Content-Type: application/json' \
  -d '{"candidateId":"cand-3","cvText":"'"$(printf 'y%.0s' {1..300})"'"}'
# {"score":0.3,"label":"reject"}
```

## 4. Verify the chain

```bash
curl -s "http://localhost:8081/aiact/log/verify?system=hiring-screener"
# {"systemId":"hiring-screener","from":null,"to":null,"inspected":3,"invalid":0,"failedEventIds":[]}

curl -s "http://localhost:8081/aiact/log/head?system=hiring-screener"
# {"system_id":"hiring-screener","head_hmac":"df1514e2...d92da5"}
```

`inspected: 3, invalid: 0` — three records, chain intact.

## 5. Tamper one record, re-verify

The log lives at `${java.io.tmpdir}/spring-aiact-sample/hiring-screener.ndjson`. Flip a single byte of one stored record's `input_hash`:

```bash
F="${TMPDIR:-/tmp}/spring-aiact-sample/hiring-screener.ndjson"
sed -i '1s/sha256:0/sha256:9/' "$F"     # corrupt one hex digit of the first record

curl -s "http://localhost:8081/aiact/log/verify?system=hiring-screener"
# {"systemId":"hiring-screener",...,"inspected":3,"invalid":1,"failedEventIds":["24f18346-..."]}
```

`invalid: 1` and the offending `event_id` named. That is the [Article 12](https://artificialintelligenceact.eu/article/12/) tamper-evidence property: you cannot silently edit or delete a record without verification catching it.

## 6. Record an Article 14 human override

A human reviewer overrules the AI output. The override is appended as a second event linked to the original (`linkedEventId`), so the audit trail shows both the AI decision and the human one. Use the `eventId` from the `failedEventIds` above (any real event id works); `decision` must be one of `flag-anomaly`, `stop`, `accept`, `reject`.

```bash
EID=<event-id-from-step-5>
curl -s -X POST "http://localhost:8081/aiact/oversight/$EID/override" \
  -H 'Content-Type: application/json' \
  -d '{"actor":"hr-anna","decision":"reject","reason":"human reviewer overrides the AI score","systemId":"hiring-screener"}'
# {"operation":"article-14-override","eventKind":"OVERRIDE","userIdPseudonymized":"hr-anna",
#  "linkedEventId":"24f18346-...","prevHmac":"df1514e2...","recordHmac":"72d1eec4..."}
```

## 7. Export the raw NDJSON

```bash
curl -s "http://localhost:8081/aiact/log/export?system=hiring-screener" | head -1
# {"event_id":"24f18346-...","event_kind":"INVOCATION","operation":"HiringScreener.score","model_id":"hiring-screener@0.0.1","input_hash":"sha256:...","prev_hmac":"0000...","record_hmac":"4550d8a4..."}
```

That is what an auditor receives: the append-only chain, each record carrying the previous record's HMAC.

---

That is the full loop in well under 15 minutes: **annotate → build the dossier → run → log → verify → tamper-detect → override**. To take it to production, see the main [README](../README.md#production-auth-wiring) (wire a real `AiActEndpointGuard`, move the HMAC secret to a vault) and [`docs/PRODUCTION.md`](../docs/PRODUCTION.md). For the database-backed audit sink on ephemeral runtimes, see [Audit sink: NDJSON or JDBC](../README.md#audit-sink-ndjson-or-jdbc).

> **Never copy `allow-without-guard: true` into a real deployment.** It lets any caller hit `/aiact/**`. It is on here only so the walkthrough above needs no auth.
