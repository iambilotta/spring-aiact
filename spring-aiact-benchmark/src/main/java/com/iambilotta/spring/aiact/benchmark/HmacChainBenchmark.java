/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.benchmark;

import com.iambilotta.spring.aiact.audit.HmacChain;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Measures the cost of {@link HmacChain#chain(String, String)}, the per-record
 * HMAC computation in the Article 12 audit chain. Pure crypto, no I/O.
 *
 * <p>Run: {@code java -jar target/benchmarks.jar HmacChainBenchmark}.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class HmacChainBenchmark {

    private HmacChain chain;
    private String previousHmac;
    private String shortPayload;
    private String mediumPayload;
    private String longPayload;

    @Setup
    public void setup() {
        this.chain = HmacChain.fromUtf8("benchmark-secret-not-for-prod-32bytes-or-more");
        this.previousHmac = HmacChain.CHAIN_SEED;
        this.shortPayload = "{\"event_id\":\"x\",\"system_id\":\"hiring-screener\"}";
        this.mediumPayload = "{\"event_id\":\"x\",\"system_id\":\"hiring-screener\","
                + "\"input_hash\":\"sha256:0000000000000000000000000000000000000000000000000000000000000000\","
                + "\"output_hash\":\"sha256:1111111111111111111111111111111111111111111111111111111111111111\"}";
        StringBuilder big = new StringBuilder(8192);
        big.append("{\"event_id\":\"x\",\"payload\":\"");
        for (int i = 0; i < 8000; i++) big.append((char) ('a' + (i % 26)));
        big.append("\"}");
        this.longPayload = big.toString();
    }

    @Benchmark
    public String chainShort() {
        return chain.chain(previousHmac, shortPayload);
    }

    @Benchmark
    public String chainMedium() {
        return chain.chain(previousHmac, mediumPayload);
    }

    @Benchmark
    public String chainLong() {
        return chain.chain(previousHmac, longPayload);
    }

    @Benchmark
    public void verifyMedium(Blackhole bh) {
        // Compute then verify, matching the read-side cost of /aiact/log/verify.
        String computed = chain.chain(previousHmac, mediumPayload);
        bh.consume(chain.verify(previousHmac, mediumPayload, computed));
    }
}
