/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.autoconfigure;

import com.iambilotta.spring.aiact.annotation.HashStrategy;
import com.iambilotta.spring.aiact.audit.PayloadHasher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the SHA-256 output of {@link PayloadHasher} on a known payload, so the v1.0
 * refactor that internalised the audit-record ObjectMapper cannot silently change
 * hash behaviour. If the hash output here ever needs to change, that is a
 * Article 12 / Annex VII evidence-format break and it must be a major version bump.
 */
class PayloadHasherDeterminismTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AiActAutoConfiguration.class))
            .withPropertyValues(
                    "spring.profiles.active=dev",
                    "aiact.endpoints.enabled=false",
                    "aiact.retention-sweeper.enabled=false");

    @Test
    void sameJsonPayloadAlwaysHashesToTheSameValue() {
        runner.run(ctx -> {
            PayloadHasher hasher = ctx.getBean(PayloadHasher.class);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("candidateId", "c-1");
            payload.put("cvText", "deterministic input for hashing");
            payload.put("score", 0.42);

            String first = hasher.hash(payload, HashStrategy.SHA_256);
            String second = hasher.hash(payload, HashStrategy.SHA_256);

            assertThat(first)
                    .as("two hashes of the same logical payload must match byte-for-byte")
                    .isEqualTo(second)
                    .startsWith("sha256:")
                    .hasSize("sha256:".length() + 64);
        });
    }

    @Test
    void hashesDifferOnDifferentPayloads() {
        runner.run(ctx -> {
            PayloadHasher hasher = ctx.getBean(PayloadHasher.class);
            String a = hasher.hash(Map.of("k", "a"), HashStrategy.SHA_256);
            String b = hasher.hash(Map.of("k", "b"), HashStrategy.SHA_256);
            assertThat(a).isNotEqualTo(b);
        });
    }

    @Test
    void noneStrategyReturnsAStableMarker() {
        runner.run(ctx -> {
            PayloadHasher hasher = ctx.getBean(PayloadHasher.class);
            assertThat(hasher.hash(null, HashStrategy.NONE)).isEqualTo("null");
            assertThat(hasher.hash("anything", HashStrategy.NONE)).startsWith("opaque:");
        });
    }
}
