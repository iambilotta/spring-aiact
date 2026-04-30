/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.audit;

/**
 * Coordinates of the high-risk AI system that owns an annotated method, resolved by inspecting
 * the {@code @AiActHighRiskSystem} annotation on the target class.
 */
public record SystemContext(String systemId, String systemName, String version, String provider) {
}
