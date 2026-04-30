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

    public static class Endpoints {
        private boolean enabled = true;
        private String basePath = "/aiact";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getBasePath() { return basePath; }
        public void setBasePath(String basePath) { this.basePath = basePath; }
    }

    public static class Hmac {
        /** Plain-text secret. Use {@code aiact.hmac.secret-ref} when reading from a vault. */
        private String secret = "change-me-please";
        /** Optional reference to a Spring Cloud Config / Vault key. */
        private String secretRef;

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public String getSecretRef() { return secretRef; }
        public void setSecretRef(String secretRef) { this.secretRef = secretRef; }
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
