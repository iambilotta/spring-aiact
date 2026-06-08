/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.codegen.fria;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Design-only placeholder for the Article 27 Fundamental Rights Impact Assessment scaffold
 * (REQ-AIACT-013, priority W).
 *
 * <p>Left disabled deliberately, not implemented this pass. The shape, once built, mirrors the
 * existing dossier generators: a {@code FriaScaffoldRenderer} in {@code codegen} that walks the
 * declared annotation model (system id, name, Annex III category, intended purpose, datasets,
 * oversight) and emits a {@code fria.md} with the Article 27 question headings and explicit gap
 * placeholders for the deployer-only sections (affected groups, foreseeable risks to fundamental
 * rights, mitigation, human-oversight measures). It is a sibling of the DPIA scaffold in
 * spring-gdpr.
 *
 * <p>Why design-only: REQ-AIACT-013 is priority W (won't this time): the FRIA is a deployer-side
 * obligation that overlaps the organisational process more than the provider's code, and there is
 * no adopter demand yet. Promotion to a real test happens when an adopter asks for it.
 */
class FriaScaffoldDesignTest {

    @Test
    @Disabled("pending: REQ-AIACT-013 FRIA scaffold is design-only this pass (priority W, no adopter demand)")
    void generatesAFriaScaffoldFromTheAnnotationModel() {
        // Intentionally empty. See the class javadoc for the intended FriaScaffoldRenderer shape.
    }
}
