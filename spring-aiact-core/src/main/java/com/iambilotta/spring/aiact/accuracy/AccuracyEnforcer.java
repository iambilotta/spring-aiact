/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.accuracy;

import com.iambilotta.spring.aiact.annotation.RiskMetric;

/**
 * Article 15 accuracy/robustness enforcement (REQ-AIACT-006). The library cannot measure a model's
 * accuracy itself; the adopter's evaluation harness supplies a measured value and this enforcer
 * compares it against the threshold declared on {@code @AiActAccuracyMetric}, turning the
 * previously declarative annotation into an actual gate.
 *
 * <p>The threshold is a small inequality grammar (encoded as a string to allow ranges on the
 * annotation): {@code >=x}, {@code >x}, {@code <=x}, {@code <x}, {@code ==x}, or a bare number
 * {@code x} meaning {@code >=x} (the common "at least this accurate" case). For upper-bound metrics
 * such as a false-positive rate, declare {@code <=x}.
 */
public final class AccuracyEnforcer {

    private AccuracyEnforcer() {
    }

    private enum Op {
        GE(">="), GT(">"), LE("<="), LT("<"), EQ("==");

        final String token;

        Op(String token) {
            this.token = token;
        }
    }

    /** Outcome of comparing a measured value against a declared threshold. */
    public record Result(String threshold, double measured, boolean belowThreshold) {

        /** Human-readable description, used in reports and exception messages. */
        public String describe() {
            return belowThreshold
                    ? "measured " + measured + " violates threshold " + threshold
                    : "measured " + measured + " satisfies threshold " + threshold;
        }
    }

    /**
     * Compare {@code measured} against {@code threshold}. Never throws on a violation: returns a
     * {@link Result} whose {@link Result#belowThreshold()} signals it. Throws only when the
     * threshold string cannot be parsed.
     */
    public static Result evaluate(String threshold, double measured) {
        Parsed p = parse(threshold);
        boolean satisfied = switch (p.op) {
            case GE -> measured >= p.value;
            case GT -> measured > p.value;
            case LE -> measured <= p.value;
            case LT -> measured < p.value;
            case EQ -> measured == p.value;
        };
        return new Result(threshold, measured, !satisfied);
    }

    /**
     * Evaluate and throw {@link AccuracyThresholdViolation} when the metric is below threshold, so a
     * build assertion or eval hook can gate on it. The message names the metric, threshold and
     * measured value.
     */
    public static void enforce(RiskMetric metric, String threshold, double measured) {
        Result r = evaluate(threshold, measured);
        if (r.belowThreshold()) {
            throw new AccuracyThresholdViolation(
                    "Article 15 metric " + metric + " below threshold: " + r.describe());
        }
    }

    private record Parsed(Op op, double value) {
    }

    private static Parsed parse(String threshold) {
        if (threshold == null || threshold.isBlank()) {
            throw new IllegalArgumentException("Accuracy threshold must not be blank");
        }
        String t = threshold.trim();
        // Order matters: two-char operators before their one-char prefixes.
        for (Op op : new Op[]{Op.GE, Op.LE, Op.EQ, Op.GT, Op.LT}) {
            if (t.startsWith(op.token)) {
                return new Parsed(op, parseNumber(t.substring(op.token.length()), threshold));
            }
        }
        // Bare number = "at least this".
        return new Parsed(Op.GE, parseNumber(t, threshold));
    }

    private static double parseNumber(String raw, String original) {
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Cannot parse accuracy threshold '" + original + "'", e);
        }
    }
}
