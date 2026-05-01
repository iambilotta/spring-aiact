/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.annotation;

/**
 * Article 15 accuracy and robustness metric kinds. The values are deliberately a small set
 * aligned with the AI Act recitals on accuracy, robustness and cybersecurity. Custom metrics
 * may be declared via {@link AiActAccuracyMetric#name()} together with {@link #CUSTOM}.
 */
public enum RiskMetric {
    ACCURACY,
    PRECISION,
    RECALL,
    F1,
    AUC_ROC,
    FALSE_POSITIVE_RATE,
    FALSE_NEGATIVE_RATE,
    ROBUSTNESS_ADVERSARIAL,
    BIAS_DEMOGRAPHIC_PARITY,
    BIAS_EQUAL_OPPORTUNITY,
    CALIBRATION_BRIER_SCORE,
    CUSTOM
}
