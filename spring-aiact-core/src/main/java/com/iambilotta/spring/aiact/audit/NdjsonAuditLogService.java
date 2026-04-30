/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iambilotta.spring.aiact.model.AuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

/**
 * Default {@link AuditLogService} implementation. Writes one append-only NDJSON file per
 * {@code system-id} under the configured root directory. Each record carries the HMAC chain
 * signature; the implementation reconstructs the head of the chain by tailing the last line
 * the first time a system is touched after a process restart.
 */
public class NdjsonAuditLogService implements AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(NdjsonAuditLogService.class);

    private final Path rootDir;
    private final HmacChain hmac;
    private final ObjectMapper mapper;
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();
    private final Map<String, String> chainHeads = new ConcurrentHashMap<>();

    public NdjsonAuditLogService(Path rootDir, HmacChain hmac, ObjectMapper mapper) {
        this.rootDir = rootDir;
        this.hmac = hmac;
        this.mapper = mapper;
        try {
            Files.createDirectories(rootDir);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create audit root " + rootDir, e);
        }
    }

    @Override
    public AuditEvent append(AuditEvent event) {
        String systemId = event.systemId();
        if (systemId == null || systemId.isBlank()) {
            throw new IllegalArgumentException("AuditEvent.systemId is mandatory");
        }
        ReentrantLock lock = locks.computeIfAbsent(systemId, k -> new ReentrantLock());
        lock.lock();
        try {
            Path file = fileFor(systemId);
            if (!Files.exists(file)) {
                chainHeads.remove(systemId);
            }
            String prev = chainHeads.computeIfAbsent(systemId, k -> tailHmac(file));
            String payload = serializeWithoutHmac(event.withRecordHmac(null));
            String mac = hmac.chain(prev, payload);
            AuditEvent toPersist = new AuditEvent(
                    event.eventId(), event.eventKind(), event.timestamp(), event.systemId(),
                    event.systemVersion(), event.operation(), event.userIdPseudonymized(),
                    event.modelId(), event.inputHash(), event.outputHash(), event.hashAlgorithm(),
                    event.latencyMs(), event.dbReference(), event.verifierId(),
                    event.linkedEventId(), event.correlationId(), event.metadata(),
                    prev, mac
            );
            String json = serialize(toPersist);
            Files.writeString(file, json + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            chainHeads.put(systemId, mac);
            return toPersist;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot append audit event for " + systemId, e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Stream<AuditEvent> stream(String systemId, Instant from, Instant to) {
        Path file = fileFor(systemId);
        if (!Files.exists(file)) {
            return Stream.empty();
        }
        try {
            BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
            return reader.lines()
                    .map(this::parseLine)
                    .filter(java.util.Objects::nonNull)
                    .filter(e -> within(e.timestamp(), from, to))
                    .onClose(() -> {
                        try { reader.close(); } catch (IOException ignored) { /* best effort */ }
                    });
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read audit log for " + systemId, e);
        }
    }

    @Override
    public ChainVerification verify(String systemId, Instant from, Instant to) {
        Path file = fileFor(systemId);
        long inspected = 0;
        long invalid = 0;
        List<String> failures = new ArrayList<>();
        if (!Files.exists(file)) {
            return new ChainVerification(systemId, from, to, 0, 0, failures);
        }
        String prev = HmacChain.CHAIN_SEED;
        try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
            for (String line : (Iterable<String>) lines::iterator) {
                if (line.isBlank()) continue;
                AuditEvent event = parseLine(line);
                if (event == null) continue;
                boolean inRange = within(event.timestamp(), from, to);
                if (inRange) inspected++;
                String payload = serializeWithoutHmac(event.withRecordHmac(null));
                boolean ok = hmac.verify(prev, payload, event.recordHmac())
                        && safeEquals(prev, event.prevHmac());
                if (!ok && inRange) {
                    invalid++;
                    failures.add(String.valueOf(event.eventId()));
                }
                prev = event.recordHmac();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot verify audit log for " + systemId, e);
        }
        return new ChainVerification(systemId, from, to, inspected, invalid, failures);
    }

    /** Visible for testing and for retention-driven pruning. */
    public Path fileFor(String systemId) {
        String safe = systemId.replaceAll("[^A-Za-z0-9._-]", "_");
        return rootDir.resolve(safe + ".ndjson");
    }

    private String serialize(AuditEvent event) {
        try {
            return mapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialize audit event " + event.eventId(), e);
        }
    }

    private String serializeWithoutHmac(AuditEvent event) {
        AuditEvent canonical = new AuditEvent(
                event.eventId(), event.eventKind(), event.timestamp(), event.systemId(),
                event.systemVersion(), event.operation(), event.userIdPseudonymized(),
                event.modelId(), event.inputHash(), event.outputHash(), event.hashAlgorithm(),
                event.latencyMs(), event.dbReference(), event.verifierId(),
                event.linkedEventId(), event.correlationId(), event.metadata(),
                null, null
        );
        try {
            return mapper.writeValueAsString(canonical);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialize audit event " + event.eventId(), e);
        }
    }

    private AuditEvent parseLine(String line) {
        try {
            return mapper.readValue(line, AuditEvent.class);
        } catch (Exception e) {
            log.warn("Skipping malformed audit line", e);
            return null;
        }
    }

    private String tailHmac(Path file) {
        if (!Files.exists(file)) {
            return HmacChain.CHAIN_SEED;
        }
        String last = null;
        try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
            for (String line : (Iterable<String>) lines::iterator) {
                if (!line.isBlank()) {
                    last = line;
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot tail audit log " + file, e);
        }
        if (last == null) {
            return HmacChain.CHAIN_SEED;
        }
        AuditEvent ev = parseLine(last);
        return ev == null || ev.recordHmac() == null ? HmacChain.CHAIN_SEED : ev.recordHmac();
    }

    private static boolean within(Instant ts, Instant from, Instant to) {
        if (ts == null) return false;
        if (from != null && ts.isBefore(from)) return false;
        if (to != null && ts.isAfter(to)) return false;
        return true;
    }

    private static boolean safeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) diff |= a.charAt(i) ^ b.charAt(i);
        return diff == 0;
    }
}
