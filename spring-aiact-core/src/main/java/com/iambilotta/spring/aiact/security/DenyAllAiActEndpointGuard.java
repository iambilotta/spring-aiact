/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Safe default {@link AiActEndpointGuard}: refuses every request. Registered by the starter when
 * the deployer has not provided their own bean. Logs the refusal once per process at the first
 * call so noisy probes do not flood the log, but every refusal still surfaces a 403.
 * <p>
 * To replace, declare a bean of type {@link AiActEndpointGuard} in your application
 * configuration. See {@code docs/PRODUCTION.md} for a Spring Security wiring example.
 */
public class DenyAllAiActEndpointGuard implements AiActEndpointGuard {

    private static final Logger log = LoggerFactory.getLogger(DenyAllAiActEndpointGuard.class);

    private volatile boolean warned = false;

    @Override
    public Decision authorize(String systemId, Action action) {
        if (!warned) {
            warned = true;
            log.warn("spring-aiact: no AiActEndpointGuard bean is configured; the default "
                    + "DenyAllAiActEndpointGuard is refusing every /aiact/** call. "
                    + "Wire your auth stack into a custom AiActEndpointGuard bean (see "
                    + "docs/PRODUCTION.md). Set aiact.endpoints.allow-without-guard=true "
                    + "to silence this with the unsafe permit-all guard during local dev.");
        }
        return Decision.deny("no-guard-configured");
    }
}
