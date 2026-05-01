/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.codegen.model;

import com.iambilotta.spring.aiact.annotation.AnnexIIICategory;
import com.iambilotta.spring.aiact.annotation.OversightLevel;
import com.iambilotta.spring.aiact.annotation.RiskMetric;

import java.time.Instant;
import java.util.List;

/**
 * In-memory representation of a single high-risk AI system, populated by either the build-time
 * Maven plugin (scanning the compiled classes) or the runtime collector (scanning the Spring
 * context). Once built, the model is rendered to the Annex IV Markdown technical file, the
 * Article 47 Declaration of Conformity PDF and the per-dataset datasheets.
 * <p>
 * The model is intentionally a transparent record-of-records. It must not contain runtime
 * objects, lambdas, or anything that resists serialization: this is a build artifact.
 */
public record TechnicalFileModel(
        String systemId,
        String systemName,
        AnnexIIICategory category,
        String annexSubpoint,
        String intendedPurpose,
        String provider,
        String version,
        String gitSha,
        Instant generatedAt,
        IntendedPurposeDetails intendedPurposeDetails,
        OversightDescriptor oversight,
        List<DatasetEntry> datasets,
        List<AccuracyMetricEntry> accuracyMetrics,
        List<LoggedOperation> loggedOperations,
        List<HarmonizedStandard> harmonizedStandards,
        List<LifecycleChange> lifecycleChanges,
        List<String> cybersecurityMeasures,
        List<String> validationStrategies,
        List<String> trainingMethodologies,
        String architectureDescription
) {

    public record IntendedPurposeDetails(
            String deploymentContext,
            List<String> users,
            List<String> foreseeableMisuse,
            List<String> geographies,
            List<String> languages
    ) { }

    public record OversightDescriptor(
            OversightLevel level,
            String description,
            String overrideRole
    ) { }

    public record DatasetEntry(
            String id,
            String name,
            String phase,
            String source,
            String size,
            String license,
            List<String> biases,
            boolean personalData
    ) { }

    public record AccuracyMetricEntry(
            RiskMetric metric,
            String name,
            String threshold,
            String harness,
            String population
    ) { }

    public record LoggedOperation(
            String declaringClass,
            String method,
            boolean captureInput,
            boolean captureOutput,
            String hashAlgorithm,
            String operation,
            String modelId
    ) { }

    public record HarmonizedStandard(String reference, String title) { }

    public record LifecycleChange(Instant when, String version, String summary) { }
}
