/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the auto-configuration refuses to start when the HMAC secret is left at the default
 * placeholder in a non-development environment, and that the guard can be bypassed via property
 * (for emergency hotfix scenarios) or by activating a development profile.
 */
class HmacFailFastTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AiActAutoConfiguration.class));

    @Test
    void failsToStartWhenSecretIsDefaultAndNoDevProfile() {
        runner
                .withPropertyValues(
                        "aiact.endpoints.enabled=false",
                        "aiact.retention-sweeper.enabled=false")
                .run(ctx -> assertThat(ctx)
                        .hasFailed()
                        .getFailure()
                        .rootCause()
                        .hasMessageContaining("spring-aiact refuses to start")
                        .hasMessageContaining("change-me-please"));
    }

    @Test
    void startsWhenDevProfileActive() {
        runner
                .withPropertyValues(
                        "aiact.endpoints.enabled=false",
                        "aiact.retention-sweeper.enabled=false",
                        "spring.profiles.active=dev")
                .run(ctx -> assertThat(ctx).hasNotFailed());
    }

    @Test
    void startsWhenSecretOverridden() {
        runner
                .withPropertyValues(
                        "aiact.endpoints.enabled=false",
                        "aiact.retention-sweeper.enabled=false",
                        "aiact.hmac.secret=a-real-secret-that-came-from-vault")
                .run(ctx -> assertThat(ctx).hasNotFailed());
    }

    @Test
    void startsWhenGuardExplicitlyDisabled() {
        runner
                .withPropertyValues(
                        "aiact.endpoints.enabled=false",
                        "aiact.retention-sweeper.enabled=false",
                        "aiact.hmac.fail-on-default-in-prod=false")
                .run(ctx -> assertThat(ctx).hasNotFailed());
    }
}
