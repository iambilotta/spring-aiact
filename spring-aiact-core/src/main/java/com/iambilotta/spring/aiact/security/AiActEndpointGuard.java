/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.security;

/**
 * SPI implemented by deployers to authorize calls to the {@code /aiact/**} endpoints exposed by
 * the starter. The library does not pull in Spring Security as a dependency: instead, every
 * incoming request is funnelled through an implementation of this interface, and the deployer
 * wires it to whatever auth stack their application already uses (Spring Security, OPA, Vault,
 * a hand-rolled API key check, etc).
 * <p>
 * The default implementation registered by the starter is {@link DenyAllAiActEndpointGuard},
 * which refuses every request and logs a warning at boot. This is deliberate: an unauthenticated
 * audit endpoint defeats the entire compliance promise. Deployers must opt in explicitly to
 * each endpoint exposure.
 */
public interface AiActEndpointGuard {

    /** Action being attempted by the caller. */
    enum Action {
        /** Read the audit log slice for a system. */
        EXPORT_LOG,
        /** Read the chain verification report for a system. */
        VERIFY_LOG,
        /** Read the chain head HMAC for a system. */
        READ_HEAD,
        /** Submit an Article 14 oversight override. */
        SUBMIT_OVERRIDE
    }

    /**
     * Authorize the action. Implementations should derive the caller identity from whatever
     * thread-local context their auth framework populates (SecurityContextHolder for Spring
     * Security, RequestAttributes for raw servlets, ...).
     *
     * @param systemId the AI system id the action targets, never null.
     * @param action   the action being attempted, never null.
     * @return a {@link Decision} carrying allow/deny and an optional reason string surfaced to
     *         the client and the audit log.
     */
    Decision authorize(String systemId, Action action);

    record Decision(boolean allowed, String reason) {
        public static Decision allow() {
            return new Decision(true, "allowed");
        }
        public static Decision allow(String reason) {
            return new Decision(true, reason);
        }
        public static Decision deny(String reason) {
            return new Decision(false, reason);
        }
    }
}
