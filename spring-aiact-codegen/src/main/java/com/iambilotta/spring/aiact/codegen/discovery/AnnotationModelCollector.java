/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.codegen.discovery;

import com.iambilotta.spring.aiact.annotation.AiActAccuracyMetric;
import com.iambilotta.spring.aiact.annotation.AiActDataset;
import com.iambilotta.spring.aiact.annotation.AiActHighRiskSystem;
import com.iambilotta.spring.aiact.annotation.AiActIntendedPurpose;
import com.iambilotta.spring.aiact.annotation.AiActLog;
import com.iambilotta.spring.aiact.annotation.AiActOversight;
import com.iambilotta.spring.aiact.codegen.model.TechnicalFileModel;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Collects a {@link TechnicalFileModel} per {@code @AiActHighRiskSystem} class. The collector is
 * deliberately reflection-only and does not depend on Spring or on Maven, so the same code path
 * is used by the runtime starter and by the build-time Maven plugin.
 */
public final class AnnotationModelCollector {

    public List<TechnicalFileModel> collect(Collection<Class<?>> candidates,
                                            CollectionContext ctx) {
        if (candidates == null || candidates.isEmpty()) return List.of();
        Map<String, TechnicalFileModel> byId = new LinkedHashMap<>();
        for (Class<?> type : candidates) {
            AiActHighRiskSystem hr = type.getAnnotation(AiActHighRiskSystem.class);
            if (hr == null) continue;
            String id = hr.id().isEmpty() ? type.getSimpleName() : hr.id();
            byId.computeIfAbsent(id, key -> buildBase(type, hr, ctx));
        }
        for (Class<?> type : candidates) {
            mergeContributions(type, byId);
        }
        return new ArrayList<>(byId.values());
    }

    private TechnicalFileModel buildBase(Class<?> type, AiActHighRiskSystem hr, CollectionContext ctx) {
        AiActIntendedPurpose ip = type.getAnnotation(AiActIntendedPurpose.class);
        AiActOversight oversight = type.getAnnotation(AiActOversight.class);

        TechnicalFileModel.IntendedPurposeDetails ipd = ip == null ? null
                : new TechnicalFileModel.IntendedPurposeDetails(
                        ip.deploymentContext(),
                        Arrays.asList(ip.users()),
                        Arrays.asList(ip.foreseeableMisuse()),
                        Arrays.asList(ip.geographies()),
                        Arrays.asList(ip.languages()));

        TechnicalFileModel.OversightDescriptor ovd = oversight == null ? null
                : new TechnicalFileModel.OversightDescriptor(
                        oversight.level(),
                        oversight.description(),
                        oversight.overrideRole());

        String id = hr.id().isEmpty() ? type.getSimpleName() : hr.id();
        String version = hr.version().isEmpty() ? ctx.version() : hr.version();

        List<TechnicalFileModel.DatasetEntry> datasets = new ArrayList<>();
        List<TechnicalFileModel.AccuracyMetricEntry> metrics = new ArrayList<>();
        List<TechnicalFileModel.LoggedOperation> ops = new ArrayList<>();
        contribute(type, datasets, metrics, ops);

        return new TechnicalFileModel(
                id,
                hr.name(),
                hr.category(),
                hr.annexSubpoint(),
                hr.intendedPurpose(),
                hr.provider(),
                version,
                ctx.gitSha(),
                ctx.generatedAt() == null ? Instant.now() : ctx.generatedAt(),
                ipd,
                ovd,
                datasets,
                metrics,
                ops,
                ctx.harmonizedStandards(),
                ctx.lifecycleChanges(),
                ctx.cybersecurityMeasures(),
                ctx.validationStrategies(),
                ctx.trainingMethodologies(),
                ctx.architectureDescription()
        );
    }

    private void mergeContributions(Class<?> type, Map<String, TechnicalFileModel> byId) {
        AiActHighRiskSystem hr = type.getAnnotation(AiActHighRiskSystem.class);
        if (hr != null) return;
        AiActDataset[] datasets = type.getAnnotationsByType(AiActDataset.class);
        if (datasets.length == 0) return;
        // Datasets declared on a non-system class default to the first system in the file.
        if (byId.isEmpty()) return;
        TechnicalFileModel target = byId.values().iterator().next();
        List<TechnicalFileModel.DatasetEntry> entries = new ArrayList<>(target.datasets());
        for (AiActDataset d : datasets) entries.add(toDatasetEntry(d));
        replace(byId, target, entries, target.accuracyMetrics(), target.loggedOperations());
    }

