/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Permit-all guard for local development. Activated only when
 * {@code aiact.endpoints.allow-without-guard=true} is set explicitly in configuration. Logs a
 * loud warning at boot so the choice is visible in the application output.
 * <p>
 * Never configure this in production. The audit endpoints would expose the full Article 12 log
 * to anyone who can reach the application port.
 */
public class AllowAllAiActEndpointGuard implements AiActEndpointGuard {

    private static final Logger log = LoggerFactory.getLogger(AllowAllAiActEndpointGuard.class);

    public AllowAllAiActEndpointGuard() {
        log.warn("spring-aiact: aiact.endpoints.allow-without-guard=true is active. "
                + "The /aiact/** endpoints are open to anyone who can reach the application "
                + "port. Use this only in local development; never in production.");
    }

    @Override
    public Decision authorize(String systemId, Action action) {
        return Decision.allow("allow-without-guard");
    }
}
