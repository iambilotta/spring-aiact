# spring-aiact-benchmark

JMH micro-benchmarks for the Article 12 audit hot path. **Not distributed**: this module is local-only, excluded from `mvn install`/`deploy`, and does not appear on JitPack.

## What we measure

| Class | What it pins |
|---|---|
| `HmacChainBenchmark` | Pure crypto cost: `HmacChain.chain(prev, payload)` per record. Three payload sizes (short, medium ~10 fields, long 8KB) plus a `verify` round-trip. |
| `PayloadHasherBenchmark` | Per-call CPU cost of `PayloadHasher.hash(value, SHA_256)`: Jackson serialization + SHA-256 digest. Three payload sizes (256B, 1KB, 10KB). Includes the `HashStrategy.NONE` baseline. |
| `AuditLogAppendBenchmark` | End-to-end append throughput of `NdjsonAuditLogService.append(event)`, in records/sec for a single writer thread. Switches on/off `singleWriterLock` to show the OS-lock + fsync cost. |

## How to run

From the reactor root:

```bash
./mvnw -B -DskipTests -pl spring-aiact-benchmark -am package
java -jar spring-aiact-benchmark/target/benchmarks.jar
```

Subset:

```bash
java -jar spring-aiact-benchmark/target/benchmarks.jar HmacChainBenchmark
java -jar spring-aiact-benchmark/target/benchmarks.jar PayloadHasherBenchmark.hashSha256
```

JSON output:

```bash
java -jar spring-aiact-benchmark/target/benchmarks.jar -rf json -rff results/$(date -I)-$(uname -m).json
```

## Reference numbers

Captured 2026-05-02 on Corretto 25.0.3 (Linux x86_64), `-wi 2 -i 3 -f 1`. Take as ballpark, not as your-laptop-numbers.

| Benchmark | Mode | Score | Unit |
|---|---|---|---|
| `HmacChainBenchmark.chainShort` (~50 bytes) | avg | 0.83 ± 0.05 | µs/op |
| `HmacChainBenchmark.chainMedium` (~250 bytes) | avg | 0.88 ± 0.10 | µs/op |
| `HmacChainBenchmark.chainLong` (~8 KB) | avg | 5.41 ± 0.19 | µs/op |
| `HmacChainBenchmark.verifyMedium` | avg | 1.71 ± 0.07 | µs/op |
| `PayloadHasherBenchmark.hashNone` | avg | 0.01 | µs/op |
| `PayloadHasherBenchmark.hashSha256` (256 B) | avg | 0.97 ± 0.19 | µs/op |
| `PayloadHasherBenchmark.hashSha256` (1 KB) | avg | 2.02 ± 0.50 | µs/op |
| `PayloadHasherBenchmark.hashSha256` (10 KB) | avg | 15.40 ± 1.14 | µs/op |
| `AuditLogAppendBenchmark.append` (single-writer-lock=true) | thrpt | **~180 ops/sec** | ops/sec |
| `AuditLogAppendBenchmark.append` (single-writer-lock=false) | thrpt | **~83k ops/sec** | ops/sec |

Raw JSON output: [`results/2026-05-02-jdk25-corretto.json`](results/2026-05-02-jdk25-corretto.json).

## What these numbers mean for an adopter

- **CPU-side**: the advisor on `@AiActLog` adds sub-microsecond cost on a 1KB payload. Your endpoint will not feel it.
- **I/O-side**: the file lock + fsync of the default NDJSON sink caps single-writer throughput at ~180 records/sec. Above that, switch to `aiact.audit.single-writer-lock=false` (single-writer process / pod, no multi-writer safety) and you get ~83k ops/sec, or wait for the JDBC sink in a future minor.
- **Hot path budget**: for an HR triage app at 10 req/sec, the audit chain costs are immeasurable next to the database. For a real-time scoring service at 1k req/sec, the lock mode becomes the thing to size for.

These are reference numbers; your laptop or your CI runner will produce different absolutes. Re-run the benchmark on your target hardware before sizing.

## Why this module exists

A compliance library that says "sub-millisecond" in the FAQ without numbers is selling intuition. A library that ships a JMH harness adopters can re-run is selling a measurement. The cost of running the harness on your own hardware is two minutes; the cost of trusting our blanket claims is finding out at production scale.
