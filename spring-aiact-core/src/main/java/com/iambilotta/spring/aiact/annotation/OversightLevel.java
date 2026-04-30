/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.annotation;

/**
 * Article 14 human oversight modes.
 */
public enum OversightLevel {

    /** Human-in-the-loop. The decision is taken by a human, the AI system advises. */
    HUMAN_IN_THE_LOOP,

    /** Human-on-the-loop. The AI system decides, a human supervises and may override. */
    HUMAN_ON_THE_LOOP,

    /** Human-out-of-the-loop, with retrospective auditability only. Use sparingly. */
    HUMAN_OUT_OF_THE_LOOP_AUDITED
}
