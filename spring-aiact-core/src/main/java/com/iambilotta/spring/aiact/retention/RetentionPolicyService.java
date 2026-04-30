/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.retention;

import com.iambilotta.spring.aiact.audit.NdjsonAuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Prunes audit log records older than the configured retention horizon. The default retention is
 * 10 years to align with the AI Act Declaration of Conformity archival window. Pruning operates
 * on each {@code system-id} NDJSON file individually; the pruned-out lines are not forwarded
 * anywhere, so any export must run before the retention sweeper.
 * <p>
 * The implementation rewrites the NDJSON file in a {@code .pruning} sibling and renames it
 * atomically. The HMAC chain remains valid for the kept slice as long as callers verify the
 * chain starting from the new head; the seed before the new head is the {@code prevHmac}
 * stored on the new first record.
 */
public class RetentionPolicyService {

    private static final Logger log = LoggerFactory.getLogger(RetentionPolicyService.class);

    private final NdjsonAuditLogService auditLog;
    private final Period retention;

    public RetentionPolicyService(NdjsonAuditLogService auditLog, Period retention) {
        this.auditLog = auditLog;
        this.retention = retention == null ? Period.ofYears(10) : retention;
    }

    public PruneReport prune(List<String> systemIds) {
        Instant cutoff = Instant.now()
                .atOffset(ZoneOffset.UTC)
                .toLocalDate()
                .minus(retention)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);
        long totalPruned = 0;
        long totalKept = 0;
        for (String systemId : systemIds) {
            PruneOutcome outcome = pruneOne(systemId, cutoff);
            totalPruned += outcome.pruned();
            totalKept += outcome.kept();
        }
        return new PruneReport(cutoff, totalPruned, totalKept, retention);
    }

    public PruneReport prune(String systemId) {
        return prune(List.of(systemId));
    }

    private PruneOutcome pruneOne(String systemId, Instant cutoff) {
        Path file = auditLog.fileFor(systemId);
        if (!Files.exists(file)) {
            return new PruneOutcome(0, 0);
        }
        Path tmp = file.resolveSibling(file.getFileName().toString() + ".pruning");
        long pruned = 0;
        long kept = 0;
        try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
            List<String> survivors = new ArrayList<>();
            for (String line : (Iterable<String>) lines::iterator) {
                if (line.isBlank()) continue;
                if (isOlderThan(line, cutoff)) {
                    pruned++;
                } else {
                    survivors.add(line);
                    kept++;
                }
            }
            if (pruned == 0) {
                return new PruneOutcome(0, kept);
            }
            Files.write(tmp,
                    String.join(System.lineSeparator(), survivors).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            if (!survivors.isEmpty()) {
                Files.writeString(tmp, System.lineSeparator(),
                        StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            }
            Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            log.info("spring-aiact retention: pruned {} records from {} (kept {})", pruned, systemId, kept);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot prune retention for " + systemId, e);
        }
        return new PruneOutcome(pruned, kept);
    }

    private boolean isOlderThan(String ndjsonLine, Instant cutoff) {
        int idx = ndjsonLine.indexOf("\"timestamp\":");
        if (idx < 0) return false;
        int start = ndjsonLine.indexOf('"', idx + "\"timestamp\":".length());
        if (start < 0) return false;
        int end = ndjsonLine.indexOf('"', start + 1);
        if (end < 0) return false;
        try {
            Instant ts = Instant.parse(ndjsonLine.substring(start + 1, end));
            return ts.isBefore(cutoff);
        } catch (java.time.format.DateTimeParseException e) {
            return false;
        }
    }

    public Duration retentionAsDuration() {
        return Duration.ofDays(retention.toTotalMonths() * 30L + retention.getDays());
    }

    public record PruneReport(Instant cutoff, long pruned, long kept, Period retention) { }

    private record PruneOutcome(long pruned, long kept) { }
}
