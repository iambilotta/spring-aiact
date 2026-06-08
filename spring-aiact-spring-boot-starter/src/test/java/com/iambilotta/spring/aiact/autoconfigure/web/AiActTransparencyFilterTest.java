/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.autoconfigure.web;

import com.iambilotta.spring.aiact.annotation.TransparencyKind;
import com.iambilotta.spring.aiact.transparency.TransparencyDisclosure;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiActTransparencyFilterTest {

    @Test
    void stampsTheDisclosureHeaderOnAResponseFromAnAiSurface() throws Exception {
        TransparencyDisclosure chatbot = new TransparencyDisclosure(
                TransparencyKind.AI_INTERACTION, "You are talking to a bot.");
        AiActTransparencyFilter filter = new AiActTransparencyFilter(
                List.of(new AiActTransparencyFilter.Mapping("/chat/**", chatbot)));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/chat/ask");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> { /* downstream handler */ };

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(TransparencyDisclosure.HEADER_NAME))
                .isEqualTo("ai-interaction; article=50");
        assertThat(response.getHeader("X-AiAct-Transparency-Message"))
                .isEqualTo("You are talking to a bot.");
    }

    @Test
    void leavesNonAiSurfacesUntouched() throws Exception {
        AiActTransparencyFilter filter = new AiActTransparencyFilter(
                List.of(new AiActTransparencyFilter.Mapping("/chat/**",
                        new TransparencyDisclosure(TransparencyKind.AI_INTERACTION, "bot"))));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(response.getHeader(TransparencyDisclosure.HEADER_NAME)).isNull();
    }
}
