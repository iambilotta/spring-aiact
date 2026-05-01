/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.codegen.datasheet;

import com.iambilotta.spring.aiact.codegen.model.TechnicalFileModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DatasetDatasheetRendererTest {

    @Test
    void rendersBiasesWhenDeclared() {
        TechnicalFileModel.DatasetEntry d = new TechnicalFileModel.DatasetEntry(
                "cv-2025", "CV corpus", "training", "internal-s3://cv",
                "12,500", "internal", List.of("under-representation of women in STEM"), true);
        String md = new DatasetDatasheetRenderer().render(d, "demo");
        assertThat(md).contains("# Dataset datasheet, `cv-2025`");
        assertThat(md).contains("Article 10 governance");
        assertThat(md).contains("under-representation of women in STEM");
        assertThat(md).contains("a separate Article 35 DPIA is required");
    }

    @Test
    void rendersGapWhenBiasesAreEmpty() {
        TechnicalFileModel.DatasetEntry d = new TechnicalFileModel.DatasetEntry(
                "x", "X", "test", "src", "100", "MIT", List.of(), false);
        String md = new DatasetDatasheetRenderer().render(d, "demo");
        assertThat(md).contains("_No biases formally measured");
        assertThat(md).contains("outside the scope of GDPR Article 35");
    }
}
