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
 * Declares the EU AI Act {@link RiskClass} of an AI system explicitly. Additive to
 * {@code @AiActHighRiskSystem}: a high-risk system needs only that annotation (it implies
 * {@link RiskClass#HIGH_RISK}); this annotation is for the other bands, in particular marking a
 * system {@link RiskClass#PROHIBITED} so the build gate refuses it by construction (REQ-AIACT-012),
 * or {@link RiskClass#LIMITED} to pair with {@code @AiActTransparency}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AiActRiskClass {

    /** The declared risk band. */
    RiskClass value();

    /** Optional justification for the classification, surfaced in the build report. */
    String rationale() default "";
}
