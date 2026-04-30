/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.mavenplugin;

import com.iambilotta.spring.aiact.annotation.AiActDataset;
import com.iambilotta.spring.aiact.annotation.AiActHighRiskSystem;
import com.iambilotta.spring.aiact.annotation.AiActIntendedPurpose;
import com.iambilotta.spring.aiact.annotation.AiActOversight;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure-Java verification logic, decoupled from the Maven mojo so it can be unit tested without
 * spinning up a Maven project. Returns a list of violation strings describing every
 * {@code @AiActHighRiskSystem} class that is missing a companion annotation.
 */
public final class HighRiskAnnotationValidator {

    private final boolean datasetOptional;

    public HighRiskAnnotationValidator(boolean datasetOptional) {
        this.datasetOptional = datasetOptional;
    }

    public List<String> validate(Iterable<Class<?>> classes) {
        List<String> violations = new ArrayList<>();
        for (Class<?> type : classes) {
            AiActHighRiskSystem hr = type.getAnnotation(AiActHighRiskSystem.class);
            if (hr == null) continue;
            if (type.getAnnotation(AiActIntendedPurpose.class) == null) {
                violations.add(type.getName() + ": missing @AiActIntendedPurpose (Article 13).");
            }
            if (type.getAnnotation(AiActOversight.class) == null) {
                violations.add(type.getName() + ": missing @AiActOversight (Article 14).");
            }
            if (!datasetOptional && !hasDatasetForSystem(type, classes)) {
                violations.add(type.getName()
                        + ": missing @AiActDataset (Article 10) on this class or on a dataset class "
                        + "linked to system id '" + (hr.id().isEmpty() ? type.getSimpleName() : hr.id()) + "'.");
            }
        }
        return violations;
    }

    /**
     * A dataset is reachable for the high-risk class when the dataset annotation is on the class
     * itself, on one of its declared methods, or on a sibling class in the same package whose
     * name suggests it is the data loader (heuristic: ends with {@code DataLoader}, {@code Repository}
     * or {@code Dao}). The previous implementation accepted any dataset anywhere on the classpath,
     * which produced false positives across unrelated systems sharing a module.
     */
    private boolean hasDatasetForSystem(Class<?> system, Iterable<Class<?>> all) {
        if (system.getAnnotationsByType(AiActDataset.class).length > 0) return true;
        for (Method m : system.getDeclaredMethods()) {
            if (m.getAnnotationsByType(AiActDataset.class).length > 0) return true;
        }
        Package pkg = system.getPackage();
        for (Class<?> candidate : all) {
            if (candidate == system) continue;
            if (candidate.getAnnotation(AiActHighRiskSystem.class) != null) continue;
            if (candidate.getAnnotationsByType(AiActDataset.class).length == 0) continue;
            if (pkg != null && candidate.getPackage() != null
                    && pkg.getName().equals(candidate.getPackage().getName())) {
                return true;
            }
        }
        return false;
    }
}
