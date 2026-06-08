/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.autoconfigure.web;

import com.iambilotta.spring.aiact.transparency.TransparencyDisclosure;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Article 50 transparency filter. For every request whose path matches an AI surface declared with
 * {@code @AiActTransparency}, stamps a machine-detectable disclosure header on the response so a
 * natural person (or their agentic client) is told the interaction or content is AI-generated.
 *
 * <p>The header is written before the chain proceeds: even an error or empty body carries the
 * disclosure. The human-readable message rides on a companion header for UIs that render it.
 */
public class AiActTransparencyFilter extends OncePerRequestFilter {

    /** Binds a request path pattern (Ant-style) to the disclosure to stamp. */
    public record Mapping(String pathPattern, TransparencyDisclosure disclosure) {
    }

    private static final String MESSAGE_HEADER = "X-AiAct-Transparency-Message";

    private final List<Mapping> mappings;
    private final AntPathMatcher matcher = new AntPathMatcher();

    public AiActTransparencyFilter(List<Mapping> mappings) {
        this.mappings = List.copyOf(mappings);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        for (Mapping m : mappings) {
            if (matcher.match(m.pathPattern(), path)) {
                TransparencyDisclosure d = m.disclosure();
                response.setHeader(d.headerName(), d.headerValue());
                response.setHeader(MESSAGE_HEADER, d.message());
                break;
            }
        }
        filterChain.doFilter(request, response);
    }
}
