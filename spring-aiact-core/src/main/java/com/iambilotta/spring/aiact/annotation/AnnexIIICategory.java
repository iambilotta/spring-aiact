/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.annotation;

/**
 * Official high-risk AI system categories from EU AI Act Annex III.
 * <p>
 * Source: Regulation (EU) 2024/1689, Annex III (point 1 to 8). Do not extend with custom values
 * unless the Annex III is officially amended. Custom categories are explicitly out of scope of
 * this starter (anti-pattern declared in the spring-aiact specification).
 */
public enum AnnexIIICategory {

    /** Annex III, point 1. Biometrics. */
    BIOMETRICS("Annex III.1"),

    /** Annex III, point 2. Critical infrastructure. */
    CRITICAL_INFRASTRUCTURE("Annex III.2"),

    /** Annex III, point 3. Education and vocational training. */
    EDUCATION_AND_VOCATIONAL_TRAINING("Annex III.3"),

    /** Annex III, point 4. Employment, workers management and access to self-employment. */
    EMPLOYMENT_AND_WORKERS_MANAGEMENT("Annex III.4"),

    /** Annex III, point 5. Access to and enjoyment of essential private and public services. */
    ESSENTIAL_PRIVATE_AND_PUBLIC_SERVICES("Annex III.5"),

    /** Annex III, point 6. Law enforcement. */
    LAW_ENFORCEMENT("Annex III.6"),

    /** Annex III, point 7. Migration, asylum and border control management. */
    MIGRATION_ASYLUM_BORDER_CONTROL("Annex III.7"),

    /** Annex III, point 8. Administration of justice and democratic processes. */
    JUSTICE_AND_DEMOCRATIC_PROCESSES("Annex III.8");

    private final String reference;

    AnnexIIICategory(String reference) {
        this.reference = reference;
    }

    public String reference() {
        return reference;
    }
}
