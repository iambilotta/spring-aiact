/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.codegen.pdf;

import java.time.Instant;
import java.util.List;

/**
 * Article 47 Declaration of Conformity payload. The structure follows the AI Act Annex V
 * (the document the provider must keep at the disposal of national authorities for ten years
 * after the AI system has been placed on the market or put into service).
 *
 * @param systemName              human-readable system name
 * @param systemId                stable system identifier
 * @param providerName            provider legal name
 * @param providerAddress         provider postal address
 * @param systemVersion           released version
 * @param annexIIIReference       Annex III category reference (for example {@code Annex III.4})
 * @param appliedStandards        list of harmonized standards applied
 * @param notifiedBody            optional notified body identification
 * @param signatureLine           full text of the signature line, populated by the signer
 * @param placeOfDeclaration      city
 * @param dateOfDeclaration       declaration date
 * @param additionalDeclarations  free additional clauses (for example representative in EU)
 */
public record DeclarationOfConformity(
        String systemName,
        String systemId,
        String providerName,
        String providerAddress,
        String systemVersion,
        String annexIIIReference,
        List<String> appliedStandards,
        String notifiedBody,
        String signatureLine,
        String placeOfDeclaration,
        Instant dateOfDeclaration,
        List<String> additionalDeclarations
) { }
