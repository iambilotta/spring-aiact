/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.autoconfigure;

import com.iambilotta.spring.aiact.audit.AuditLogService;
import com.iambilotta.spring.aiact.audit.JdbcAuditLogService;
import com.iambilotta.spring.aiact.audit.NdjsonAuditLogService;
import com.iambilotta.spring.aiact.retention.RetentionPolicyService;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the audit sink selection behind {@code aiact.audit.sink}: NDJSON is the default, and
 * {@code aiact.audit.sink=jdbc} wires the JDBC sink against the adopter's {@link DataSource} plus a
 * matching retention service, so swapping the sink never trips the
 * {@code requires NdjsonAuditLogService} bean-creation error (DX gap #1).
 */
class AuditSinkSelectionTest {

    private static final String[] COMMON = {
            "aiact.endpoints.enabled=false",
            "aiact.retention-sweeper.enabled=false",
            "aiact.hmac.secret=a-real-secret-that-came-from-vault"
    };

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AiActAutoConfiguration.class));

    private static DataSource h2() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:aiact_sink_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }

    @Test
    void defaultsToTheNdjsonSink() {
        runner.withPropertyValues(COMMON)
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx).getBean(AuditLogService.class)
                            .isInstanceOf(NdjsonAuditLogService.class);
                });
    }

    @Test
    void wiresTheJdbcSinkWhenSinkIsJdbc() {
        runner.withBean(DataSource.class, AuditSinkSelectionTest::h2)
                .withPropertyValues(COMMON)
                .withPropertyValues("aiact.audit.sink=jdbc")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx).getBean(AuditLogService.class)
                            .isInstanceOf(JdbcAuditLogService.class);
                });
    }

    @Test
    void doesNotTripTheNdjsonRetentionRequirementUnderJdbc() {
        runner.withBean(DataSource.class, AuditSinkSelectionTest::h2)
                .withPropertyValues(COMMON)
                .withPropertyValues("aiact.audit.sink=jdbc")
                .run(ctx -> {
                    // The context must start: the old NDJSON-only default threw
                    // "requires NdjsonAuditLogService" at bean creation when the sink was swapped.
                    assertThat(ctx).hasNotFailed();
                    // File-based pruning is meaningless for a table, so the NDJSON retention
                    // service is correctly absent under the JDBC sink (rather than throwing).
                    assertThat(ctx).doesNotHaveBean(RetentionPolicyService.class);
                });
    }

    @Test
    void keepsTheNdjsonRetentionServiceUnderTheDefaultSink() {
        runner.withPropertyValues(COMMON)
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx).hasSingleBean(RetentionPolicyService.class);
                });
    }
}
