/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.audit;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.iambilotta.spring.aiact.model.AuditEvent;

import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Append-only Article 12 audit log. Implementations must guarantee strict append ordering and
 * must never rewrite a previously written record.
 */
public interface AuditLogService {

    /**
     * Append the given event, after the implementation has stamped the HMAC chain on it.
     *
     * @return the persisted event, with {@code prevHmac} and {@code recordHmac} populated.
     */
    AuditEvent append(AuditEvent event);

    /**
     * Stream the events stored for {@code systemId} that fall within the inclusive time range.
     * The stream must be consumed in a try-with-resources to release any underlying handle.
     */
    Stream<AuditEvent> stream(String systemId, Instant from, Instant to);

    /**
     * Verify the HMAC chain integrity for {@code systemId} between {@code from} and {@code to}.
     * Returns a verification report ready to be embedded in an audit submission.
     */
    ChainVerification verify(String systemId, Instant from, Instant to);

    /**
     * Serialise {@code event} to the writer in the canonical NDJSON form used by this audit
     * log, followed by a newline. Implementations decide the JSON shape (the default
     * {@link NdjsonAuditLogService} uses snake_case keys to match the Article 12 schema and
     * to keep the SHA-256 input deterministic). Callers should only need this for streaming
     * exports; routine writes go through {@link #append(AuditEvent)}.
     *
     * <p>The default implementation throws {@link UnsupportedOperationException}: Article 12
     * exports are an NDJSON-specific concern and any non-NDJSON sink that does not opt in
     * will reject this call rather than silently emit a different shape.
     */
    default void writeJsonLine(Writer w, AuditEvent event) throws IOException {
        throw new UnsupportedOperationException(
                getClass().getName() + " does not support NDJSON streaming export");
    }

    /**
     * Return the current head of the HMAC chain for {@code systemId}: the
     * {@code record_hmac} of the most recent record, or {@link HmacChain#CHAIN_SEED}
     * if the system has never logged. Acts as a cheap tamper canary: an external
     * verifier can poll this endpoint and notice if the head moves backward.
     *
     * <p>The default implementation walks the stream to the end; concrete sinks that
     * can answer in O(1) (for example by keeping the last HMAC in memory) should
     * override.
     */
    default ChainHead head(String systemId) {
        String last = HmacChain.CHAIN_SEED;
        try (Stream<AuditEvent> s = stream(systemId, null, null)) {
            Iterator<AuditEvent> it = s.iterator();
            while (it.hasNext()) {
                AuditEvent e = it.next();
                if (e.recordHmac() != null) {
                    last = e.recordHmac();
                }
            }
        }
        return new ChainHead(systemId, last);
    }

    /**
     * Current head of the HMAC chain for a system.
     *
     * <p>Wire format keeps {@code snake_case} keys to match the Article 12 NDJSON
     * schema: a downstream verifier can compare the {@code head_hmac} field against
     * the last {@code record_hmac} of an exported slice without translating naming
     * conventions.
     */
    record ChainHead(
            @JsonProperty("system_id") String systemId,
            @JsonProperty("head_hmac") String headHmac
    ) {}

    /**
     * Result of an HMAC chain verification run.
     */
    record ChainVerification(
            String systemId,
            Instant from,
            Instant to,
            long inspected,
            long invalid,
            List<String> failedEventIds
    ) {
        public boolean valid() {
            return invalid == 0;
        }
    }
}
