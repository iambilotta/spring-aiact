/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.autoconfigure.web;

import com.iambilotta.spring.aiact.model.AuditEvent;
import com.iambilotta.spring.aiact.oversight.OversightOverride;
import com.iambilotta.spring.aiact.oversight.OversightService;
import com.iambilotta.spring.aiact.security.AiActEndpointGuard;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Article 14 oversight endpoint. Posting to {@code /aiact/oversight/{eventId}/override} appends a
 * second event to the audit log linked to the original event id, recording the actor and the
 * decision. The body must contain the {@link OversightOverride} payload.
 */
@RestController
@RequestMapping("${aiact.endpoints.base-path:/aiact}/oversight")
public class AiActOversightController {

    private final OversightService oversightService;
    private final AiActEndpointGuard guard;

    public AiActOversightController(OversightService oversightService,
                                    AiActEndpointGuard guard) {
        this.oversightService = oversightService;
        this.guard = guard;
    }

    @PostMapping(value = "/{eventId}/override",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public AuditEvent override(@PathVariable("eventId") UUID eventId,
                               @RequestBody OversightOverride body) {
        AiActEndpointGuard.Decision decision = guard.authorize(
                body.systemId(), AiActEndpointGuard.Action.SUBMIT_OVERRIDE);
        if (!decision.allowed()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, decision.reason());
        }
        OversightOverride normalized = new OversightOverride(
                body.actor(), body.decision(), body.reason(), body.systemId(),
                body.linkedEventId() != null ? body.linkedEventId() : eventId);
        return oversightService.recordOverride(normalized);
    }
}
