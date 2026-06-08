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
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcAuditLogServiceTest {

    private ObjectMapper mapper;
    private DataSource dataSource;
    private JdbcAuditLogService service;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .findAndRegisterModules();
        JdbcDataSource ds = new JdbcDataSource();
        // unique in-memory DB per test instance, kept alive for the whole test.
        ds.setURL("jdbc:h2:mem:aiact_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        this.dataSource = ds;
        HmacChain hmac = HmacChain.fromUtf8("unit-test-secret");
        service = new JdbcAuditLogService(dataSource, hmac, mapper);
        service.initSchema();
    }

    @Test
    void appendsAndChainsThreeEventsAcrossADbRoundTrip() {
        AuditEvent e1 = service.append(sample("sys-1", "op-a"));
        AuditEvent e2 = service.append(sample("sys-1", "op-b"));
        AuditEvent e3 = service.append(sample("sys-1", "op-c"));

        assertThat(e1.prevHmac()).isEqualTo(HmacChain.CHAIN_SEED);
        assertThat(e2.prevHmac()).isEqualTo(e1.recordHmac());
        assertThat(e3.prevHmac()).isEqualTo(e2.recordHmac());

        AuditLogService.ChainVerification report = service.verify(
                "sys-1", Instant.EPOCH, Instant.now().plusSeconds(60));
        assertThat(report.valid()).isTrue();
        assertThat(report.inspected()).isEqualTo(3);
    }

    @Test
    void verifyDetectsTamperingInTheDatabase() throws SQLException {
        service.append(sample("sys-2", "op"));
        service.append(sample("sys-2", "op"));

        // Mutate one stored record's JSON behind the chain's back.
        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate("UPDATE aiact_audit_log "
                    + "SET record_json = REPLACE(record_json, '\"op\"', '\"tampered\"') "
                    + "WHERE seq = (SELECT MIN(seq) FROM aiact_audit_log)");
        }

        AuditLogService.ChainVerification report = service.verify(
                "sys-2", Instant.EPOCH, Instant.now().plusSeconds(60));
        assertThat(report.valid()).isFalse();
        assertThat(report.invalid()).isGreaterThan(0);
    }

    @Test
    void recoversChainHeadAfterServiceRecreation() {
        service.append(sample("sys-3", "first"));
        service.append(sample("sys-3", "second"));

        JdbcAuditLogService restarted = new JdbcAuditLogService(
                dataSource, HmacChain.fromUtf8("unit-test-secret"), mapper);
        AuditEvent third = restarted.append(sample("sys-3", "third"));

        assertThat(third.prevHmac()).isNotEqualTo(HmacChain.CHAIN_SEED);
        AuditLogService.ChainVerification report = restarted.verify(
                "sys-3", Instant.EPOCH, Instant.now().plusSeconds(60));
        assertThat(report.valid()).isTrue();
        assertThat(report.inspected()).isEqualTo(3);
    }

    @Test
    void headReturnsTheLatestRecordHmac() {
        AuditEvent last = service.append(sample("sys-4", "only"));
        AuditLogService.ChainHead head = service.head("sys-4");
        assertThat(head.headHmac()).isEqualTo(last.recordHmac());
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
