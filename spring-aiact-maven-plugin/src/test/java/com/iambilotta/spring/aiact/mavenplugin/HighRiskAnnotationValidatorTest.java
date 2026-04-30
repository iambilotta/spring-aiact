/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.mavenplugin;

import com.iambilotta.spring.aiact.mavenplugin.fixtures.CompliantSystem;
import com.iambilotta.spring.aiact.mavenplugin.fixtures.MissingPurposeSystem;
import com.iambilotta.spring.aiact.mavenplugin.fixtures.alone.MissingDatasetSystem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HighRiskAnnotationValidatorTest {

    @Test
    void compliantClassPassesValidation() {
        List<String> v = new HighRiskAnnotationValidator(false)
                .validate(List.of(CompliantSystem.class));
        assertThat(v).isEmpty();
    }

    @Test
    void missingPurposeIsReported() {
        List<String> v = new HighRiskAnnotationValidator(false)
                .validate(List.of(MissingPurposeSystem.class));
        assertThat(v).hasSize(1);
        assertThat(v.get(0)).contains("@AiActIntendedPurpose").contains("Article 13");
    }

    @Test
    void missingDatasetIsReportedEvenWhenAnotherSystemHasOneInADifferentPackage() {
        List<String> v = new HighRiskAnnotationValidator(false)
                .validate(List.of(MissingDatasetSystem.class, CompliantSystem.class));
        // The previous bug returned empty here because *any* dataset on the classpath
        // satisfied *every* system. The fix scopes the lookup to the same package.
        assertThat(v).anyMatch(line -> line.contains("@AiActDataset")
                && line.contains("missing-dataset"));
    }

    @Test
    void datasetOptionalDisablesTheCheck() {
        List<String> v = new HighRiskAnnotationValidator(true)
                .validate(List.of(MissingDatasetSystem.class));
        assertThat(v).isEmpty();
    }

    @Test
    void nonHighRiskClassesAreIgnored() {
        List<String> v = new HighRiskAnnotationValidator(false)
                .validate(List.of(String.class, Integer.class));
        assertThat(v).isEmpty();
    }
}
