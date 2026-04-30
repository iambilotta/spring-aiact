/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.Instant;
import java.util.UUID;

/**
 * Single Article 12 audit record. Persisted as one NDJSON line, immutable, chained with the
 * previous record via {@link #prevHmac()} and authenticated via {@link #recordHmac()}.
 * <p>
 * The schema is intentionally narrow: hashes (never raw payloads), an optional pseudonymous user
 * identifier, the model id, the latency and a logical database reference. Anything else belongs
 * outside the audit log and should be referenced via {@link #dbReference()}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "event_id", "event_kind", "timestamp", "system_id", "system_version", "operation",
        "user_id_pseudonymized", "model_id", "input_hash", "output_hash", "hash_algorithm",
        "latency_ms", "db_reference", "verifier_id", "linked_event_id", "correlation_id",
        "metadata", "prev_hmac", "record_hmac"
})
public record AuditEvent(
        UUID eventId,
        EventKind eventKind,
        Instant timestamp,
        String systemId,
        String systemVersion,
        String operation,
        String userIdPseudonymized,
        String modelId,
        String inputHash,
        String outputHash,
        String hashAlgorithm,
        Long latencyMs,
        String dbReference,
        String verifierId,
        UUID linkedEventId,
        String correlationId,
        java.util.Map<String, String> metadata,
        String prevHmac,
        String recordHmac
) {

    public AuditEvent withRecordHmac(String hmacHex) {
        return new AuditEvent(
                eventId, eventKind, timestamp, systemId, systemVersion, operation,
                userIdPseudonymized, modelId, inputHash, outputHash, hashAlgorithm,
                latencyMs, dbReference, verifierId, linkedEventId, correlationId,
                metadata, prevHmac, hmacHex
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private UUID eventId = UUID.randomUUID();
        private EventKind eventKind = EventKind.INVOCATION;
        private Instant timestamp = Instant.now();
        private String systemId;
        private String systemVersion;
        private String operation;
        private String userIdPseudonymized;
        private String modelId;
        private String inputHash;
        private String outputHash;
        private String hashAlgorithm;
        private Long latencyMs;
        private String dbReference;
        private String verifierId;
        private UUID linkedEventId;
        private String correlationId;
        private java.util.Map<String, String> metadata;

        public Builder eventId(UUID v) { this.eventId = v; return this; }
        public Builder eventKind(EventKind v) { this.eventKind = v; return this; }
        public Builder timestamp(Instant v) { this.timestamp = v; return this; }
        public Builder systemId(String v) { this.systemId = v; return this; }
        public Builder systemVersion(String v) { this.systemVersion = v; return this; }
        public Builder operation(String v) { this.operation = v; return this; }
        public Builder userIdPseudonymized(String v) { this.userIdPseudonymized = v; return this; }
        public Builder modelId(String v) { this.modelId = v; return this; }
        public Builder inputHash(String v) { this.inputHash = v; return this; }
        public Builder outputHash(String v) { this.outputHash = v; return this; }
        public Builder hashAlgorithm(String v) { this.hashAlgorithm = v; return this; }
        public Builder latencyMs(Long v) { this.latencyMs = v; return this; }
        public Builder dbReference(String v) { this.dbReference = v; return this; }
        public Builder verifierId(String v) { this.verifierId = v; return this; }
        public Builder linkedEventId(UUID v) { this.linkedEventId = v; return this; }
        public Builder correlationId(String v) { this.correlationId = v; return this; }
        public Builder metadata(java.util.Map<String, String> v) { this.metadata = v; return this; }

        public AuditEvent build() {
            return new AuditEvent(
                    eventId, eventKind, timestamp, systemId, systemVersion, operation,
                    userIdPseudonymized, modelId, inputHash, outputHash, hashAlgorithm,
                    latencyMs, dbReference, verifierId, linkedEventId, correlationId,
                    metadata, null, null
            );
        }
    }
}
