/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.audit;

/**
 * Resolves the pseudonymous user identifier to record on each Article 12 audit event.
 * The default implementation returns {@code null} (no user attribution).
 * <p>
 * Replace by registering your own bean wired to Spring Security context, request scope,
 * or any other source of authenticated identity. Always pseudonymize at this boundary:
 * the audit log is not a place for raw email addresses or government identifiers.
 */
@FunctionalInterface
public interface UserPseudonymizer {

    String resolve();

    static UserPseudonymizer noop() {
        return () -> null;
    }
}
