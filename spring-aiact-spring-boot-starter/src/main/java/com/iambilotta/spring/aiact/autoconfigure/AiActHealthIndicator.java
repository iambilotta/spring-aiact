/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.autoconfigure;

import com.iambilotta.spring.aiact.config.AiActProperties;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * Spring Boot Actuator health indicator that surfaces the {@code spring-aiact} runtime state at
 * {@code /actuator/health/aiact}. Goes DOWN when the audit log directory is not writable or the
 * HMAC secret is the default placeholder in a non-development environment; UP otherwise. The
 * details map carries operator-relevant fields (last write time, retention, multi-process).
 * <p>
 * Auto-configured only when Spring Boot Actuator is on the classpath (the dependency is marked
 * optional in the starter pom).
 */
public class AiActHealthIndicator implements HealthIndicator {

    private final AiActAutoConfiguration.AiActConfigProperties props;

    public AiActHealthIndicator(AiActAutoConfiguration.AiActConfigProperties props) {
        this.props = props;
    }

    @Override
    public Health health() {
        Health.Builder builder = Health.up();
        Path logDir = props.getLogDir();
        boolean dirOk = isDirectoryWritable(logDir);
        AiActProperties.Hmac hmac = props.getHmac();

        builder.withDetail("log-dir", logDir.toAbsolutePath().toString());
        builder.withDetail("log-dir-writable", dirOk);
        builder.withDetail("retention", props.getRetention().toString());
        builder.withDetail("multi-process-safe", props.getAudit().isSingleWriterLock());
        builder.withDetail("hmac-secret", hmac.isDefaultSecret() ? "DEFAULT-PLACEHOLDER" : "configured");
        builder.withDetail("endpoints-enabled", props.getEndpoints().isEnabled());
        builder.withDetail("last-append-system", lastAppendSummary(logDir));

        if (!dirOk) {
            return builder.down()
                    .withDetail("reason", "log directory is not writable: " + logDir)
                    .build();
        }
        if (hmac.isDefaultSecret() && hmac.isFailOnDefaultInProd()) {
            return builder.down()
                    .withDetail("reason", "HMAC secret is the default placeholder; "
                            + "see aiact.hmac.secret")
                    .build();
        }
        return builder.build();
    }

    private boolean isDirectoryWritable(Path dir) {
        try {
            if (!Files.isDirectory(dir)) {
                Files.createDirectories(dir);
            }
            return Files.isWritable(dir);
        } catch (IOException e) {
            return false;
        }
    }

    private String lastAppendSummary(Path dir) {
        if (!Files.isDirectory(dir)) return "directory not present";
        try (java.util.stream.Stream<Path> files = Files.list(dir)) {
            return files
                    .filter(p -> p.getFileName().toString().endsWith(".ndjson"))
                    .max(Comparator.comparingLong(p -> p.toFile().lastModified()))
                    .map(p -> p.getFileName().toString() + " @ "
                            + java.time.Instant.ofEpochMilli(p.toFile().lastModified()))
                    .orElse("no audit file yet");
        } catch (IOException e) {
            return "error reading directory: " + e.getMessage();
        }
    }
}
