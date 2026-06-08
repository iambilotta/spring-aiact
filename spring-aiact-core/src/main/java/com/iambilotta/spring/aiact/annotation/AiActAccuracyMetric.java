/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares an Article 15 accuracy or robustness metric that the system commits to.
 * The metric is forwarded to the technical file accuracy section. The library does not
 * compute the metric for you: your harness (test suite or evaluation pipeline) measures it.
 * <p>
 * Given a measured value, the threshold declared here is enforceable via
 * {@code com.iambilotta.spring.aiact.accuracy.AccuracyEnforcer}: it compares the value to the
 * {@link #threshold()} and signals (or throws) when the metric falls below it, so a build
 * assertion or eval hook can gate on Article 15 accuracy instead of only recording it.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Repeatable(AiActAccuracyMetric.List.class)
public @interface AiActAccuracyMetric {

    RiskMetric metric();

    /**
     * Free metric name, mandatory only when {@link #metric()} is {@link RiskMetric#CUSTOM}.
     */
    String name() default "";

    /**
     * Threshold the system commits to (numeric, encoded as a string to allow ranges or
     * inequalities like {@code ">=0.92"}).
     */
    String threshold();

    /**
     * Test harness or evaluation pipeline reference (file path, URL, or test class).
     */
    String harness() default "";

    /**
     * Population or slice the metric applies to (for example {@code "production-traffic"}
     * or {@code "demographic.gender=female"}).
     */
    String population() default "all";

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @interface List {
        AiActAccuracyMetric[] value();
    }
}
