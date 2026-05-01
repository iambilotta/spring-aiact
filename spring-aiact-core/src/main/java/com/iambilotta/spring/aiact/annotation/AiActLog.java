/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a logged AI Act operation. Each invocation produces one Article 12
 * audit record, written by the AOP advisor to the configured append-only NDJSON sink and
 * chained with HMAC.
 * <p>
 * The annotation never causes the method to capture raw payloads: only the hash of the input
 * and output payloads is recorded, together with the model id, the latency and the database
 * reference (if any). This is by design, to keep the audit log free of personal data and
 * proprietary content while still being verifiable.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface AiActLog {

    /**
     * If true (default), the input arguments are hashed and the hash is recorded.
     */
    boolean captureInput() default true;

    /**
     * If true (default), the return value is hashed and the hash is recorded.
     */
    boolean captureOutput() default true;

    /**
     * Hashing algorithm. Defaults to {@link HashStrategy#SHA_256}.
     */
    HashStrategy hashStrategy() default HashStrategy.SHA_256;

    /**
     * Optional model identifier override. When empty, the advisor falls back to the value
     * declared on the surrounding {@link AiActHighRiskSystem} or to {@code unknown}.
     */
    String modelId() default "";

    /**
     * Optional logical operation name. Defaults to {@code class.method}.
     */
    String operation() default "";
}
