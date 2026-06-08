/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.accuracy;

import com.iambilotta.spring.aiact.annotation.AiActAccuracyMetric;
import com.iambilotta.spring.aiact.annotation.RiskMetric;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * First-class Article 15 accuracy gate (REQ-AIACT-006). Where {@link AccuracyEnforcer} is a static
 * façade the adopter must remember to call, this lifts enforcement to the same tier as the build
 * plugin and the runtime advisor: the {@code @AiActAccuracyMetric} annotations <em>drive</em> the
 * gate. The adopter reflects the subject once, feeds in the values their evaluation harness
 * measured, and every declared metric is enforced together, with a declared-but-not-measured metric
 * failing loudly rather than silently passing.
 *
 * <p>Usage as a batteries-included JUnit 5 extension (auto-enforces after the test body):
 * <pre>{@code
 * @RegisterExtension
 * static AiActAccuracyExtension accuracy = AiActAccuracyExtension.forSubject(HiringScreener.class);
 *
 * @Test
 * void meetsArticle15Thresholds() {
 *     // ... run your eval harness ...
 *     accuracy.measured(RiskMetric.PRECISION, measuredPrecision)
 *             .measured(RiskMetric.FALSE_POSITIVE_RATE, measuredFpr);
 * }
 * }</pre>
 *
 * <p>Or imperatively, calling {@link #enforce()} yourself. Both paths read the thresholds straight
 * off the annotation, so the test cannot drift from the declared Article 15 commitment.
 */
public final class AiActAccuracyExtension implements AfterTestExecutionCallback, Extension {

    private final Class<?> subject;
    private final List<AiActAccuracyMetric> declared;
    private final Map<String, Double> measured = new LinkedHashMap<>();

    private AiActAccuracyExtension(Class<?> subject, List<AiActAccuracyMetric> declared) {
        this.subject = subject;
        this.declared = declared;
    }

    /**
     * Build a gate for the metrics declared on {@code subject} via {@code @AiActAccuracyMetric}
     * (single or repeated). Throws when the subject declares none, so a typo or a missing
     * annotation is caught at wiring time, not silently treated as "nothing to enforce".
     */
    public static AiActAccuracyExtension forSubject(Class<?> subject) {
        List<AiActAccuracyMetric> declared = List.of(subject.getAnnotationsByType(AiActAccuracyMetric.class));
        if (declared.isEmpty()) {
            throw new IllegalArgumentException(
                    "Class " + subject.getName() + " declares no @AiActAccuracyMetric to enforce. "
                    + "Annotate it with the Article 15 metric(s) it commits to, or do not wire an "
                    + "AiActAccuracyExtension for it.");
        }
        return new AiActAccuracyExtension(subject, declared);
    }

    /** Supply the value your harness measured for a standard {@link RiskMetric}. */
    public AiActAccuracyExtension measured(RiskMetric metric, double value) {
        measured.put(key(metric, null), value);
        return this;
    }

    /**
     * Supply the value for a {@link RiskMetric#CUSTOM} metric, keyed by the free name declared in
     * {@link AiActAccuracyMetric#name()}.
     */
    public AiActAccuracyExtension measured(String customName, double value) {
        measured.put(key(RiskMetric.CUSTOM, customName), value);
        return this;
    }

    /**
     * Enforce every declared metric against its supplied measured value. Throws
     * {@link AccuracyThresholdViolation} on the first metric below threshold (message names it), or
     * {@link IllegalStateException} if a declared metric was never measured (a silent pass would
     * defeat the Article 15 gate). Returns the per-metric results when all pass.
     */
    public List<AccuracyEnforcer.Result> enforce() {
        List<AccuracyEnforcer.Result> results = new ArrayList<>(declared.size());
        for (AiActAccuracyMetric m : declared) {
            String k = key(m.metric(), m.name());
            Double value = measured.get(k);
            if (value == null) {
                throw new IllegalStateException(
                        "No measured value supplied for Article 15 metric " + describe(m)
                        + " declared on " + subject.getName() + ". Call measured(...) for it before "
                        + "enforcing, or the gate cannot prove the threshold is met.");
            }
            AccuracyEnforcer.enforce(m.metric(), m.threshold(), value);
            results.add(AccuracyEnforcer.evaluate(m.threshold(), value));
        }
        return results;
    }

    /** JUnit hook: enforce after the test body, so {@code @RegisterExtension} needs no extra call. */
    @Override
    public void afterTestExecution(ExtensionContext context) {
        enforce();
    }

    private static String key(RiskMetric metric, String name) {
        if (metric == RiskMetric.CUSTOM) {
            return "CUSTOM:" + (name == null ? "" : name);
        }
        return metric.name();
    }

    private static String describe(AiActAccuracyMetric m) {
        return m.metric() == RiskMetric.CUSTOM ? "CUSTOM(" + m.name() + ")" : m.metric().name();
    }
}
