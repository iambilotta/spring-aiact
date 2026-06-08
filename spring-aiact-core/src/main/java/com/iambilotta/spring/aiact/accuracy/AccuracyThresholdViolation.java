/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.accuracy;

/**
 * Raised when a measured Article 15 accuracy/robustness metric falls below the declared threshold
 * (REQ-AIACT-006). A build step or evaluation hook lets this propagate to fail the gate; a runtime
 * monitor may catch it to raise an anomaly audit event instead.
 */
public class AccuracyThresholdViolation extends RuntimeException {

    public AccuracyThresholdViolation(String message) {
        super(message);
    }
}
