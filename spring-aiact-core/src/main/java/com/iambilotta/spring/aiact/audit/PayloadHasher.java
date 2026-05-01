/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iambilotta.spring.aiact.annotation.HashStrategy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Hashes the input arguments and the return value of an annotated method according to the
 * configured {@link HashStrategy}. The serialization step uses Jackson with stable property
 * ordering so the same logical payload yields the same hash across processes.
 */
public final class PayloadHasher {

    private final ObjectMapper mapper;

    public PayloadHasher(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String hash(Object value, HashStrategy strategy) {
        if (strategy == HashStrategy.NONE || value == null) {
            return value == null ? "null" : "opaque:" + System.identityHashCode(value);
        }
        String serialized = serialize(value);
        try {
            MessageDigest md = MessageDigest.getInstance(strategy.algorithm());
            byte[] digest = md.digest(serialized.getBytes(StandardCharsets.UTF_8));
            return strategy.algorithm().toLowerCase().replace("-", "") + ":" + toHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "Hash algorithm " + strategy.algorithm() + " not available on this JVM", e);
        }
    }

    private String serialize(Object value) {
        if (value instanceof CharSequence cs) {
            return cs.toString();
        }
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return value.getClass().getName() + "@" + System.identityHashCode(value);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
