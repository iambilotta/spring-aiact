/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.autoconfigure;

import com.iambilotta.spring.aiact.annotation.AiActTransparency;
import com.iambilotta.spring.aiact.autoconfigure.web.AiActLogController;
import com.iambilotta.spring.aiact.autoconfigure.web.AiActOversightController;
import com.iambilotta.spring.aiact.autoconfigure.web.AiActTransparencyFilter;
import com.iambilotta.spring.aiact.security.AiActEndpointGuard;
import com.iambilotta.spring.aiact.security.AllowAllAiActEndpointGuard;
import com.iambilotta.spring.aiact.security.DenyAllAiActEndpointGuard;
import com.iambilotta.spring.aiact.transparency.TransparencyDisclosure;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(RestController.class)
@ConditionalOnProperty(prefix = "aiact.endpoints", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({AiActLogController.class, AiActOversightController.class})
public class AiActWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    AiActEndpointGuard aiActEndpointGuard(AiActAutoConfiguration.AiActConfigProperties props) {
        return props.getEndpoints().isAllowWithoutGuard()
                ? new AllowAllAiActEndpointGuard()
                : new DenyAllAiActEndpointGuard();
    }

    /**
     * Article 50 transparency filter, wired only when at least one bean is annotated
     * {@code @AiActTransparency}. The path prefix is derived from the class-level
     * {@code @RequestMapping} of the annotated surface (defaulting to {@code /**} when none is
     * declared, since the surface still owes disclosure on every path it serves).
     */
    @Bean
    @ConditionalOnMissingBean
    AiActTransparencyFilter aiActTransparencyFilter(ApplicationContext context) {
        List<AiActTransparencyFilter.Mapping> mappings = new ArrayList<>();
        for (Object bean : context.getBeansWithAnnotation(AiActTransparency.class).values()) {
            Class<?> type = AopUtils.getTargetClass(bean);
            TransparencyDisclosure disclosure = TransparencyDisclosure.forType(type);
            if (disclosure == null) {
                continue;
            }
            for (String pattern : pathPatternsFor(type)) {
                mappings.add(new AiActTransparencyFilter.Mapping(pattern, disclosure));
            }
        }
        return new AiActTransparencyFilter(mappings);
    }

    private static List<String> pathPatternsFor(Class<?> type) {
        RequestMapping rm = AnnotatedElementUtils.findMergedAnnotation(type, RequestMapping.class);
        List<String> patterns = new ArrayList<>();
        if (rm != null) {
            for (String base : rm.path().length > 0 ? rm.path() : rm.value()) {
                if (!base.isBlank()) {
                    patterns.add(base.endsWith("/**") ? base
                            : (base.endsWith("/") ? base + "**" : base + "/**"));
                }
            }
        }
        if (patterns.isEmpty()) {
            patterns.add("/**");
        }
        return patterns;
    }
}
