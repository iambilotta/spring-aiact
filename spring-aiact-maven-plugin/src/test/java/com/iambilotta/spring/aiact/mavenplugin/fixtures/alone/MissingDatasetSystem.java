/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.mavenplugin.fixtures.alone;

import com.iambilotta.spring.aiact.annotation.AiActHighRiskSystem;
import com.iambilotta.spring.aiact.annotation.AiActIntendedPurpose;
import com.iambilotta.spring.aiact.annotation.AiActOversight;
import com.iambilotta.spring.aiact.annotation.AnnexIIICategory;
import com.iambilotta.spring.aiact.annotation.OversightLevel;

@AiActHighRiskSystem(
        id = "missing-dataset",
        name = "Missing dataset fixture",
        category = AnnexIIICategory.EMPLOYMENT_AND_WORKERS_MANAGEMENT,
        intendedPurpose = "test",
        provider = "test"
)
@AiActIntendedPurpose(deploymentContext = "test", users = {"test"})
@AiActOversight(level = OversightLevel.HUMAN_IN_THE_LOOP)
public class MissingDatasetSystem {
}
