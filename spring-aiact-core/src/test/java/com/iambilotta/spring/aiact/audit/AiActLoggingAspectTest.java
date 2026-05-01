/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.iambilotta.spring.aiact.annotation.AiActHighRiskSystem;
import com.iambilotta.spring.aiact.annotation.AiActLog;
import com.iambilotta.spring.aiact.annotation.AnnexIIICategory;
import com.iambilotta.spring.aiact.model.AuditEvent;
import com.iambilotta.spring.aiact.model.EventKind;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = AiActLoggingAspectTest.TestConfig.class)
@TestPropertySource(properties = {
        "aiact.test.dir=${java.io.tmpdir}/spring-aiact-aspect-test"
})
class AiActLoggingAspectTest {

    @Autowired SampleAnnotated annotated;
    @Autowired AuditLogService auditLog;

    @Test
    void invocationProducesOneAuditRecord() {
        annotated.score("hello");
        List<AuditEvent> events = collect("aspect-system");
        assertThat(events).hasSize(1);
        AuditEvent e = events.get(0);
        assertThat(e.eventKind()).isEqualTo(EventKind.INVOCATION);
        assertThat(e.systemId()).isEqualTo("aspect-system");
        assertThat(e.operation()).isEqualTo("SampleAnnotated.score");
        assertThat(e.inputHash()).startsWith("sha256:");
        assertThat(e.outputHash()).startsWith("sha256:");
    }

    @Test
    void thrownExceptionProducesAnomalyRecordWithSanitizedMetadata() {
        assertThatThrownBy(() -> annotated.fails())
                .isInstanceOf(IllegalStateException.class);

        List<AuditEvent> events = collect("aspect-system");
        AuditEvent last = events.get(events.size() - 1);
        assertThat(last.eventKind()).isEqualTo(EventKind.ANOMALY);
        assertThat(last.metadata()).containsKeys("exception_class", "message_fingerprint");
        assertThat(last.metadata().values()).noneMatch(v -> v.contains("secret-Mario"));
    }

    @Test
    void disablingCaptureSkipsHashes() {
        annotated.scoreWithoutCapture("hello");
        List<AuditEvent> events = collect("aspect-system");
        AuditEvent last = events.get(events.size() - 1);
        assertThat(last.inputHash()).isNull();
        assertThat(last.outputHash()).isNull();
    }

    private List<AuditEvent> collect(String systemId) {
        return auditLog.stream(systemId, Instant.EPOCH, Instant.now().plusSeconds(60)).toList();
    }

    @AiActHighRiskSystem(
            id = "aspect-system",
            name = "Aspect test",
            category = AnnexIIICategory.EMPLOYMENT_AND_WORKERS_MANAGEMENT,
            intendedPurpose = "Aspect test fixture",
            provider = "test"
    )
    @Component
    static class SampleAnnotated {
        @AiActLog
        public String score(String in) {
            return in.toUpperCase();
        }

        @AiActLog
        public void fails() {
            throw new IllegalStateException("secret-Mario-Rossi-not-found");
        }

        @AiActLog(captureInput = false, captureOutput = false)
        public String scoreWithoutCapture(String in) {
            return in;
        }
    }

    @Configuration
    @EnableAspectJAutoProxy
    static class TestConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }

        @Bean
        HmacChain hmacChain() {
            return HmacChain.fromUtf8("aspect-test-secret");
        }

        @Bean
        AuditLogService auditLogService(HmacChain hmac, ObjectMapper mapper,
                                         org.springframework.core.env.Environment env) {
            String dir = env.getProperty("aiact.test.dir", "/tmp/spring-aiact-aspect-test");
            Path path = Path.of(dir);
            try {
                if (java.nio.file.Files.exists(path)) {
                    java.nio.file.Files.walk(path)
                            .sorted(java.util.Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(java.io.File::delete);
                }
            } catch (Exception ignored) { /* test cleanup best effort */ }
            return new NdjsonAuditLogService(path, hmac, mapper, false);
        }

        @Bean
        PayloadHasher payloadHasher(ObjectMapper mapper) {
            return new PayloadHasher(mapper);
        }

        @Bean
        UserPseudonymizer userPseudonymizer() {
            return UserPseudonymizer.noop();
        }

        @Bean
        MetadataSanitizer metadataSanitizer() {
            return new MetadataSanitizer();
        }

        @Bean
        AiActLoggingAspect aiActLoggingAspect(AuditLogService log, PayloadHasher h,
                                              UserPseudonymizer u, MetadataSanitizer s) {
            return new AiActLoggingAspect(log, h, u, s);
        }

        @Bean
        SampleAnnotated sampleAnnotated() {
            return new SampleAnnotated();
        }
    }
}
