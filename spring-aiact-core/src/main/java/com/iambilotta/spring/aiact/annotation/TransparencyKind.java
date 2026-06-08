/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.annotation;

/**
 * Article 50 transparency obligations the disclosure satisfies.
 * <p>
 * Source: Regulation (EU) 2024/1689, Article 50. The two values map the two obligations the
 * library can stamp from code: a natural person interacting with an AI system (Art.50(1)) and
 * AI-generated or manipulated content (Art.50(2)/(4)).
 */
public enum TransparencyKind {

    /** Article 50(1). The person is interacting with an AI system (for example a chatbot). */
    AI_INTERACTION("ai-interaction"),

    /** Article 50(2)/(4). The output content is AI-generated or AI-manipulated. */
    AI_GENERATED_CONTENT("ai-generated-content");

    private final String token;

    TransparencyKind(String token) {
        this.token = token;
    }

    /** Machine-detectable token used in the disclosure header value. */
    public String token() {
        return token;
    }
}
