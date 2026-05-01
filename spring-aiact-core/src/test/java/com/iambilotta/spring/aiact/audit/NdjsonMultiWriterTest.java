/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.iambilotta.spring.aiact.model.AuditEvent;
import com.iambilotta.spring.aiact.model.EventKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Two {@link NdjsonAuditLogService} instances simulate two pods writing to the same shared
 * NDJSON file. The HMAC chain must remain valid end to end. Without the file-lock path, the two
 * caches drift and the chain corrupts; with the lock, every record reads the head from disk
 * before writing.
 */
class NdjsonMultiWriterTest {

    @TempDir
    Path tempDir;

    private ObjectMapper mapper;
    private HmacChain hmac;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        hmac = HmacChain.fromUtf8("multi-writer-key");
    }

    @Test
    void chainStaysValidWithFileLockEnabledAcrossTwoConcurrentWriters() throws Exception {
        NdjsonAuditLogService a = new NdjsonAuditLogService(tempDir, hmac, mapper, true);
        NdjsonAuditLogService b = new NdjsonAuditLogService(tempDir, hmac, mapper, true);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int i = 0; i < 50; i++) {
                NdjsonAuditLogService writer = (i % 2 == 0) ? a : b;
                int idx = i;
                pool.submit(() -> writer.append(sample("sys-shared", "op-" + idx)));
            }
        } finally {
            pool.shutdown();
            assertThat(pool.awaitTermination(15, TimeUnit.SECONDS)).isTrue();
        }

        AuditLogService.ChainVerification verification =
                a.verify("sys-shared", java.time.Instant.EPOCH, java.time.Instant.now().plusSeconds(60));
        assertThat(verification.invalid()).isZero();
        assertThat(verification.inspected()).isEqualTo(50);
    }

    @Test
    void chainCorruptsWithoutFileLockAcrossTwoConcurrentWriters() throws Exception {
        NdjsonAuditLogService a = new NdjsonAuditLogService(tempDir, hmac, mapper, false);
        NdjsonAuditLogService b = new NdjsonAuditLogService(tempDir, hmac, mapper, false);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int i = 0; i < 100; i++) {
                NdjsonAuditLogService writer = (i % 2 == 0) ? a : b;
                int idx = i;
                pool.submit(() -> writer.append(sample("sys-conflict", "op-" + idx)));
            }
        } finally {
            pool.shutdown();
            assertThat(pool.awaitTermination(15, TimeUnit.SECONDS)).isTrue();
        }

        // Without the file lock, the two in-memory chainHeads caches drift apart and the
        // chain breaks. We assert the failure mode visibly so a regression that "fixes"
        // the unsafe path silently never goes unnoticed.
        AuditLogService.ChainVerification verification =
                a.verify("sys-conflict", java.time.Instant.EPOCH, java.time.Instant.now().plusSeconds(60));
        assertThat(verification.invalid()).isGreaterThan(0);
    }

    private AuditEvent sample(String systemId, String operation) {
        return AuditEvent.builder()
                .eventKind(EventKind.INVOCATION)
                .systemId(systemId)
                .systemVersion("0.0.1")
                .operation(operation)
                .modelId("test-model")
                .latencyMs(1L)
                .inputHash("sha256:abc")
                .outputHash("sha256:def")
                .hashAlgorithm("SHA-256")
                .build();
    }
}
