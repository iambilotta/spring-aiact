/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.annotation;

/**
 * Hash algorithm used to fingerprint inputs and outputs of an AI Act high-risk system call,
 * before they are written to the Article 12 audit log. The audit log never contains
 * the raw payload: only the hash, the model id, and the metadata required by Article 12.
 * <p>
 * Use {@link #NONE} only when the payload is already an opaque identifier that
 * cannot be hashed in a meaningful way (rare). The default is {@link #SHA_256}.
 */
public enum HashStrategy {

    /** SHA-256, recommended default. */
    SHA_256("SHA-256"),

    /** SHA-512, when collision resistance margin is required. */
    SHA_512("SHA-512"),

    /** SHA3-256, when an SHA-3 family algorithm is mandated. */
    SHA3_256("SHA3-256"),

    /**
     * No hashing. The payload reference is recorded verbatim. Use only for opaque ids,
     * never for free text or PII.
     */
    NONE("NONE");

    private final String algorithm;

    HashStrategy(String algorithm) {
        this.algorithm = algorithm;
    }

    /**
     * @return the JCA algorithm name (or {@code NONE} for the no-op strategy).
     */
    public String algorithm() {
        return algorithm;
    }
}
