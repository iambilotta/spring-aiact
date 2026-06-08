/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.mavenplugin;

import com.iambilotta.spring.aiact.annotation.AiActRiskClass;
import com.iambilotta.spring.aiact.annotation.RiskClass;
import com.iambilotta.spring.aiact.mavenplugin.fixtures.CompliantSystem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RiskClassificationValidatorTest {

    @AiActRiskClass(value = RiskClass.PROHIBITED, rationale = "social scoring")
    static class ProhibitedSystem {
    }

    @AiActRiskClass(RiskClass.LIMITED)
    static class LimitedSystem {
    }

    @Test
    void refusesAProhibitedArticle5Practice() {
        List<String> v = new RiskClassificationValidator()
                .validate(List.of(ProhibitedSystem.class));
        assertThat(v).hasSize(1);
        assertThat(v.get(0))
                .contains("PROHIBITED")
                .contains("Article 5")
                .contains("ProhibitedSystem");
    }

    @Test
    void allowsNonProhibitedBands() {
        List<String> v = new RiskClassificationValidator()
                .validate(List.of(LimitedSystem.class, CompliantSystem.class));
        assertThat(v).isEmpty();
    }
}
