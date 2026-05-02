/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.sample;

import com.iambilotta.spring.aiact.audit.AuditLogService;
import com.iambilotta.spring.aiact.audit.NdjsonAuditLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@TestPropertySource(properties = {
        "aiact.log-dir=${java.io.tmpdir}/spring-aiact-it",
        "aiact.hmac.secret=integration-test-secret",
        "aiact.retention-sweeper.enabled=false",
        "aiact.endpoints.allow-without-guard=true"
})
class HiringScreenerIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate http;

    @Autowired
    AuditLogService auditLogService;

    @Value("${aiact.log-dir}")
    String logDir;

    @Test
    void scoringProducesArticle12RecordWithValidHmacChain() throws Exception {
        Path file = ((NdjsonAuditLogService) auditLogService).fileFor("hiring-screener");
        if (Files.exists(file)) Files.delete(file);

        for (int i = 0; i < 3; i++) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<CandidateApplication> req = new HttpEntity<>(
                    new CandidateApplication("cand-" + i, "cv-text-" + String.valueOf(i).repeat(50)),
                    headers);
            ResponseEntity<ScoringResult> r = http.postForEntity(
                    "http://localhost:" + port + "/hiring/score", req, ScoringResult.class);
            assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
        }

        assertThat(Files.exists(file)).isTrue();
        String content = Files.readString(file, StandardCharsets.UTF_8);
        assertThat(content.lines().count()).isEqualTo(3);
        assertThat(content).contains("\"hiring-screener\"");
        assertThat(content).contains("\"hash_algorithm\":\"SHA-256\"");

        AuditLogService.ChainVerification verification = auditLogService.verify(
                "hiring-screener", Instant.EPOCH, Instant.now().plusSeconds(60));
        assertThat(verification.valid()).isTrue();
        assertThat(verification.inspected()).isEqualTo(3);
    }

    @Test
    void exportEndpointReturnsNdjsonAndVerifyEndpointIsOk() throws Exception {
        Path file = ((NdjsonAuditLogService) auditLogService).fileFor("hiring-screener");
        if (Files.exists(file)) Files.delete(file);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        http.postForEntity(
                "http://localhost:" + port + "/hiring/score",
                new HttpEntity<>(new CandidateApplication("c1", "abc"), headers),
                ScoringResult.class);

        ResponseEntity<String> exportResp = http.getForEntity(
                "http://localhost:" + port + "/aiact/log/export?system=hiring-screener", String.class);
        assertThat(exportResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(exportResp.getBody()).contains("\"hiring-screener\"");

        ResponseEntity<String> verifyResp = http.getForEntity(
                "http://localhost:" + port + "/aiact/log/verify?system=hiring-screener",
                String.class);
        assertThat(verifyResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(verifyResp.getBody()).contains("\"invalid\":0");
        assertThat(verifyResp.getBody()).contains("\"inspected\":1");
    }
}
