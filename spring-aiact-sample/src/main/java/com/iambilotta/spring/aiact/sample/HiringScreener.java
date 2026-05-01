/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.sample;

import com.iambilotta.spring.aiact.annotation.AiActAccuracyMetric;
import com.iambilotta.spring.aiact.annotation.AiActDataset;
import com.iambilotta.spring.aiact.annotation.AiActHighRiskSystem;
import com.iambilotta.spring.aiact.annotation.AiActIntendedPurpose;
import com.iambilotta.spring.aiact.annotation.AiActLog;
import com.iambilotta.spring.aiact.annotation.AiActOversight;
import com.iambilotta.spring.aiact.annotation.AnnexIIICategory;
import com.iambilotta.spring.aiact.annotation.OversightLevel;
import com.iambilotta.spring.aiact.annotation.RiskMetric;
import org.springframework.stereotype.Service;

/**
 * Demonstration high-risk AI system: a fake CV scoring engine. The annotation set is the entire
 * point of the sample, the scoring math is a deliberately trivial placeholder
 * ({@code length / 1000}). This class is not, and is not intended to be, a usable hiring
 * screener; do not deploy it. See the README for the supported real-world adoption path.
 */
@Service
@AiActHighRiskSystem(
        id = "hiring-screener",
        name = "Hiring screener (sample)",
        category = AnnexIIICategory.EMPLOYMENT_AND_WORKERS_MANAGEMENT,
        annexSubpoint = "4(a)",
        intendedPurpose = "Score CV applicants for a generic engineering role.",
        provider = "Sample Provider Ltd.",
        version = "0.0.1"
)
@AiActIntendedPurpose(
        deploymentContext = "Internal HR triage tool, before any human review.",
        users = {"HR specialists"},
        foreseeableMisuse = {
                "Auto-rejection without human review",
                "Use outside the engineering role context"
        },
        geographies = {"EU"},
        languages = {"it", "en"}
)
@AiActOversight(
        level = OversightLevel.HUMAN_IN_THE_LOOP,
        description = "Every output is reviewed by a HR specialist before action.",
        overrideRole = "hr"
)
@AiActDataset(
        id = "cv-corpus-2025",
        name = "Anonymized CV corpus 2025",
        phase = "training",
        source = "internal-s3://cv-2025",
        size = "12,500 records",
        license = "internal",
        biases = {"under-representation of women in STEM"},
        personalData = true
)
@AiActAccuracyMetric(metric = RiskMetric.PRECISION, threshold = ">=0.92",
        harness = "src/test/.../HiringScreenerMetricTest.java", population = "all")
@AiActAccuracyMetric(metric = RiskMetric.BIAS_DEMOGRAPHIC_PARITY, threshold = "<=0.05",
        harness = "src/test/.../HiringScreenerFairnessTest.java", population = "demographic.gender")
public class HiringScreener {

    @AiActLog(modelId = "hiring-screener@0.0.1")
    public ScoringResult score(CandidateApplication application) {
        if (application == null || application.cvText() == null) {
            return new ScoringResult(0.0, "missing-data");
        }
        double score = Math.min(1.0, application.cvText().length() / 1000.0);
        String label = score >= 0.5 ? "shortlist" : "reject";
        return new ScoringResult(score, label);
    }
}
