/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.config;

import java.nio.file.Path;
import java.time.Period;

/**
 * Configuration holder for the spring-aiact starter. Mirrored as
 * {@code @ConfigurationProperties} in the Spring Boot starter module.
 */
public class AiActProperties {

    private boolean enabled = true;
    private Path logDir = Path.of("aiact-logs");
    private Period retention = Period.ofYears(10);
    private Endpoints endpoints = new Endpoints();
    private Hmac hmac = new Hmac();
    private Encryption encryption = new Encryption();
    private Audit audit = new Audit();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Path getLogDir() { return logDir; }
    public void setLogDir(Path logDir) { this.logDir = logDir; }

    public Period getRetention() { return retention; }
    public void setRetention(Period retention) { this.retention = retention; }

    public Endpoints getEndpoints() { return endpoints; }
    public void setEndpoints(Endpoints endpoints) { this.endpoints = endpoints; }

    public Hmac getHmac() { return hmac; }
    public void setHmac(Hmac hmac) { this.hmac = hmac; }

    public Encryption getEncryption() { return encryption; }
    public void setEncryption(Encryption encryption) { this.encryption = encryption; }

    public Audit getAudit() { return audit; }
    public void setAudit(Audit audit) { this.audit = audit; }

    public static class Audit {
        /**
         * When {@code true} (default), the file-backed audit log acquires an OS-level
         * {@link java.nio.channels.FileLock} on every append and tails the file under the lock.
         * Required when the same NDJSON path is shared across pods, JVMs or hosts (typical in
         * Kubernetes deployments with a ReadWriteMany volume). Set to {@code false} only for
         * single-writer deployments where the contention overhead is undesirable.
         */
        private boolean singleWriterLock = true;

        public boolean isSingleWriterLock() { return singleWriterLock; }
        public void setSingleWriterLock(boolean v) { this.singleWriterLock = v; }
    }

    public static class Endpoints {
        private boolean enabled = true;
        private String basePath = "/aiact";
        /**
         * When {@code true}, allow the unsafe {@code AllowAllAiActEndpointGuard} as the default
         * guard. Intended for local development only. The starter registers
         * {@code DenyAllAiActEndpointGuard} when this flag is {@code false}, so production
         * deployments that forget to wire a real guard will return 403, not leak the audit log.
         */
        private boolean allowWithoutGuard = false;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getBasePath() { return basePath; }
        public void setBasePath(String basePath) { this.basePath = basePath; }
        public boolean isAllowWithoutGuard() { return allowWithoutGuard; }
        public void setAllowWithoutGuard(boolean v) { this.allowWithoutGuard = v; }
    }

    public static class Hmac {
        /** Sentinel value reported by {@link #isDefaultSecret()} to drive fail-fast on boot. */
        public static final String DEFAULT_SECRET_PLACEHOLDER = "change-me-please";

        /** Plain-text secret. Use {@code aiact.hmac.secret-ref} when reading from a vault. */
        private String secret = DEFAULT_SECRET_PLACEHOLDER;
        /** Optional reference to a Spring Cloud Config / Vault key. */
        private String secretRef;
        /**
         * When {@code true} (default), the auto-configuration refuses to start if
         * {@link #getSecret()} is still the placeholder and the active Spring profile is not one
         * of the development profiles in {@link #getDevelopmentProfiles()}. Set to {@code false}
         * only for emergency hotfix scenarios; the placeholder secret defeats the entire chain.
         */
        private boolean failOnDefaultInProd = true;
        /** Profile names where the default secret is tolerated. Lower-case match. */
        private java.util.List<String> developmentProfiles = java.util.List.of("dev", "test", "local");

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public String getSecretRef() { return secretRef; }
        public void setSecretRef(String secretRef) { this.secretRef = secretRef; }
        public boolean isFailOnDefaultInProd() { return failOnDefaultInProd; }
        public void setFailOnDefaultInProd(boolean v) { this.failOnDefaultInProd = v; }
        public java.util.List<String> getDevelopmentProfiles() { return developmentProfiles; }
        public void setDevelopmentProfiles(java.util.List<String> v) { this.developmentProfiles = v; }

        public boolean isDefaultSecret() {
            return DEFAULT_SECRET_PLACEHOLDER.equals(secret);
        }
    }

    public static class Encryption {
        private boolean enabled = false;
        /** Reference to a key handle (KMS arn, Vault path, JKS alias). Resolution is deployer specific. */
        private String keyRef;
        private String algorithm = "AES/GCM/NoPadding";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getKeyRef() { return keyRef; }
        public void setKeyRef(String keyRef) { this.keyRef = keyRef; }
        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
    }
}
