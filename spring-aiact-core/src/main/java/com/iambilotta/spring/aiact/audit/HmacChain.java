/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.audit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * HMAC-SHA256 chain helper used to authenticate the Article 12 audit log. Each record's HMAC is
 * computed over {@code prev_hmac || record_payload} so that any tampering with a record breaks
 * the chain at every subsequent record. The chain seed is the all-zero 32-byte string for the
 * first record.
 * <p>
 * The HMAC key is provided by configuration; callers are responsible for not committing it
 * to source control. When the key is rotated, the previous chain segment must be exported and
 * archived before reseeding.
 */
public final class HmacChain {

    public static final String CHAIN_SEED = "0".repeat(64);
    private static final String ALGO = "HmacSHA256";

    private final byte[] key;

    public HmacChain(byte[] key) {
        if (key == null || key.length == 0) {
            throw new IllegalArgumentException("HMAC key must not be empty");
        }
        this.key = key.clone();
    }

    public static HmacChain fromUtf8(String secret) {
        if (secret == null || secret.isEmpty()) {
            throw new IllegalArgumentException("HMAC secret must not be empty");
        }
        return new HmacChain(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String chain(String prevHmacHex, String payload) {
        try {
            Mac mac = Mac.getInstance(ALGO);
            mac.init(new SecretKeySpec(key, ALGO));
            String safePrev = prevHmacHex == null || prevHmacHex.isBlank() ? CHAIN_SEED : prevHmacHex;
            mac.update(safePrev.getBytes(StandardCharsets.UTF_8));
            mac.update(payload.getBytes(StandardCharsets.UTF_8));
            return toHex(mac.doFinal());
        } catch (NoSuchAlgorithmException | java.security.InvalidKeyException e) {
            throw new IllegalStateException("HMAC computation failed", e);
        }
    }

    public boolean verify(String prevHmacHex, String payload, String expectedHmacHex) {
        String actual = chain(prevHmacHex, payload);
        return constantTimeEquals(actual, expectedHmacHex);
    }

    public static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return toHex(md.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
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
