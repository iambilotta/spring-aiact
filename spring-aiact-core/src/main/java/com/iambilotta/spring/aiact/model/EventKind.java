/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.model;

/**
 * Type of audit event recorded in the Article 12 log.
 */
public enum EventKind {
    /** A normal AI Act high-risk system invocation. */
    INVOCATION,
    /** An Article 14 human oversight override. */
    OVERRIDE,
    /** An anomaly flagged by the deployer (model drift, low confidence, etc). */
    ANOMALY,
    /** A stop event raised by the human supervisor. */
    STOP
}
