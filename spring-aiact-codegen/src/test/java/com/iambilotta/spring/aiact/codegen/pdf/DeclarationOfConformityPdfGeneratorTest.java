/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.codegen.pdf;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeclarationOfConformityPdfGeneratorTest {

    @Test
    void rendersValidPdfHeader() {
        DeclarationOfConformity doc = new DeclarationOfConformity(
                "Demo system", "demo", "ACME", "Via Roma 1, Parma",
                "0.0.1", "Annex III.4",
                List.of("ISO/IEC 42001"),
                "",
                "Anna Rossi, Compliance Officer",
                "Parma",
                Instant.parse("2026-04-29T00:00:00Z"),
                List.of()
        );
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new DeclarationOfConformityPdfGenerator().render(doc, baos);
        byte[] pdf = baos.toByteArray();
        assertThat(pdf.length).isGreaterThan(500);
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }
}
