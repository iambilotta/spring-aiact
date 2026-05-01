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
 * The metric is forwarded to the technical file accuracy section. The starter does not
 * compute these metrics for you: it only records what you have measured and the harness
 * (test suite or evaluation pipeline) that produced the value.
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
