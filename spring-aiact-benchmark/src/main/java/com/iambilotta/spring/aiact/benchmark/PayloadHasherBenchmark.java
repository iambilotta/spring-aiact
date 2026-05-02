/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.iambilotta.spring.aiact.annotation.HashStrategy;
import com.iambilotta.spring.aiact.audit.PayloadHasher;
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
import org.openjdk.jmh.annotations.Warmup;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Measures the cost of {@link PayloadHasher#hash(Object, HashStrategy)} on
 * payloads of different sizes. This is the per-call CPU tax of an
 * {@code @AiActLog} method, before any I/O.
 *
 * <p>Run: {@code java -jar target/benchmarks.jar PayloadHasherBenchmark}.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class PayloadHasherBenchmark {

    @Param({"256", "1024", "10240"})
    public int payloadSize;

    private PayloadHasher hasher;
    private Map<String, Object> payload;

    @Setup
    public void setup() {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.hasher = new PayloadHasher(mapper);

        // Build a deterministic payload of approximately payloadSize bytes once
        // a JSON pass has converted it; the size is approximate, the workload
        // is what matters across the three @Param sizes.
        Random rnd = new Random(42);
        Map<String, Object> p = new LinkedHashMap<>();
        StringBuilder sb = new StringBuilder(payloadSize);
        for (int i = 0; i < payloadSize / 4; i++) {
            sb.append((char) ('a' + rnd.nextInt(26)));
            sb.append((char) ('A' + rnd.nextInt(26)));
            sb.append((char) ('0' + rnd.nextInt(10)));
            sb.append('-');
        }
        p.put("candidateId", "c-" + payloadSize);
        p.put("cvText", sb.toString());
        p.put("score", rnd.nextDouble());
        this.payload = p;
    }

    @Benchmark
    public String hashSha256() {
        return hasher.hash(payload, HashStrategy.SHA_256);
    }

    @Benchmark
    public String hashNone() {
        return hasher.hash(payload, HashStrategy.NONE);
    }
}
