/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.security;

import org.junit.jupiter.api.Test;

import static com.iambilotta.spring.aiact.security.AiActEndpointGuard.Action;
import static org.assertj.core.api.Assertions.assertThat;

class AiActMockEndpointGuardTest {

    @Test
    void allowsExplicitlyConfiguredCombination() {
        AiActMockEndpointGuard guard = AiActMockEndpointGuard.builder()
                .allow("sys-a", Action.EXPORT_LOG)
                .denyAllByDefault()
                .build();

        assertThat(guard.authorize("sys-a", Action.EXPORT_LOG).allowed()).isTrue();
        assertThat(guard.authorize("sys-a", Action.SUBMIT_OVERRIDE).allowed()).isFalse();
        assertThat(guard.authorize("sys-b", Action.EXPORT_LOG).allowed()).isFalse();
    }

    @Test
    void denyTrumpsAllow() {
        AiActMockEndpointGuard guard = AiActMockEndpointGuard.builder()
                .allow("sys", Action.values())
                .deny("sys", Action.SUBMIT_OVERRIDE)
                .build();

        assertThat(guard.authorize("sys", Action.SUBMIT_OVERRIDE).allowed()).isFalse();
        assertThat(guard.authorize("sys", Action.EXPORT_LOG).allowed()).isTrue();
    }

    @Test
    void allowAllByDefaultLetsThroughUnknownSystems() {
        AiActMockEndpointGuard guard = AiActMockEndpointGuard.builder()
                .allowAllByDefault()
                .build();
        assertThat(guard.authorize("any", Action.READ_HEAD).allowed()).isTrue();
    }
}
