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
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
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

    /**
     * JVM-wide registry of intra-process locks keyed by absolute file path. Multiple
     * {@link NdjsonAuditLogService} instances pointing at the same file (typical in tests, also
     * possible in deployers that wire two beans by mistake) share the same lock so they do not
     * trip over OverlappingFileLockException on the OS-level {@link FileLock}.
     */
    private static final Map<String, ReentrantLock> JVM_FILE_LOCKS = new ConcurrentHashMap<>();

    private final Path rootDir;
    private final HmacChain hmac;
    private final ObjectMapper mapper;
    private final boolean multiProcessSafe;
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();
    private final Map<String, String> chainHeads = new ConcurrentHashMap<>();

    public NdjsonAuditLogService(Path rootDir, HmacChain hmac, ObjectMapper mapper) {
        this(rootDir, hmac, mapper, true);
    }

    /**
     * @param multiProcessSafe when {@code true} (recommended default), every append acquires an
     *                         OS-level exclusive {@link FileLock} on the per-system NDJSON file
     *                         and tails it under the lock to recompute the chain head from disk.
     *                         This is the only mode that is correct under multi-pod / multi-JVM
     *                         deployments writing to the same shared filesystem. The trade-off
     *                         is one extra fsync-class operation per append (typically 1-3 ms on
     *                         a modern SSD; slower on networked filesystems).
     *                         <p>
     *                         When {@code false}, the service relies only on its in-memory
     *                         {@link ReentrantLock} per system id. Faster, but only correct for
     *                         single-writer deployments.
     */
    public NdjsonAuditLogService(Path rootDir, HmacChain hmac, ObjectMapper mapper,
                                 boolean multiProcessSafe) {
        this.rootDir = rootDir;
        this.hmac = hmac;
        this.mapper = mapper;
        this.multiProcessSafe = multiProcessSafe;
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
            return multiProcessSafe ? appendUnderFileLock(event) : appendInProcess(event);
        } finally {
            lock.unlock();
        }
    }

    private AuditEvent appendInProcess(AuditEvent event) {
        Path file = fileFor(event.systemId());
        if (!Files.exists(file)) {
            chainHeads.remove(event.systemId());
        }
        String prev = chainHeads.computeIfAbsent(event.systemId(), k -> tailHmac(file));
        AuditEvent toPersist = sealWithChain(event, prev);
        try {
            Files.writeString(file, serialize(toPersist) + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot append audit event for " + event.systemId(), e);
        }
        chainHeads.put(event.systemId(), toPersist.recordHmac());
        return toPersist;
    }

    private AuditEvent appendUnderFileLock(AuditEvent event) {
        Path file = fileFor(event.systemId());
        ReentrantLock jvmLock = JVM_FILE_LOCKS.computeIfAbsent(
                file.toAbsolutePath().toString(), k -> new ReentrantLock());
        jvmLock.lock();
        try (FileChannel ch = FileChannel.open(file,
                StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
             FileLock fileLock = ch.lock()) {
            String prev = tailHmacFromChannel(ch);
            AuditEvent toPersist = sealWithChain(event, prev);
            ch.position(ch.size());
            ByteBuffer buf = ByteBuffer.wrap(
                    (serialize(toPersist) + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
            while (buf.hasRemaining()) ch.write(buf);
            ch.force(true);
            chainHeads.put(event.systemId(), toPersist.recordHmac());
            return toPersist;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot append audit event for " + event.systemId(), e);
        } finally {
            jvmLock.unlock();
        }
    }

    private AuditEvent sealWithChain(AuditEvent event, String prev) {
        String payload = serializeWithoutHmac(event.withRecordHmac(null));
        String mac = hmac.chain(prev, payload);
        return new AuditEvent(
                event.eventId(), event.eventKind(), event.timestamp(), event.systemId(),
                event.systemVersion(), event.operation(), event.userIdPseudonymized(),
                event.modelId(), event.inputHash(), event.outputHash(), event.hashAlgorithm(),
                event.latencyMs(), event.dbReference(), event.verifierId(),
                event.linkedEventId(), event.correlationId(), event.metadata(),
                prev, mac
        );
    }

    private String tailHmacFromChannel(FileChannel ch) throws IOException {
        long size = ch.size();
        if (size == 0) return HmacChain.CHAIN_SEED;
        long readFrom = Math.max(0, size - 65_536);
        ByteBuffer buf = ByteBuffer.allocate((int) (size - readFrom));
        ch.read(buf, readFrom);
        String tail = new String(buf.array(), 0, buf.position(), StandardCharsets.UTF_8);
        int lastNewline = tail.lastIndexOf('\n');
        if (lastNewline >= 0 && lastNewline == tail.length() - 1) {
            int prevNewline = tail.lastIndexOf('\n', lastNewline - 1);
            tail = prevNewline >= 0 ? tail.substring(prevNewline + 1, lastNewline)
                                    : tail.substring(0, lastNewline);
        } else if (lastNewline >= 0) {
            tail = tail.substring(lastNewline + 1);
        }
        if (tail.isBlank()) return HmacChain.CHAIN_SEED;
        AuditEvent ev = parseLine(tail);
        return ev == null || ev.recordHmac() == null ? HmacChain.CHAIN_SEED : ev.recordHmac();
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

    @Override
    public void writeJsonLine(Writer w, AuditEvent event) throws IOException {
        w.write(serialize(event));
        w.write('\n');
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
