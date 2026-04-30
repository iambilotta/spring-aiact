/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.autoconfigure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iambilotta.spring.aiact.audit.AuditLogService;
import com.iambilotta.spring.aiact.audit.HmacChain;
import com.iambilotta.spring.aiact.model.AuditEvent;
import com.iambilotta.spring.aiact.security.AiActEndpointGuard;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * REST endpoints exposed by the spring-aiact starter for the audit log.
 *
 * <ul>
 *   <li>{@code GET /aiact/log/export} streams the NDJSON slice for a system in a time range.</li>
 *   <li>{@code GET /aiact/log/verify} returns the HMAC chain verification report.</li>
 *   <li>{@code GET /aiact/log/head} returns the current chain head HMAC (acts as a tamper canary).</li>
 * </ul>
 */
@RestController
@RequestMapping("${aiact.endpoints.base-path:/aiact}/log")
public class AiActLogController {

    private final AuditLogService auditLog;
    private final ObjectMapper mapper;
    private final AiActEndpointGuard guard;

    public AiActLogController(AuditLogService auditLog, ObjectMapper mapper,
                              AiActEndpointGuard guard) {
        this.auditLog = auditLog;
        this.mapper = mapper;
        this.guard = guard;
    }

    private void enforce(String systemId, AiActEndpointGuard.Action action) {
        AiActEndpointGuard.Decision d = guard.authorize(systemId, action);
        if (!d.allowed()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, d.reason());
        }
    }

    @GetMapping(value = "/export", produces = "application/x-ndjson")
    public ResponseEntity<StreamingResponseBody> export(
            @RequestParam("system") String systemId,
            @RequestParam(value = "from", required = false) Instant from,
            @RequestParam(value = "to", required = false) Instant to) {
        enforce(systemId, AiActEndpointGuard.Action.EXPORT_LOG);
        StreamingResponseBody body = out -> {
            try (Writer w = new OutputStreamWriter(out, StandardCharsets.UTF_8);
                 Stream<AuditEvent> events = auditLog.stream(systemId, from, to)) {
                events.forEach(event -> writeJsonLine(w, event));
                w.flush();
            }
        };
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/x-ndjson"))
                .header("X-AiAct-System", systemId)
                .header("X-AiAct-Schema", "article-12")
                .body(body);
    }

    private void writeJsonLine(Writer w, AuditEvent event) {
        try {
            w.write(mapper.writeValueAsString(event));
            w.write('\n');
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @GetMapping(value = "/verify", produces = MediaType.APPLICATION_JSON_VALUE)
    public AuditLogService.ChainVerification verify(
            @RequestParam("system") String systemId,
            @RequestParam(value = "from", required = false) Instant from,
            @RequestParam(value = "to", required = false) Instant to) {
        enforce(systemId, AiActEndpointGuard.Action.VERIFY_LOG);
        return auditLog.verify(systemId, from, to);
    }

    @GetMapping(value = "/head", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> head(@RequestParam("system") String systemId) {
        enforce(systemId, AiActEndpointGuard.Action.READ_HEAD);
        AtomicReference<String> last = new AtomicReference<>(HmacChain.CHAIN_SEED);
        try (Stream<AuditEvent> s = auditLog.stream(systemId, null, null)) {
            s.forEach(e -> {
                if (e.recordHmac() != null) last.set(e.recordHmac());
            });
        }
        return Map.of("system_id", systemId, "head_hmac", last.get());
    }
}
