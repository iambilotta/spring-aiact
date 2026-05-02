/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.iambilotta.spring.aiact.audit.HmacChain;
import com.iambilotta.spring.aiact.audit.NdjsonAuditLogService;
import com.iambilotta.spring.aiact.model.AuditEvent;
import com.iambilotta.spring.aiact.model.EventKind;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * End-to-end append throughput on the NDJSON sink, with the file lock and
 * the HMAC chain step both contributing. Measured in records / second under
 * sustained writes by a single thread.
 *
 * <p>The benchmark writes to a fresh temp directory per fork. The
 * {@code @Param} switches the {@code single-writer-lock} mode: ON gives the
 * default v1.x behaviour (one OS file lock per append, multi-pod safe);
 * OFF gives the relaxed mode (no lock, single-writer process assumed).
 *
 * <p>Run: {@code java -jar target/benchmarks.jar AuditLogAppendBenchmark}.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 2)
@Fork(1)
@State(Scope.Benchmark)
public class AuditLogAppendBenchmark {

    @Param({"true", "false"})
    public boolean singleWriterLock;

    private NdjsonAuditLogService sink;
    private Path tempDir;

    @Setup
    public void setup() throws IOException {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.tempDir = Files.createTempDirectory("aiact-bench");
        this.sink = new NdjsonAuditLogService(
                tempDir,
                HmacChain.fromUtf8("benchmark-secret-not-for-prod-32bytes-or-more"),
                mapper,
                singleWriterLock);
    }

    @TearDown
    public void tearDown() throws IOException {
        if (tempDir != null) {
            try (Stream<Path> stream = Files.walk(tempDir)) {
                stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (IOException ignored) { }
                });
            }
        }
    }

    @Benchmark
    public AuditEvent append() {
        AuditEvent event = new AuditEvent(
                UUID.randomUUID(),
                EventKind.INVOCATION,
                Instant.now(),
                "hiring-screener",
                "0.1.0",
                "HiringScreener.score",
                null,
                "hiring-screener@0.1.0",
                "sha256:0000000000000000000000000000000000000000000000000000000000000000",
                "sha256:1111111111111111111111111111111111111111111111111111111111111111",
                "SHA-256",
                3L,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
        return sink.append(event);
    }
}
