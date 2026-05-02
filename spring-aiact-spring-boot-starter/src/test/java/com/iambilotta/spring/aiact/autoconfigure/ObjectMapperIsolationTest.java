/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for the bug where {@code aiActObjectMapper} satisfied
 * {@code @ConditionalOnMissingBean(ObjectMapper.class)} of {@code JacksonAutoConfiguration}
 * and replaced the application's primary ObjectMapper, breaking Spring MVC's HTTP message
 * conversion for any user app importing the starter.
 *
 * <p>The aiact ObjectMapper is intentionally configured with SNAKE_CASE for deterministic
 * audit-record hashing. The application's ObjectMapper must stay independent of that choice.
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
    void aiActObjectMapperIsStillReachableByQualifier() {
        runner.run(ctx -> {
            ObjectMapper aiActMapper = (ObjectMapper) ctx.getBean("aiActObjectMapper");
            assertThat(aiActMapper.getPropertyNamingStrategy())
                    .as("the aiact-internal mapper must remain SNAKE_CASE for deterministic hashing")
                    .isInstanceOf(PropertyNamingStrategies.SnakeCaseStrategy.class);
        });
    }
}
