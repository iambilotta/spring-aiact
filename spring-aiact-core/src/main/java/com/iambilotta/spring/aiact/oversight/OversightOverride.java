/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.oversight;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

/**
 * Article 14 override request payload posted by the human supervisor to the oversight endpoint.
 *
 * @param actor          natural person performing the override (badge id, employee id, role-tag).
 * @param decision       one of {@code accept}, {@code reject}, {@code stop}, {@code flag-anomaly}.
 * @param reason         free-text justification, archived in the audit log.
 * @param systemId       system identifier the linked event belongs to.
 * @param linkedEventId  the original event the override refers to. Optional for {@code stop}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OversightOverride(
        String actor,
        String decision,
        String reason,
        String systemId,
        UUID linkedEventId
) { }
