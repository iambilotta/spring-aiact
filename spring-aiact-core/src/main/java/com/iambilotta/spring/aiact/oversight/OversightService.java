/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.oversight;

import com.iambilotta.spring.aiact.audit.AuditLogService;
import com.iambilotta.spring.aiact.model.AuditEvent;
import com.iambilotta.spring.aiact.model.EventKind;

import java.util.Map;
import java.util.Set;

/**
 * Records Article 14 override events. Each override is appended as a second event linked to the
 * original event via {@code linked_event_id}. The service does not enforce policy on what an
 * override means: that decision belongs to the deployer.
 */
public class OversightService {

    private static final Set<String> ALLOWED_DECISIONS = Set.of(
            "accept", "reject", "stop", "flag-anomaly"
    );

    private final AuditLogService auditLog;

    public OversightService(AuditLogService auditLog) {
        this.auditLog = auditLog;
    }

    public AuditEvent recordOverride(OversightOverride override) {
        validate(override);
        EventKind kind = "stop".equals(override.decision()) ? EventKind.STOP
                : "flag-anomaly".equals(override.decision()) ? EventKind.ANOMALY
                : EventKind.OVERRIDE;
        AuditEvent event = AuditEvent.builder()
                .eventKind(kind)
                .systemId(override.systemId())
                .systemVersion("oversight")
                .operation("article-14-override")
                .userIdPseudonymized(override.actor())
                .verifierId(override.actor())
                .linkedEventId(override.linkedEventId())
                .metadata(Map.of(
                        "decision", override.decision(),
                        "reason", override.reason() == null ? "" : override.reason()
                ))
                .build();
        return auditLog.append(event);
    }

    private void validate(OversightOverride override) {
        if (override == null) {
            throw new IllegalArgumentException("override payload required");
        }
        if (override.actor() == null || override.actor().isBlank()) {
            throw new IllegalArgumentException("actor required");
        }
        if (override.systemId() == null || override.systemId().isBlank()) {
            throw new IllegalArgumentException("systemId required");
        }
        if (override.decision() == null || !ALLOWED_DECISIONS.contains(override.decision())) {
            throw new IllegalArgumentException(
                    "decision must be one of " + ALLOWED_DECISIONS + ", got " + override.decision());
        }
        if (!"stop".equals(override.decision()) && override.linkedEventId() == null) {
            throw new IllegalArgumentException("linkedEventId required for decision " + override.decision());
        }
    }
}
