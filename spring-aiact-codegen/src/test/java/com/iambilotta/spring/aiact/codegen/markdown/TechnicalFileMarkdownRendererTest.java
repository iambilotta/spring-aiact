/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.codegen.markdown;

import com.iambilotta.spring.aiact.annotation.AnnexIIICategory;
import com.iambilotta.spring.aiact.annotation.OversightLevel;
import com.iambilotta.spring.aiact.annotation.RiskMetric;
import com.iambilotta.spring.aiact.codegen.model.TechnicalFileModel;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TechnicalFileMarkdownRendererTest {

    @Test
    void rendersAllNineSectionsWithGapPlaceholdersWhenEmpty() {
        TechnicalFileModel m = new TechnicalFileModel(
                "demo", "Demo system", AnnexIIICategory.EMPLOYMENT_AND_WORKERS_MANAGEMENT,
                "4(a)", "Score CV applicants", "ACME", "0.0.1", "deadbeef",
                Instant.parse("2026-04-29T10:00:00Z"),
                null, null,
                List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(), ""
        );
        String md = new TechnicalFileMarkdownRenderer().render(m);
        for (String heading : List.of(
                "# Technical File, AI Act Annex IV",
                "## 1. General description",
                "## 2. Design and development",
                "## 3. Datasets and data governance",
                "## 4. Training methodology",
                "## 5. Validation and testing",
                "## 6. Accuracy, robustness and Article 15 metrics",
                "## 7. Cybersecurity",
                "## 8. Lifecycle changes and post-market monitoring",
                "## 9. Harmonized standards and references"
        )) {
            assertThat(md).contains(heading);
        }
        assertThat(md).contains("Annex III.4");
        assertThat(md).contains("Not declared in code");
    }

    @Test
    void rendersTablesWhenSectionsArePopulated() {
        TechnicalFileModel m = new TechnicalFileModel(
                "demo", "Demo system", AnnexIIICategory.EMPLOYMENT_AND_WORKERS_MANAGEMENT,
                "", "Score CV applicants", "ACME", "0.0.1", "abc",
                Instant.parse("2026-04-29T10:00:00Z"),
                new TechnicalFileModel.IntendedPurposeDetails(
                        "Recruitment", List.of("HR specialists"), List.of("blanket auto-rejection"),
                        List.of("EU"), List.of("it", "en")),
                new TechnicalFileModel.OversightDescriptor(
                        OversightLevel.HUMAN_IN_THE_LOOP, "HR reviews every output", "hr"),
                List.of(new TechnicalFileModel.DatasetEntry(
                        "cv-2025", "Anonymized CV corpus", "training", "internal-s3://cv",
                        "12,500 records", "internal", List.of("under-representation of women in STEM"),
                        true)),
                List.of(new TechnicalFileModel.AccuracyMetricEntry(
                        RiskMetric.PRECISION, "", ">=0.92", "src/test/.../HiringMetricTest.java", "all")),
                List.of(new TechnicalFileModel.LoggedOperation(
                        "HiringScreener", "score", true, true, "SHA-256",
                        "HiringScreener.score", "demo@0.0.1")),
                List.of(new TechnicalFileModel.HarmonizedStandard(
                        "ISO/IEC 42001", "AI management system")),
                List.of(),
                List.of("Inputs validated against schema",
                        "All artifacts signed with the build HMAC chain"),
                List.of("k-fold 5"),
                List.of("Supervised learning, gradient-boosted trees"),
                "Spring Boot service exposing one REST endpoint, calling a model server."
        );
        String md = new TechnicalFileMarkdownRenderer().render(m);
        assertThat(md).contains("| `cv-2025` | Anonymized CV corpus | training |");
        assertThat(md).contains("| PRECISION |");
        assertThat(md).contains("HUMAN_IN_THE_LOOP");
        assertThat(md).contains("ISO/IEC 42001");
    }
}
