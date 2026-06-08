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
 * Marks a Spring bean (typically a controller or a service exposed over an AI surface) as subject
 * to the EU AI Act Article 50 transparency obligation: a natural person interacting with the AI
 * system, or receiving AI-generated content, must be told so clearly and machine-detectably.
 * <p>
 * This is the limited-risk transparency band, distinct from {@link AiActHighRiskSystem}. A single
 * system can be both (a high-risk chatbot still owes Article 50 disclosure). The starter stamps the
 * disclosure as a response header on every request handled by the annotated surface; the message is
 * also available to render in the UI.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AiActTransparency {

    /**
     * Which Article 50 obligation this surface satisfies (interaction or generated content).
     */
    TransparencyKind kind();

    /**
     * Human-readable disclosure shown to the natural person. When empty, a default phrased for
     * the {@link #kind()} is used.
     */
    String disclosure() default "";
}
