/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.audit;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Sanitizes the {@code metadata} map before it lands on an Article 12 audit record. The audit
 * log is not a place for stack traces, raw exception messages, or arbitrary string fragments
 * that may carry personal data. This class is the single chokepoint that enforces:
 *
 * <ul>
 *   <li>a whitelist of allowed keys (extensible via the constructor),</li>
 *   <li>a maximum length per value (default 256 chars),</li>
 *   <li>a {@link #describeException(Throwable)} helper that emits the exception class name and a
 *       hash-derived fingerprint of the message instead of the raw message.</li>
 * </ul>
 *
 * The whitelist defaults are deliberately small: {@code decision}, {@code reason},
 * {@code exception_class}, {@code message_fingerprint}. Anything outside the whitelist is dropped
 * with a counter increment in the resulting map.
 */
public final class MetadataSanitizer {

    public static final int DEFAULT_MAX_VALUE_LENGTH = 256;
    public static final Set<String> DEFAULT_ALLOWED_KEYS = Set.of(
            "decision",
            "reason",
            "exception_class",
            "message_fingerprint"
    );

    private final Set<String> allowedKeys;
    private final int maxValueLength;

    public MetadataSanitizer() {
        this(DEFAULT_ALLOWED_KEYS, DEFAULT_MAX_VALUE_LENGTH);
    }

    public MetadataSanitizer(Set<String> allowedKeys, int maxValueLength) {
        this.allowedKeys = Set.copyOf(allowedKeys);
        this.maxValueLength = maxValueLength;
    }

    public Map<String, String> sanitize(Map<String, String> input) {
        if (input == null || input.isEmpty()) return Map.of();
        Map<String, String> out = new LinkedHashMap<>();
        int dropped = 0;
        for (Map.Entry<String, String> e : input.entrySet()) {
            String key = e.getKey();
            if (key == null || !allowedKeys.contains(key)) {
                dropped++;
                continue;
            }
            String v = e.getValue();
            if (v == null) {
                out.put(key, "");
                continue;
            }
            if (v.length() > maxValueLength) {
                v = v.substring(0, maxValueLength);
            }
            out.put(key, v);
        }
        if (dropped > 0) {
            out.put("dropped_keys_count", String.valueOf(dropped));
        }
        return out;
    }

    /**
     * Describes an exception as two safe fields: the fully qualified class name and a
     * deterministic SHA-256 prefix of the message (lower-case hex, 16 chars). The raw message
     * is never recorded.
     */
    public Map<String, String> describeException(Throwable t) {
        if (t == null) return Map.of();
        String cls = t.getClass().getName();
        String msg = t.getMessage();
        String fingerprint = msg == null || msg.isEmpty()
                ? "no-message"
                : HmacChain.sha256Hex(msg).substring(0, 16);
        return sanitize(Map.of("exception_class", cls, "message_fingerprint", fingerprint));
    }
}
