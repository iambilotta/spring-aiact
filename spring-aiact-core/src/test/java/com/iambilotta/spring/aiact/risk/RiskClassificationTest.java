/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.risk;

import com.iambilotta.spring.aiact.annotation.AiActHighRiskSystem;
import com.iambilotta.spring.aiact.annotation.AiActRiskClass;
import com.iambilotta.spring.aiact.annotation.AnnexIIICategory;
import com.iambilotta.spring.aiact.annotation.RiskClass;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RiskClassificationTest {

    @AiActRiskClass(RiskClass.PROHIBITED)
    static class SocialScorer {
    }

    @AiActRiskClass(RiskClass.LIMITED)
    static class Chatbot {
    }

    @AiActHighRiskSystem(name = "CV screener",
            category = AnnexIIICategory.EMPLOYMENT_AND_WORKERS_MANAGEMENT,
            intendedPurpose = "rank candidates", provider = "Acme")
    static class CvScreener {
    }

    static class PlainBean {
    }

    @Test
    void classifiesAnExplicitlyDeclaredBand() {
        assertThat(RiskClassifier.classify(SocialScorer.class)).isEqualTo(RiskClass.PROHIBITED);
        assertThat(RiskClassifier.classify(Chatbot.class)).isEqualTo(RiskClass.LIMITED);
    }

    @Test
    void inferssHighRiskFromTheHighRiskSystemAnnotation() {
        // A class declaring @AiActHighRiskSystem is high-risk without repeating @AiActRiskClass.
        assertThat(RiskClassifier.classify(CvScreener.class)).isEqualTo(RiskClass.HIGH_RISK);
    }

    @Test
    void defaultsToMinimalWhenNothingIsDeclared() {
        assertThat(RiskClassifier.classify(PlainBean.class)).isEqualTo(RiskClass.MINIMAL);
    }

    @Test
    void prohibitedIsTheOnlyBandRefusedByConstruction() {
        assertThat(RiskClass.PROHIBITED.isRefused()).isTrue();
        assertThat(RiskClass.HIGH_RISK.isRefused()).isFalse();
        assertThat(RiskClass.LIMITED.isRefused()).isFalse();
        assertThat(RiskClass.MINIMAL.isRefused()).isFalse();
        assertThat(RiskClass.GPAI.isRefused()).isFalse();
    }
}