    private void contribute(Class<?> type,
                            List<TechnicalFileModel.DatasetEntry> datasets,
                            List<TechnicalFileModel.AccuracyMetricEntry> metrics,
                            List<TechnicalFileModel.LoggedOperation> ops) {
        for (AiActDataset d : type.getAnnotationsByType(AiActDataset.class)) {
            datasets.add(toDatasetEntry(d));
        }
        for (AiActAccuracyMetric m : type.getAnnotationsByType(AiActAccuracyMetric.class)) {
            metrics.add(toMetricEntry(m));
        }
        AiActLog typeLog = type.getAnnotation(AiActLog.class);
        for (Method method : type.getDeclaredMethods()) {
            AiActLog ann = method.getAnnotation(AiActLog.class);
            if (ann == null) ann = typeLog;
            if (ann == null) continue;
            ops.add(new TechnicalFileModel.LoggedOperation(
                    type.getSimpleName(),
                    method.getName(),
                    ann.captureInput(),
                    ann.captureOutput(),
                    ann.hashStrategy().algorithm(),
                    ann.operation().isEmpty() ? type.getSimpleName() + "." + method.getName() : ann.operation(),
                    ann.modelId().isEmpty() ? "_inherited_" : ann.modelId()
            ));
        }
        for (Method method : type.getDeclaredMethods()) {
            for (AiActDataset d : method.getAnnotationsByType(AiActDataset.class)) {
                datasets.add(toDatasetEntry(d));
            }
            for (AiActAccuracyMetric m : method.getAnnotationsByType(AiActAccuracyMetric.class)) {
                metrics.add(toMetricEntry(m));
            }
        }
    }

    private TechnicalFileModel.DatasetEntry toDatasetEntry(AiActDataset d) {
        return new TechnicalFileModel.DatasetEntry(
                d.id(), d.name(), d.phase(), d.source(), d.size(), d.license(),
                Arrays.asList(d.biases()), d.personalData());
    }

    private TechnicalFileModel.AccuracyMetricEntry toMetricEntry(AiActAccuracyMetric m) {
        return new TechnicalFileModel.AccuracyMetricEntry(
                m.metric(), m.name(), m.threshold(), m.harness(), m.population());
    }

    private void replace(Map<String, TechnicalFileModel> byId,
                         TechnicalFileModel old,
                         List<TechnicalFileModel.DatasetEntry> datasets,
                         List<TechnicalFileModel.AccuracyMetricEntry> metrics,
                         List<TechnicalFileModel.LoggedOperation> ops) {
        TechnicalFileModel updated = new TechnicalFileModel(
                old.systemId(), old.systemName(), old.category(), old.annexSubpoint(),
                old.intendedPurpose(), old.provider(), old.version(), old.gitSha(),
                old.generatedAt(), old.intendedPurposeDetails(), old.oversight(),
                datasets, metrics, ops,
                old.harmonizedStandards(), old.lifecycleChanges(), old.cybersecurityMeasures(),
                old.validationStrategies(), old.trainingMethodologies(),
                old.architectureDescription());
        byId.put(updated.systemId(), updated);
    }

    /**
     * Build-time inputs that are not encoded in annotations but populated by the caller.
     */
    public record CollectionContext(
            String version,
            String gitSha,
            Instant generatedAt,
            String architectureDescription,
            List<String> trainingMethodologies,
            List<String> validationStrategies,
            List<String> cybersecurityMeasures,
            List<TechnicalFileModel.LifecycleChange> lifecycleChanges,
            List<TechnicalFileModel.HarmonizedStandard> harmonizedStandards
    ) {
        public static CollectionContext minimal() {
            return new CollectionContext(
                    "unknown", "", Instant.now(),
                    "", List.of(), List.of(), List.of(), List.of(), List.of()
            );
        }
    }
}
