/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.accuracy;

import com.iambilotta.spring.aiact.annotation.AiActAccuracyMetric;
import com.iambilotta.spring.aiact.annotation.RiskMetric;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the first-class Art.15 accuracy hook: instead of calling the static
 * {@link AccuracyEnforcer} façade and remembering to fail the build, an adopter registers an
 * {@link AiActAccuracyExtension} for the annotated subject, feeds it the measured values from its
 * own evaluation harness, and gets every {@code @AiActAccuracyMetric} on that subject enforced in
 * one call (REQ-AIACT-006). This is the same enforcement tier as the build plugin and the runtime
 * advisor: the annotation drives the gate.
 */
class AiActAccuracyExtensionTest {

    @AiActAccuracyMetric(metric = RiskMetric.PRECISION, threshold = ">=0.92")
    @AiActAccuracyMetric(metric = RiskMetric.FALSE_POSITIVE_RATE, threshold = "<=0.05")
    static class TwoMetricSystem {
    }

    @Test
    void passesWhenEveryDeclaredMetricMeetsItsThreshold() {
        AiActAccuracyExtension gate = AiActAccuracyExtension.forSubject(TwoMetricSystem.class)
                .measured(RiskMetric.PRECISION, 0.95)
                .measured(RiskMetric.FALSE_POSITIVE_RATE, 0.03);

        var results = gate.enforce();

        assertThat(results).hasSize(2);
        assertThat(results).noneMatch(AccuracyEnforcer.Result::belowThreshold);
    }

    @Test
    void throwsNamingTheMetricWhenADeclaredMetricIsBelowThreshold() {
        AiActAccuracyExtension gate = AiActAccuracyExtension.forSubject(TwoMetricSystem.class)
                .measured(RiskMetric.PRECISION, 0.80)
                .measured(RiskMetric.FALSE_POSITIVE_RATE, 0.03);

        assertThatThrownBy(gate::enforce)
                .isInstanceOf(AccuracyThresholdViolation.class)
                .hasMessageContaining("PRECISION");
    }

    @Test
    void refusesToSilentlyPassWhenADeclaredMetricHasNoMeasuredValue() {
        AiActAccuracyExtension gate = AiActAccuracyExtension.forSubject(TwoMetricSystem.class)
                .measured(RiskMetric.PRECISION, 0.95);
        // FALSE_POSITIVE_RATE is declared but never measured: the gate must not declare success.

        assertThatThrownBy(gate::enforce)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FALSE_POSITIVE_RATE");
    }

    @Test
    void rejectsASubjectThatDeclaresNoAccuracyMetric() {
        class NotAnnotated {
        }
        assertThatThrownBy(() -> AiActAccuracyExtension.forSubject(NotAnnotated.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@AiActAccuracyMetric");
    }
}
