/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.annotation;

/**
 * The EU AI Act risk bands. The library has always modelled the {@link #HIGH_RISK} band in depth
 * (Annex III via {@code @AiActHighRiskSystem}); this enum adds the surrounding bands so a system
 * can be classified across the whole risk spectrum (REQ-AIACT-012).
 * <p>
 * Source: Regulation (EU) 2024/1689. Art.5 prohibited practices, Art.6 + Annex III high-risk,
 * Art.50 limited-risk transparency, Art.51-55 general-purpose AI models.
 */
public enum RiskClass {

    /** Article 5. A prohibited practice. Refused by construction: the build gate fails on it. */
    PROHIBITED(true),

    /** Article 6 + Annex III. High-risk system (the band the library tooling targets). */
    HIGH_RISK(false),

    /** Article 50. Limited-risk: only the transparency obligation applies. */
    LIMITED(false),

    /** Minimal or no risk: no obligation beyond voluntary codes of conduct. */
    MINIMAL(false),

    /** Articles 51-55. General-purpose AI model (orthogonal flag, modelled as a band here). */
    GPAI(false);

    private final boolean refused;

    RiskClass(boolean refused) {
        this.refused = refused;
    }

    /** Whether a system in this band must be refused by construction (only {@link #PROHIBITED}). */
    public boolean isRefused() {
        return refused;
    }
}
