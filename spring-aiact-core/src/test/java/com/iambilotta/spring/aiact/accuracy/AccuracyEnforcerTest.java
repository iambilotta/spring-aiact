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

class AccuracyEnforcerTest {

    @AiActAccuracyMetric(metric = RiskMetric.ACCURACY, threshold = ">=0.92")
    static class WellCalibrated {
    }

    @AiActAccuracyMetric(metric = RiskMetric.FALSE_POSITIVE_RATE, threshold = "<=0.05")
    static class FprBounded {
    }

    @Test
    void passesWhenAGreaterEqualThresholdIsMet() {
        AccuracyEnforcer.Result r = AccuracyEnforcer.evaluate(">=0.92", 0.94);
        assertThat(r.belowThreshold()).isFalse();
        assertThat(r.measured()).isEqualTo(0.94);
    }

    @Test
    void failsAndSignalsWhenAGreaterEqualThresholdIsMissed() {
        AccuracyEnforcer.Result r = AccuracyEnforcer.evaluate(">=0.92", 0.90);
        assertThat(r.belowThreshold()).isTrue();
        assertThat(r.describe()).contains("0.9").contains("0.92");
    }

    @Test
    void honoursAnUpperBoundThresholdForErrorRates() {
        assertThat(AccuracyEnforcer.evaluate("<=0.05", 0.04).belowThreshold()).isFalse();
        assertThat(AccuracyEnforcer.evaluate("<=0.05", 0.07).belowThreshold()).isTrue();
    }

    @Test
    void treatsABareNumberAsAGreaterEqualThreshold() {
        assertThat(AccuracyEnforcer.evaluate("0.80", 0.80).belowThreshold()).isFalse();
        assertThat(AccuracyEnforcer.evaluate("0.80", 0.79).belowThreshold()).isTrue();
    }

    @Test
    void readsTheThresholdStraightFromTheAnnotation() {
        AiActAccuracyMetric a = WellCalibrated.class.getAnnotation(AiActAccuracyMetric.class);
        AccuracyEnforcer.Result r = AccuracyEnforcer.evaluate(a.threshold(), 0.99);
        assertThat(r.belowThreshold()).isFalse();

        AiActAccuracyMetric fpr = FprBounded.class.getAnnotation(AiActAccuracyMetric.class);
        assertThat(AccuracyEnforcer.evaluate(fpr.threshold(), 0.06).belowThreshold()).isTrue();
    }

    @Test
    void enforceThrowsBelowThresholdSoABuildOrEvalHookCanGateOnIt() {
        assertThatThrownBy(() -> AccuracyEnforcer.enforce(RiskMetric.ACCURACY, ">=0.92", 0.50))
                .isInstanceOf(AccuracyThresholdViolation.class)
                .hasMessageContaining("ACCURACY");
    }

    @Test
    void rejectsAnUnparseableThreshold() {
        assertThatThrownBy(() -> AccuracyEnforcer.evaluate("almost good", 0.9))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
