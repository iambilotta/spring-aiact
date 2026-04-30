/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.oversight;

import com.iambilotta.spring.aiact.audit.AuditLogService;
import com.iambilotta.spring.aiact.audit.MetadataSanitizer;
import com.iambilotta.spring.aiact.model.AuditEvent;
import com.iambilotta.spring.aiact.model.EventKind;

import java.util.HashMap;
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
    private final MetadataSanitizer metadataSanitizer;

    public OversightService(AuditLogService auditLog, MetadataSanitizer metadataSanitizer) {
        this.auditLog = auditLog;
        this.metadataSanitizer = metadataSanitizer;
    }

    /** Convenience for callers wiring without the starter. */
    public OversightService(AuditLogService auditLog) {
        this(auditLog, new MetadataSanitizer());
    }

    public AuditEvent recordOverride(OversightOverride override) {
        validate(override);
        EventKind kind = "stop".equals(override.decision()) ? EventKind.STOP
                : "flag-anomaly".equals(override.decision()) ? EventKind.ANOMALY
                : EventKind.OVERRIDE;
        Map<String, String> raw = new HashMap<>();
        raw.put("decision", override.decision());
        raw.put("reason", override.reason() == null ? "" : override.reason());
        AuditEvent event = AuditEvent.builder()
                .eventKind(kind)
                .systemId(override.systemId())
                .systemVersion("oversight")
                .operation("article-14-override")
                .userIdPseudonymized(override.actor())
                .verifierId(override.actor())
                .linkedEventId(override.linkedEventId())
                .metadata(metadataSanitizer.sanitize(raw))
                .build();
        return auditLog.append(event);
    }

    private void validate(OversightOverride override) {
        if (override == null) {
            throw new IllegalArgumentException(
                    "override payload required. POST /aiact/oversight/{eventId}/override with a JSON body "
                    + "containing actor, decision, reason, systemId.");
        }
        if (override.actor() == null || override.actor().isBlank()) {
            throw new IllegalArgumentException(
                    "actor required: every Article 14 override must record the natural person performing it. "
                    + "Provide a stable badge id, employee id, or role-tag in the 'actor' field.");
        }
        if (override.systemId() == null || override.systemId().isBlank()) {
            throw new IllegalArgumentException(
                    "systemId required: the override must reference the AI system id declared on "
                    + "@AiActHighRiskSystem. Provide it in the 'systemId' field of the request body.");
        }
        if (override.decision() == null || !ALLOWED_DECISIONS.contains(override.decision())) {
            throw new IllegalArgumentException(
                    "decision must be one of " + ALLOWED_DECISIONS + ", got '" + override.decision()
                    + "'. The audit log only records verbs an assessor can interpret; pick the closest "
                    + "fit, do not invent a new one.");
        }
        if (!"stop".equals(override.decision()) && override.linkedEventId() == null) {
            throw new IllegalArgumentException(
                    "linkedEventId required for decision '" + override.decision()
                    + "'. The override is recorded as a separate audit event linked to the original "
                    + "INVOCATION; provide the original event_id in linkedEventId. Only 'stop' may be "
                    + "submitted without a linked event.");
        }
    }
}
