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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class NdjsonAuditLogServiceTest {

    @TempDir
    Path tempDir;

    private ObjectMapper mapper;
    private NdjsonAuditLogService service;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .findAndRegisterModules();
        HmacChain hmac = HmacChain.fromUtf8("unit-test-secret");
        service = new NdjsonAuditLogService(tempDir, hmac, mapper);
    }

    @Test
    void appendsAndChainsThreeEvents() throws IOException {
        AuditEvent e1 = service.append(sample("sys-1", "op-a"));
        AuditEvent e2 = service.append(sample("sys-1", "op-b"));
        AuditEvent e3 = service.append(sample("sys-1", "op-c"));

        assertThat(e1.prevHmac()).isEqualTo(HmacChain.CHAIN_SEED);
        assertThat(e2.prevHmac()).isEqualTo(e1.recordHmac());
        assertThat(e3.prevHmac()).isEqualTo(e2.recordHmac());

        String content = Files.readString(service.fileFor("sys-1"), StandardCharsets.UTF_8);
        assertThat(content.trim().split("\n")).hasSize(3);
    }

    @Test
    void verifyReturnsCleanReportOnGoodLog() {
        service.append(sample("sys-2", "op"));
        service.append(sample("sys-2", "op"));

        AuditLogService.ChainVerification report = service.verify(
                "sys-2", Instant.EPOCH, Instant.now().plusSeconds(60));
        assertThat(report.valid()).isTrue();
        assertThat(report.inspected()).isEqualTo(2);
        assertThat(report.invalid()).isZero();
    }

    @Test
    void verifyDetectsTamperingOnDisk() throws IOException {
        service.append(sample("sys-3", "op"));
        service.append(sample("sys-3", "op"));

        Path file = service.fileFor("sys-3");
        String content = Files.readString(file, StandardCharsets.UTF_8);
        Files.writeString(file,
                content.replace("\"op\"", "\"tampered\""),
                StandardCharsets.UTF_8);

        AuditLogService.ChainVerification report = service.verify(
                "sys-3", Instant.EPOCH, Instant.now().plusSeconds(60));
        assertThat(report.valid()).isFalse();
        assertThat(report.invalid()).isGreaterThan(0);
    }

    @Test
    void recoversChainHeadAfterServiceRecreation() {
        service.append(sample("sys-4", "first"));
        service.append(sample("sys-4", "second"));

        NdjsonAuditLogService restarted = new NdjsonAuditLogService(
                tempDir, HmacChain.fromUtf8("unit-test-secret"), mapper);
        AuditEvent third = restarted.append(sample("sys-4", "third"));

        AuditLogService.ChainVerification report = restarted.verify(
                "sys-4", Instant.EPOCH, Instant.now().plusSeconds(60));
        assertThat(report.valid()).isTrue();
        assertThat(report.inspected()).isEqualTo(3);
        assertThat(third.prevHmac()).isNotEqualTo(HmacChain.CHAIN_SEED);
    }

    private static AuditEvent sample(String systemId, String operation) {
        return AuditEvent.builder()
                .eventKind(EventKind.INVOCATION)
                .systemId(systemId)
                .systemVersion("0.0.1")
                .operation(operation)
                .modelId("test-model")
                .latencyMs(5L)
                .inputHash("sha256:abc")
                .outputHash("sha256:def")
                .hashAlgorithm("SHA-256")
                .build();
    }
}
