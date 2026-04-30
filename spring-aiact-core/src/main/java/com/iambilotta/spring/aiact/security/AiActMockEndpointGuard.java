/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.security;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test helper that lets a consumer write integration tests against the spring-aiact endpoints
 * without standing up their full auth stack. Configure allow/deny rules via the fluent builder
 * and register the bean only in the test profile.
 *
 * <pre>
 *   @TestConfiguration
 *   static class TestSecurityConfig {
 *       &#64;Bean
 *       AiActEndpointGuard guard() {
 *           return AiActMockEndpointGuard.builder()
 *                   .allow("hiring-screener", Action.EXPORT_LOG, Action.VERIFY_LOG)
 *                   .deny("internal-system", Action.SUBMIT_OVERRIDE)
 *                   .denyAllByDefault()
 *                   .build();
 *       }
 *   }
 * </pre>
 *
 * The class lives in the production source tree so a downstream test classpath can pick it up
 * without adding an extra dependency on a test-jar artifact. It is harmless in production: the
 * starter never wires it automatically.
 */
public final class AiActMockEndpointGuard implements AiActEndpointGuard {

    private final ConcurrentHashMap<String, Set<Action>> allows;
    private final ConcurrentHashMap<String, Set<Action>> denies;
    private final boolean defaultAllow;

    private AiActMockEndpointGuard(Builder b) {
        this.allows = b.allows;
        this.denies = b.denies;
        this.defaultAllow = b.defaultAllow;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Decision authorize(String systemId, Action action) {
        Set<Action> denied = denies.get(systemId);
        if (denied != null && denied.contains(action)) {
            return Decision.deny("mock-deny: " + systemId + "/" + action);
        }
        Set<Action> allowed = allows.get(systemId);
        if (allowed != null && allowed.contains(action)) {
            return Decision.allow("mock-allow: " + systemId + "/" + action);
        }
        return defaultAllow
                ? Decision.allow("mock-default-allow")
                : Decision.deny("mock-default-deny");
    }

    public static final class Builder {
        private final ConcurrentHashMap<String, Set<Action>> allows = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, Set<Action>> denies = new ConcurrentHashMap<>();
        private boolean defaultAllow = false;

        public Builder allow(String systemId, Action... actions) {
            allows.computeIfAbsent(systemId, k -> ConcurrentHashMap.newKeySet()).addAll(Set.of(actions));
            return this;
        }

        public Builder deny(String systemId, Action... actions) {
            denies.computeIfAbsent(systemId, k -> ConcurrentHashMap.newKeySet()).addAll(Set.of(actions));
            return this;
        }

        public Builder allowAllByDefault() {
            this.defaultAllow = true;
            return this;
        }

        public Builder denyAllByDefault() {
            this.defaultAllow = false;
            return this;
        }

        public AiActMockEndpointGuard build() {
            return new AiActMockEndpointGuard(this);
        }
    }
}
