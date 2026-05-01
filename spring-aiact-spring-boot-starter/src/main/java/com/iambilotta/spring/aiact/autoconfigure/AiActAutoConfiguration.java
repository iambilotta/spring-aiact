/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.iambilotta.spring.aiact.audit.AiActLoggingAspect;
import com.iambilotta.spring.aiact.audit.AuditLogService;
import com.iambilotta.spring.aiact.audit.HmacChain;
import com.iambilotta.spring.aiact.audit.NdjsonAuditLogService;
import com.iambilotta.spring.aiact.audit.PayloadHasher;
import com.iambilotta.spring.aiact.audit.UserPseudonymizer;
import com.iambilotta.spring.aiact.codegen.AuditExportPackager;
import com.iambilotta.spring.aiact.codegen.datasheet.DatasetDatasheetRenderer;
import com.iambilotta.spring.aiact.codegen.markdown.TechnicalFileMarkdownRenderer;
import com.iambilotta.spring.aiact.codegen.pdf.DeclarationOfConformityPdfGenerator;
import com.iambilotta.spring.aiact.oversight.OversightService;
import com.iambilotta.spring.aiact.retention.RetentionPolicyService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@AutoConfiguration
@ConditionalOnProperty(prefix = "aiact", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AiActAutoConfiguration.AiActConfigProperties.class)
@EnableScheduling
@Import(AiActWebAutoConfiguration.class)
public class AiActAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "aiActObjectMapper")
    ObjectMapper aiActObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Bean
    @ConditionalOnMissingBean
    HmacChain aiActHmacChain(AiActConfigProperties props) {
        String secret = props.getHmac().getSecret();
        if ("change-me-please".equals(secret)) {
            org.slf4j.LoggerFactory.getLogger(AiActAutoConfiguration.class)
                    .warn("spring-aiact: aiact.hmac.secret is set to the default placeholder. "
                            + "Override it before going to production.");
        }
        return HmacChain.fromUtf8(secret);
    }

    @Bean
    @ConditionalOnMissingBean
    PayloadHasher aiActPayloadHasher(ObjectMapper mapper) {
        return new PayloadHasher(mapper);
    }

    @Bean
    @ConditionalOnMissingBean
    UserPseudonymizer aiActUserPseudonymizer() {
        return UserPseudonymizer.noop();
    }

    @Bean
    @ConditionalOnMissingBean
    AuditLogService aiActAuditLogService(AiActConfigProperties props,
                                          HmacChain hmac,
                                          ObjectMapper mapper) {
        return new NdjsonAuditLogService(props.getLogDir(), hmac, mapper);
    }

    @Bean
    @ConditionalOnMissingBean
    AiActLoggingAspect aiActLoggingAspect(AuditLogService auditLog,
                                          PayloadHasher hasher,
                                          UserPseudonymizer userPseudonymizer) {
        return new AiActLoggingAspect(auditLog, hasher, userPseudonymizer);
    }

    @Bean
    @ConditionalOnMissingBean
    OversightService aiActOversightService(AuditLogService auditLog) {
        return new OversightService(auditLog);
    }

    @Bean
    @ConditionalOnMissingBean
    RetentionPolicyService aiActRetentionPolicyService(AuditLogService auditLog,
                                                       AiActConfigProperties props) {
        if (auditLog instanceof NdjsonAuditLogService nd) {
            return new RetentionPolicyService(nd, props.getRetention());
        }
        throw new IllegalStateException(
                "Default RetentionPolicyService requires NdjsonAuditLogService. "
                + "Provide your own RetentionPolicyService bean when using a custom AuditLogService.");
    }

    @Bean
    @ConditionalOnMissingBean
    TechnicalFileMarkdownRenderer aiActTechnicalFileRenderer() {
        return new TechnicalFileMarkdownRenderer();
    }

    @Bean
    @ConditionalOnMissingBean
    DeclarationOfConformityPdfGenerator aiActDoCGenerator() {
        return new DeclarationOfConformityPdfGenerator();
    }

    @Bean
    @ConditionalOnMissingBean
    DatasetDatasheetRenderer aiActDatasetDatasheetRenderer() {
        return new DatasetDatasheetRenderer();
    }

    @Bean
    @ConditionalOnMissingBean
    AuditExportPackager aiActAuditExportPackager(HmacChain hmac) {
        return new AuditExportPackager(hmac);
    }

    /**
     * {@link com.iambilotta.spring.aiact.config.AiActProperties} mirror with
     * {@code @ConfigurationProperties} binding.
     */
    @ConfigurationProperties(prefix = "aiact")
    public static class AiActConfigProperties extends com.iambilotta.spring.aiact.config.AiActProperties {
    }
}
