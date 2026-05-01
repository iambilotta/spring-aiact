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
 * Marks a Spring bean (typically a controller, service, or domain class) as the entry point of
 * a high-risk AI system within the meaning of EU AI Act Article 6.2 and Annex III. The annotation
 * is the anchor used by the build-time generators to populate the Annex IV technical file and by
 * the {@code @AiActLog} aspect to attribute Article 12 audit records to a system.
 * <p>
 * Use exactly one {@code @AiActHighRiskSystem} per logical AI system. Sub-components reusing the
 * same system id should not re-declare it.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface AiActHighRiskSystem {

    /**
     * Stable system identifier used in audit logs and the Annex IV technical file.
     * Defaults to the simple class name when empty.
     */
    String id() default "";

    /**
     * Human-readable system name used in the Annex IV "general description" section
     * and in the Article 47 declaration of conformity.
     */
    String name();

    /**
     * Annex III category the system falls under. Custom Annex III sub-points should be expressed
     * in the {@link #annexSubpoint()} field, never by inventing new enum values.
     */
    AnnexIIICategory category();

    /**
     * Optional Annex III sub-point (for example {@code "1(a)"} for remote biometric identification).
     */
    String annexSubpoint() default "";

    /**
     * Short one-line intended purpose, used as a summary. Detailed Article 13 instructions
     * for use should be provided via {@link AiActIntendedPurpose} on the same class.
     */
    String intendedPurpose();

    /**
     * Provider name as defined by AI Act Article 3, point 3 (the natural or legal person that develops
     * an AI system or that has it developed and places it on the market under its name or trademark).
     */
    String provider();

    /**
     * Optional version override. When empty, the build-time generator uses the Maven coordinates
     * and the git SHA discovered at build time.
     */
    String version() default "";
}
