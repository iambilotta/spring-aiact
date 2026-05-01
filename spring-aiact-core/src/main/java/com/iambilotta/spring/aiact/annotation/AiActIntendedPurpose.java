/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Detailed Article 13 intended purpose used to generate the Instructions for Use document.
 * Where {@link AiActHighRiskSystem#intendedPurpose()} is a one-liner, this annotation captures
 * the granular fields that Article 13 requires (the deployment context, the categories of
 * users, the foreseeable misuse, the geography of the deployment).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface AiActIntendedPurpose {

    /**
     * Description of the deployment context and operational environment.
     */
    String deploymentContext();

    /**
     * Intended categories of users (for example {@code "HR specialists"},
     * {@code "loan officers"}, {@code "triage nurses"}).
     */
    String[] users();

    /**
     * Foreseeable forms of misuse the deployer must be made aware of.
     */
    String[] foreseeableMisuse() default {};

    /**
     * Geographies the system has been validated for.
     */
    String[] geographies() default {"EU"};

    /**
     * Languages the system has been validated for. ISO 639-1 codes preferred.
     */
    String[] languages() default {};
}
