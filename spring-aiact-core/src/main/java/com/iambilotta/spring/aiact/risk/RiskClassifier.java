/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.risk;

import com.iambilotta.spring.aiact.annotation.AiActHighRiskSystem;
import com.iambilotta.spring.aiact.annotation.AiActRiskClass;
import com.iambilotta.spring.aiact.annotation.AiActTransparency;
import com.iambilotta.spring.aiact.annotation.RiskClass;

/**
 * Resolves the EU AI Act {@link RiskClass} of a type from its annotations (REQ-AIACT-012).
 *
 * <p>Resolution order, most-specific first:
 * <ol>
 *   <li>an explicit {@code @AiActRiskClass} wins (it can override anything, including marking a
 *       system {@link RiskClass#PROHIBITED});</li>
 *   <li>{@code @AiActHighRiskSystem} implies {@link RiskClass#HIGH_RISK};</li>
 *   <li>a bare {@code @AiActTransparency} surface is {@link RiskClass#LIMITED};</li>
 *   <li>otherwise {@link RiskClass#MINIMAL}.</li>
 * </ol>
 *
 * <p>Static façade over a default {@link Hook}; adopters can supply their own classification hook
 * (for example one that consults a registry) and call it directly.
 */
public final class RiskClassifier {

    /** Classification hook: an adopter can implement a richer policy than the annotations. */
    @FunctionalInterface
    public interface Hook {
        RiskClass classify(Class<?> type);
    }

    private RiskClassifier() {
    }

    public static RiskClass classify(Class<?> type) {
        AiActRiskClass explicit = type.getAnnotation(AiActRiskClass.class);
        if (explicit != null) {
            return explicit.value();
        }
        if (type.getAnnotation(AiActHighRiskSystem.class) != null) {
            return RiskClass.HIGH_RISK;
        }
        if (type.getAnnotation(AiActTransparency.class) != null) {
            return RiskClass.LIMITED;
        }
        return RiskClass.MINIMAL;
    }
}
