/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
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
 * v1.0.0 removes the bean entirely and folds the audit-record mapper inside the consumers
 * that need it. This test ensures the regression cannot come back: there is no
 * {@code aiActObjectMapper} bean, and Spring Boot's primary mapper is intact.
 */
class ObjectMapperIsolationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JacksonAutoConfiguration.class,
                    AiActAutoConfiguration.class))
            .withPropertyValues(
                    "spring.profiles.active=dev",
                    "aiact.endpoints.enabled=false",
                    "aiact.retention-sweeper.enabled=false");

    @Test
    void springBootObjectMapperIsNotReplacedByAiActOne() {
        runner.run(ctx -> {
            ObjectMapper primary = ctx.getBean(ObjectMapper.class);
            assertThat(primary.getPropertyNamingStrategy())
                    .as("Spring Boot's primary ObjectMapper must keep the app's naming strategy "
                        + "(default null = LOWER_CAMEL_CASE), not the aiact SNAKE_CASE one")
                    .isNull();
        });
    }

    @Test
    void aiActDoesNotPublishAnInternalObjectMapperBean() {
        runner.run(ctx -> {
            assertThat(ctx)
                    .as("the aiact internal ObjectMapper is an implementation detail "
                        + "and must not appear as a bean in the Spring context")
                    .doesNotHaveBean("aiActObjectMapper");
            String[] mapperBeans = ctx.getBeanNamesForType(ObjectMapper.class);
            assertThat(mapperBeans)
                    .as("only Spring Boot's mapper should be in the context")
                    .hasSize(1);
        });
    }
}
