/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.sample;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that with no AiActEndpointGuard bean configured and no opt-in to the unsafe
 * allow-without-guard flag, every /aiact/** call returns 403. The guarantee is the foundation of
 * the production deployment story; without it, the audit log would be readable by anyone who can
 * reach the application port.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@TestPropertySource(properties = {
        "aiact.log-dir=${java.io.tmpdir}/spring-aiact-deny-test",
        "aiact.hmac.secret=integration-test-secret",
        "aiact.retention-sweeper.enabled=false",
        "aiact.endpoints.allow-without-guard=false"
})
class EndpointDenyByDefaultTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate http;

    @Test
    void exportEndpointReturns403WhenNoGuardConfigured() {
        ResponseEntity<String> r = http.getForEntity(
                "http://localhost:" + port + "/aiact/log/export?system=hiring-screener", String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void verifyEndpointReturns403WhenNoGuardConfigured() {
        ResponseEntity<String> r = http.getForEntity(
                "http://localhost:" + port + "/aiact/log/verify?system=hiring-screener", String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void headEndpointReturns403WhenNoGuardConfigured() {
        ResponseEntity<String> r = http.getForEntity(
                "http://localhost:" + port + "/aiact/log/head?system=hiring-screener", String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
