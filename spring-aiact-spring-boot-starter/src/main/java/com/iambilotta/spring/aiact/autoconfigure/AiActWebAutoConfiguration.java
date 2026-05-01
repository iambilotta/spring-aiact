/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.autoconfigure;

import com.iambilotta.spring.aiact.autoconfigure.web.AiActLogController;
import com.iambilotta.spring.aiact.autoconfigure.web.AiActOversightController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.RestController;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(RestController.class)
@ConditionalOnProperty(prefix = "aiact.endpoints", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({AiActLogController.class, AiActOversightController.class})
public class AiActWebAutoConfiguration {
}
