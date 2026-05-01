/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.audit;

import com.iambilotta.spring.aiact.annotation.AiActHighRiskSystem;
import com.iambilotta.spring.aiact.annotation.AiActLog;
import com.iambilotta.spring.aiact.model.AuditEvent;
import com.iambilotta.spring.aiact.model.EventKind;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;

/**
 * AOP advisor that intercepts every invocation of a method (or every method on a class) that
 * carries {@link AiActLog} and writes one Article 12 audit record per call. The aspect runs
 * around the call to capture the latency, hashes input and output via {@link PayloadHasher}
 * and forwards the record to the {@link AuditLogService}.
 */
@Aspect
public class AiActLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(AiActLoggingAspect.class);

    private final AuditLogService auditLog;
    private final PayloadHasher hasher;
    private final UserPseudonymizer userPseudonymizer;
    private final MetadataSanitizer metadataSanitizer;

    public AiActLoggingAspect(AuditLogService auditLog, PayloadHasher hasher,
                              UserPseudonymizer userPseudonymizer,
                              MetadataSanitizer metadataSanitizer) {
        this.auditLog = auditLog;
        this.hasher = hasher;
        this.userPseudonymizer = userPseudonymizer;
        this.metadataSanitizer = metadataSanitizer;
    }

    @Around("@annotation(com.iambilotta.spring.aiact.annotation.AiActLog) "
            + "|| @within(com.iambilotta.spring.aiact.annotation.AiActLog)")
    public Object aroundLogged(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        AiActLog annotation = resolveAnnotation(method);
        if (annotation == null) {
            return pjp.proceed();
        }
        SystemContext ctx = resolveSystemContext(pjp);
        long started = System.nanoTime();
        Object result = null;
        Throwable failure = null;
        try {
            result = pjp.proceed();
            return result;
        } catch (Throwable t) {
            failure = t;
            throw t;
        } finally {
            long latencyMs = (System.nanoTime() - started) / 1_000_000L;
            try {
                AuditEvent.Builder b = AuditEvent.builder()
                        .eventKind(failure == null ? EventKind.INVOCATION : EventKind.ANOMALY)
                        .systemId(ctx.systemId())
                        .systemVersion(ctx.version())
                        .operation(operationName(annotation, method))
                        .modelId(modelId(annotation, ctx))
                        .latencyMs(latencyMs)
                        .userIdPseudonymized(userPseudonymizer.resolve())
                        .hashAlgorithm(annotation.hashStrategy().algorithm());

                if (annotation.captureInput()) {
                    Object[] args = pjp.getArgs();
                    Object payload = args.length == 1 ? args[0] : args;
                    b.inputHash(hasher.hash(payload, annotation.hashStrategy()));
                }
                if (annotation.captureOutput() && failure == null) {
                    b.outputHash(hasher.hash(result, annotation.hashStrategy()));
                }
                if (failure != null) {
                    b.metadata(metadataSanitizer.describeException(failure));
                }
                auditLog.append(b.build());
            } catch (RuntimeException auditError) {
                log.error("spring-aiact: failed to append audit event for {}", method, auditError);
            }
        }
    }

    private AiActLog resolveAnnotation(Method method) {
        AiActLog onMethod = AnnotationUtils.findAnnotation(method, AiActLog.class);
        if (onMethod != null) return onMethod;
        return AnnotationUtils.findAnnotation(method.getDeclaringClass(), AiActLog.class);
    }

    private SystemContext resolveSystemContext(ProceedingJoinPoint pjp) {
        Class<?> targetClass = pjp.getTarget() != null ? pjp.getTarget().getClass()
                : ((MethodSignature) pjp.getSignature()).getMethod().getDeclaringClass();
        AiActHighRiskSystem hr = AnnotationUtils.findAnnotation(targetClass, AiActHighRiskSystem.class);
        if (hr == null) {
            return new SystemContext(targetClass.getSimpleName(), targetClass.getSimpleName(),
                    "unknown", "unknown");
        }
        String id = hr.id().isEmpty() ? targetClass.getSimpleName() : hr.id();
        String version = hr.version().isEmpty() ? "unknown" : hr.version();
        return new SystemContext(id, hr.name(), version, hr.provider());
    }

    private String operationName(AiActLog ann, Method method) {
        if (!ann.operation().isEmpty()) return ann.operation();
        return method.getDeclaringClass().getSimpleName() + "." + method.getName();
    }

    private String modelId(AiActLog ann, SystemContext ctx) {
        if (!ann.modelId().isEmpty()) return ann.modelId();
        return ctx.systemId() + "@" + ctx.version();
    }
}
