/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.autoconfigure;

import com.iambilotta.spring.aiact.retention.RetentionPolicyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Daily retention sweeper. Iterates the {@code logDir} once a day and prunes records older than
 * the configured retention horizon (default 10 years). Disable by setting
 * {@code aiact.retention-sweeper.enabled=false}.
 */
@Component
@ConditionalOnProperty(prefix = "aiact.retention-sweeper", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class RetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(RetentionScheduler.class);

    private final RetentionPolicyService retention;
    private final AiActAutoConfiguration.AiActConfigProperties props;

    public RetentionScheduler(RetentionPolicyService retention,
                              AiActAutoConfiguration.AiActConfigProperties props) {
        this.retention = retention;
        this.props = props;
    }

    @Scheduled(cron = "${aiact.retention-sweeper.cron:0 0 3 * * *}")
    public void sweep() {
        Path dir = props.getLogDir();
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> files = Files.list(dir)) {
            List<String> systemIds = files
                    .filter(p -> p.getFileName().toString().endsWith(".ndjson"))
                    .map(p -> stripExtension(p.getFileName().toString()))
                    .toList();
            if (systemIds.isEmpty()) return;
            RetentionPolicyService.PruneReport report = retention.prune(systemIds);
            log.info("spring-aiact retention sweep done: pruned {} kept {} cutoff {}",
                    report.pruned(), report.kept(), report.cutoff());
        } catch (IOException e) {
            log.warn("spring-aiact retention sweep failed", e);
        }
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(0, dot);
    }
}
