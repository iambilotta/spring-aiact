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
import com.iambilotta.spring.aiact.audit.MetadataSanitizer;
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
    HmacChain aiActHmacChain(AiActConfigProperties props,
                             org.springframework.core.env.Environment environment) {
        com.iambilotta.spring.aiact.config.AiActProperties.Hmac hmac = props.getHmac();
        if (hmac.isDefaultSecret()) {
            boolean inDevProfile = isAnyProfileActive(environment, hmac.getDevelopmentProfiles());
            if (hmac.isFailOnDefaultInProd() && !inDevProfile) {
                throw new IllegalStateException(
                        "spring-aiact refuses to start: aiact.hmac.secret is still the default "
                        + "placeholder '" + com.iambilotta.spring.aiact.config.AiActProperties.Hmac.DEFAULT_SECRET_PLACEHOLDER
                        + "'. Override aiact.hmac.secret (or aiact.hmac.secret-ref) with a real secret, "
                        + "or activate a development profile (one of "
                        + hmac.getDevelopmentProfiles() + "). To disable this guard explicitly, set "
                        + "aiact.hmac.fail-on-default-in-prod=false (not recommended).");
            }
            org.slf4j.LoggerFactory.getLogger(AiActAutoConfiguration.class)
                    .warn("spring-aiact: aiact.hmac.secret is the default placeholder. "
                            + "Tolerated only because a development profile is active or fail-on-default-in-prod is off. "
                            + "Override before production.");
        }
        return HmacChain.fromUtf8(hmac.getSecret());
    }

    private static boolean isAnyProfileActive(org.springframework.core.env.Environment env,
                                              java.util.List<String> candidates) {
        if (candidates == null || candidates.isEmpty()) return false;
        java.util.Set<String> active = new java.util.HashSet<>();
        for (String p : env.getActiveProfiles()) active.add(p.toLowerCase(java.util.Locale.ROOT));
        for (String p : env.getDefaultProfiles()) active.add(p.toLowerCase(java.util.Locale.ROOT));
        for (String c : candidates) {
            if (active.contains(c.toLowerCase(java.util.Locale.ROOT))) return true;
        }
        return false;
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
        return new NdjsonAuditLogService(
                props.getLogDir(), hmac, mapper, props.getAudit().isSingleWriterLock());
    }

    @Bean
    @ConditionalOnMissingBean
    MetadataSanitizer aiActMetadataSanitizer() {
        return new MetadataSanitizer();
    }

    @Bean
    @ConditionalOnMissingBean
    AiActLoggingAspect aiActLoggingAspect(AuditLogService auditLog,
                                          PayloadHasher hasher,
                                          UserPseudonymizer userPseudonymizer,
                                          MetadataSanitizer metadataSanitizer) {
        return new AiActLoggingAspect(auditLog, hasher, userPseudonymizer, metadataSanitizer);
    }

    @Bean
    @ConditionalOnMissingBean
    OversightService aiActOversightService(AuditLogService auditLog,
                                           MetadataSanitizer metadataSanitizer) {
        return new OversightService(auditLog, metadataSanitizer);
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

    @Bean
    AiActStartupReporter aiActStartupReporter(
            AiActConfigProperties props,
            org.springframework.beans.factory.ObjectProvider<
                    com.iambilotta.spring.aiact.security.AiActEndpointGuard> guardProvider) {
        return new AiActStartupReporter(props, guardProvider);
    }

    /**
     * Health indicator wired only when Spring Boot Actuator is present on the classpath. Lives
     * in a nested static class so the outer auto-configuration does not require the actuator
     * to even resolve at compile time.
     */
    @Configuration(proxyBeanMethods = false)
    @org.springframework.boot.autoconfigure.condition.ConditionalOnClass(
            org.springframework.boot.actuate.health.HealthIndicator.class)
    static class HealthIndicatorAutoConfig {
        @Bean
        @ConditionalOnMissingBean(name = "aiActHealthIndicator")
        AiActHealthIndicator aiActHealthIndicator(AiActConfigProperties props) {
            return new AiActHealthIndicator(props);
        }
    }

    /**
     * {@link com.iambilotta.spring.aiact.config.AiActProperties} mirror with
     * {@code @ConfigurationProperties} binding.
     */
    @ConfigurationProperties(prefix = "aiact")
    public static class AiActConfigProperties extends com.iambilotta.spring.aiact.config.AiActProperties {
    }
}
