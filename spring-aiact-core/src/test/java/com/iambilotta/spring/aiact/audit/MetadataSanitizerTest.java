/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.audit;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MetadataSanitizerTest {

    @Test
    void dropsKeysOutsideTheWhitelist() {
        MetadataSanitizer s = new MetadataSanitizer();
        Map<String, String> in = new HashMap<>();
        in.put("decision", "reject");
        in.put("ssn", "123-45-6789");
        in.put("email", "candidate@example.com");

        Map<String, String> out = s.sanitize(in);

        assertThat(out).containsEntry("decision", "reject");
        assertThat(out).doesNotContainKeys("ssn", "email");
        assertThat(out).containsEntry("dropped_keys_count", "2");
    }

    @Test
    void truncatesValuesAtTheConfiguredMaxLength() {
        MetadataSanitizer s = new MetadataSanitizer(Set.of("reason"), 16);
        String longValue = "a".repeat(64);

        Map<String, String> out = s.sanitize(Map.of("reason", longValue));

        assertThat(out.get("reason")).hasSize(16);
    }

    @Test
    void describeExceptionEmitsClassNameAndFingerprintNotMessage() {
        Throwable t = new IllegalStateException("Mario Rossi profile id=42 not found");

        Map<String, String> out = new MetadataSanitizer().describeException(t);

        assertThat(out)
                .containsEntry("exception_class", "java.lang.IllegalStateException")
                .containsKey("message_fingerprint");
        assertThat(out.get("message_fingerprint")).hasSize(16);
        assertThat(out.values()).allSatisfy(v -> {
            assertThat(v).doesNotContain("Mario Rossi");
            assertThat(v).doesNotContain("id=42");
        });
    }

    @Test
    void describeExceptionWithoutMessageEmitsNoMessageMarker() {
        Map<String, String> out = new MetadataSanitizer()
                .describeException(new NullPointerException());

        assertThat(out).containsEntry("message_fingerprint", "no-message");
    }

    @Test
    void emptyOrNullInputReturnsEmptyMap() {
        MetadataSanitizer s = new MetadataSanitizer();
        assertThat(s.sanitize(null)).isEmpty();
        assertThat(s.sanitize(Map.of())).isEmpty();
    }
}
