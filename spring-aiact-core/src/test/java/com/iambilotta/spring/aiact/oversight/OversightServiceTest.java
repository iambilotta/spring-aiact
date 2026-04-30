/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.oversight;

import com.iambilotta.spring.aiact.audit.AuditLogService;
import com.iambilotta.spring.aiact.audit.MetadataSanitizer;
import com.iambilotta.spring.aiact.model.AuditEvent;
import com.iambilotta.spring.aiact.model.EventKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OversightServiceTest {

    private RecordingAuditLog auditLog;
    private OversightService service;

    @BeforeEach
    void setUp() {
        auditLog = new RecordingAuditLog();
        service = new OversightService(auditLog, new MetadataSanitizer());
    }

    @Test
    void recordsAcceptAsOverrideKind() {
        UUID linked = UUID.randomUUID();
        AuditEvent recorded = service.recordOverride(new OversightOverride(
                "emp:124857", "accept", "Looks fine", "demo-system", linked));

        assertThat(recorded.eventKind()).isEqualTo(EventKind.OVERRIDE);
        assertThat(recorded.linkedEventId()).isEqualTo(linked);
        assertThat(recorded.userIdPseudonymized()).isEqualTo("emp:124857");
        assertThat(recorded.metadata()).containsEntry("decision", "accept");
        assertThat(auditLog.appended).hasSize(1);
    }

    @Test
    void recordsStopAsStopKindWithoutLinkedEventId() {
        AuditEvent recorded = service.recordOverride(new OversightOverride(
                "emp:1", "stop", "Halt the system", "demo-system", null));
        assertThat(recorded.eventKind()).isEqualTo(EventKind.STOP);
        assertThat(recorded.linkedEventId()).isNull();
    }

    @Test
    void recordsFlagAnomalyAsAnomalyKind() {
        UUID linked = UUID.randomUUID();
        AuditEvent recorded = service.recordOverride(new OversightOverride(
                "emp:1", "flag-anomaly", "Drift detected", "demo-system", linked));
        assertThat(recorded.eventKind()).isEqualTo(EventKind.ANOMALY);
    }

    @Test
    void rejectsUnknownDecision() {
        assertThatThrownBy(() -> service.recordOverride(new OversightOverride(
                "emp:1", "delete-everything", "yolo", "demo-system", UUID.randomUUID())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("decision must be one of");
    }

    @Test
    void rejectsMissingActor() {
        assertThatThrownBy(() -> service.recordOverride(new OversightOverride(
                "", "accept", "no actor", "demo-system", UUID.randomUUID())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("actor required");
    }

    @Test
    void rejectsMissingSystemId() {
        assertThatThrownBy(() -> service.recordOverride(new OversightOverride(
                "emp:1", "accept", "no system", "  ", UUID.randomUUID())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("systemId required");
    }

    @Test
    void rejectsMissingLinkedEventIdForNonStopDecision() {
        assertThatThrownBy(() -> service.recordOverride(new OversightOverride(
                "emp:1", "accept", "ok", "demo-system", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("linkedEventId required");
    }

    @Test
    void truncatesLongReasonAtSanitizerLimit() {
        String veryLong = "a".repeat(1024);
        AuditEvent recorded = service.recordOverride(new OversightOverride(
                "emp:1", "accept", veryLong, "demo-system", UUID.randomUUID()));
        assertThat(recorded.metadata().get("reason")).hasSize(MetadataSanitizer.DEFAULT_MAX_VALUE_LENGTH);
    }

    private static class RecordingAuditLog implements AuditLogService {
        private final List<AuditEvent> appended = new ArrayList<>();

        @Override public AuditEvent append(AuditEvent event) {
            AuditEvent stamped = event.withRecordHmac("test-hmac");
            appended.add(stamped);
            return stamped;
        }
        @Override public Stream<AuditEvent> stream(String systemId, Instant from, Instant to) {
            return appended.stream();
        }
        @Override public ChainVerification verify(String systemId, Instant from, Instant to) {
            return new ChainVerification(systemId, from, to, appended.size(), 0, List.of());
        }
    }
}
