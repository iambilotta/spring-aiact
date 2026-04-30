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
 * Declares the human oversight model required by AI Act Article 14 for the annotated component.
 * The advisor uses this declaration to resolve which override endpoint is exposed and which
 * actor must be recorded on the override audit event.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface AiActOversight {

    /**
     * Required oversight level.
     */
    OversightLevel level();

    /**
     * Free description of the oversight measures actually in place (escalation channel,
     * stop button, anomaly flagging). Forwarded verbatim to the technical file.
     */
    String description() default "";

    /**
     * Logical role that is allowed to override (for example {@code clinician},
     * {@code reviewer}, {@code supervisor}). The endpoint enforces the actor field
     * but does not interpret its meaning.
     */
    String overrideRole() default "supervisor";
}
