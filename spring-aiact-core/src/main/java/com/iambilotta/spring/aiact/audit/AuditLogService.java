/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.audit;

import com.iambilotta.spring.aiact.model.AuditEvent;

import java.time.Instant;
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
