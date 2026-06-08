/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.mavenplugin;

import com.iambilotta.spring.aiact.annotation.RiskClass;
import com.iambilotta.spring.aiact.risk.RiskClassifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Build-time gate that refuses, by construction, any AI system classified as a prohibited
 * Article 5 practice (REQ-AIACT-012). Pure-Java logic decoupled from the Maven mojo so it can be
 * unit tested without a Maven project, mirroring {@link HighRiskAnnotationValidator}.
 *
 * <p>Returns one violation string per class whose resolved {@link RiskClass#isRefused()} is true.
 */
public final class RiskClassificationValidator {

    public List<String> validate(Iterable<Class<?>> classes) {
        List<String> violations = new ArrayList<>();
        for (Class<?> type : classes) {
            RiskClass band = RiskClassifier.classify(type);
            if (band.isRefused()) {
                violations.add(type.getName() + ": classified " + band
                        + " (Article 5 prohibited practice). The build refuses a prohibited AI "
                        + "system by construction; remove the practice or correct the classification.");
            }
        }
        return violations;
    }
}
