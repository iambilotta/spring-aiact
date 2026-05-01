/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.autoconfigure;

import com.iambilotta.spring.aiact.config.AiActProperties;
import com.iambilotta.spring.aiact.security.AiActEndpointGuard;
import com.iambilotta.spring.aiact.security.AllowAllAiActEndpointGuard;
import com.iambilotta.spring.aiact.security.DenyAllAiActEndpointGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

/**
 * Logs a single, structured INFO line at application ready time describing the active
 * spring-aiact configuration. The intent is operational: the operator opens the boot log,
 * sees the line, and knows immediately whether the audit pipeline is on the safe path.
 * <p>
 * Sample output:
 * <pre>
 * spring-aiact 0.1.0 active | endpoints=/aiact (custom-guard) | multi-process=true | retention=P10Y | hmac=ok | log-dir=/var/log/aiact
 * </pre>
 */
public class AiActStartupReporter {

    private static final Logger log = LoggerFactory.getLogger(AiActStartupReporter.class);

    private final AiActAutoConfiguration.AiActConfigProperties props;
    private final ObjectProvider<AiActEndpointGuard> guardProvider;

    public AiActStartupReporter(AiActAutoConfiguration.AiActConfigProperties props,
                                ObjectProvider<AiActEndpointGuard> guardProvider) {
        this.props = props;
        this.guardProvider = guardProvider;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void report() {
        if (!props.isEnabled()) {
            log.info("spring-aiact disabled (aiact.enabled=false). No audit pipeline is active.");
            return;
        }
        AiActProperties.Endpoints ep = props.getEndpoints();
        AiActProperties.Audit audit = props.getAudit();
        AiActProperties.Hmac hmac = props.getHmac();
        AiActEndpointGuard guard = guardProvider.getIfAvailable();
        String guardKind = describeGuard(guard);
        String hmacStatus = hmac.isDefaultSecret() ? "DEFAULT-PLACEHOLDER" : "ok";

        log.info("spring-aiact {} active | endpoints={} ({}) | multi-process={} | retention={} | hmac={} | log-dir={}",
                version(),
                ep.isEnabled() ? ep.getBasePath() : "disabled",
                guardKind,
                audit.isSingleWriterLock(),
                props.getRetention(),
                hmacStatus,
                props.getLogDir());

        if (hmac.isDefaultSecret()) {
            log.warn("spring-aiact: HMAC secret is the default placeholder. The audit chain is "
                    + "verifiable by anyone reading the source. Override aiact.hmac.secret before "
                    + "any data is written that you intend to keep.");
        }
        if (ep.isEnabled() && guard instanceof AllowAllAiActEndpointGuard) {
            log.warn("spring-aiact: AllowAllAiActEndpointGuard is active. /aiact/** is open to "
                    + "any caller that can reach the application port.");
        }
    }

    private String describeGuard(AiActEndpointGuard guard) {
        if (guard == null) return "none";
        if (guard instanceof DenyAllAiActEndpointGuard) return "deny-all (default)";
        if (guard instanceof AllowAllAiActEndpointGuard) return "allow-all (UNSAFE)";
        return "custom-guard";
    }

    private String version() {
        Package pkg = AiActStartupReporter.class.getPackage();
        String impl = pkg == null ? null : pkg.getImplementationVersion();
        return impl == null ? "0.x" : impl;
    }
}
