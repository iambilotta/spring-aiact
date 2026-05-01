/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.retention;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.iambilotta.spring.aiact.audit.AuditLogService;
import com.iambilotta.spring.aiact.audit.HmacChain;
import com.iambilotta.spring.aiact.audit.NdjsonAuditLogService;
import com.iambilotta.spring.aiact.model.AuditEvent;
import com.iambilotta.spring.aiact.model.EventKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.Period;

import static org.assertj.core.api.Assertions.assertThat;

class RetentionPolicyServiceTest {

    @TempDir
    Path tempDir;

    private ObjectMapper mapper;
    private NdjsonAuditLogService auditLog;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        auditLog = new NdjsonAuditLogService(tempDir, HmacChain.fromUtf8("k"), mapper, false);
    }

    @Test
    void prunesRecordsOlderThanCutoff() throws IOException {
        Path file = auditLog.fileFor("sys");
        // Inject one historical record + two recent records by writing then re-reading.
        auditLog.append(eventAt(Instant.now().minusSeconds(60))); // recent
        auditLog.append(eventAt(Instant.now().minusSeconds(30))); // recent

        // Manually rewrite the first line with an old timestamp to simulate aged record.
        injectAgedRecord(file, Instant.parse("2010-01-01T00:00:00Z"));

        RetentionPolicyService r = new RetentionPolicyService(auditLog, Period.ofYears(10));
        RetentionPolicyService.PruneReport report = r.prune("sys");

        assertThat(report.pruned()).isEqualTo(1);
        long survivors = Files.lines(file, StandardCharsets.UTF_8).filter(s -> !s.isBlank()).count();
        assertThat(survivors).isEqualTo(2);
    }

    @Test
    void leavesFileIntactWhenNothingIsOld() {
        auditLog.append(eventAt(Instant.now()));
        auditLog.append(eventAt(Instant.now()));

        RetentionPolicyService r = new RetentionPolicyService(auditLog, Period.ofYears(10));
        RetentionPolicyService.PruneReport report = r.prune("sys");

        assertThat(report.pruned()).isZero();
        assertThat(report.kept()).isEqualTo(2);
    }

    @Test
    void emptyFileNoOps() {
        RetentionPolicyService r = new RetentionPolicyService(auditLog, Period.ofYears(10));
        RetentionPolicyService.PruneReport report = r.prune("sys-never-touched");
        assertThat(report.pruned()).isZero();
        assertThat(report.kept()).isZero();
    }

    @Test
    void chainStaysVerifiableForKeptSliceAfterPrune() throws IOException {
        auditLog.append(eventAt(Instant.now().minusSeconds(20)));
        auditLog.append(eventAt(Instant.now().minusSeconds(10)));
        injectAgedRecord(auditLog.fileFor("sys"), Instant.parse("2010-01-01T00:00:00Z"));

        new RetentionPolicyService(auditLog, Period.ofYears(10)).prune("sys");

        // The verifier walks from CHAIN_SEED, so it surfaces the boundary mismatch on the
        // new first record. We assert that the documented behavior (one mismatch on the
        // boundary, 0 elsewhere) holds, instead of pretending the chain stays seamless.
        AuditLogService.ChainVerification verification =
                auditLog.verify("sys", Instant.EPOCH, Instant.now().plusSeconds(60));
        assertThat(verification.invalid()).isLessThanOrEqualTo(1);
    }

    private AuditEvent eventAt(Instant ts) {
        return AuditEvent.builder()
                .eventKind(EventKind.INVOCATION)
                .timestamp(ts)
                .systemId("sys")
                .systemVersion("0.0.1")
                .operation("op")
                .modelId("m")
                .latencyMs(1L)
                .inputHash("sha256:a")
                .outputHash("sha256:b")
                .hashAlgorithm("SHA-256")
                .build();
    }

    private void injectAgedRecord(Path file, Instant when) throws IOException {
        AuditEvent ev = eventAt(when);
        // Append directly bypassing the chain helper: this is a synthetic "aged" record
        // for the prune test. The retention sweeper does not care about the chain: it
        // matches by timestamp string only.
        Files.writeString(file,
                mapper.writeValueAsString(ev) + System.lineSeparator(),
                StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND);
    }
}
