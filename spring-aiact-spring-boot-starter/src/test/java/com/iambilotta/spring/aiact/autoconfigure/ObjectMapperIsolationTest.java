/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the contract that the spring-aiact starter never exposes its internal,
 * snake_case-configured ObjectMapper to the application's Spring context.
 *
 * <p>History: in v0.1.x the starter registered an {@code aiActObjectMapper} bean of
 * type {@link ObjectMapper}. That bean satisfied
 * {@code @ConditionalOnMissingBean(ObjectMapper.class)} of {@code JacksonAutoConfiguration}
 * and replaced Spring Boot's primary mapper, breaking Spring MVC HTTP message conversion.
 * v1.0.0 removed the bean entirely and folded the audit-record mapper inside the consumers
 * that need it.
 *
 * <p>v2.0.0 (Spring Boot 4 line): Spring Boot 4 ships Jackson 3 ({@code tools.jackson.*})
 * and no longer registers a Jackson 2 {@link ObjectMapper} bean by default; the
 * shadowing failure mode of v0.1.x is structurally impossible. This test now only asserts
 * the architectural invariant: <strong>the starter publishes no Jackson 2
 * {@code ObjectMapper} bean of any kind</strong>. The audit-record mapper stays an
 * implementation detail of {@code PayloadHasher} and {@code NdjsonAuditLogService}.
 */
class ObjectMapperIsolationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AiActAutoConfiguration.class))
            .withPropertyValues(
                    "spring.profiles.active=dev",
                    "aiact.endpoints.enabled=false",
                    "aiact.retention-sweeper.enabled=false");

    @Test
    void aiActDoesNotPublishAnInternalObjectMapperBean() {
        runner.run(ctx -> {
            assertThat(ctx)
                    .as("the aiact internal ObjectMapper is an implementation detail "
                        + "and must not appear as a bean in the Spring context")
                    .doesNotHaveBean("aiActObjectMapper");
            String[] mapperBeans = ctx.getBeanNamesForType(ObjectMapper.class);
            assertThat(mapperBeans)
                    .as("the starter exposes zero Jackson 2 ObjectMapper beans; the "
                        + "application is free to register its own")
                    .isEmpty();
        });
    }
}
