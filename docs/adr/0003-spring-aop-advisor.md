# ADR-0003: Spring AOP advisor on @AiActLog instead of bytecode rewriting

- **Status:** accepted
- **Date:** 2026-04-29
- **Deciders:** Francesco Bilotta

## Context

Article 12 requires the provider to log every inference of a high-risk system. The library has to intercept method calls annotated `@AiActLog` and append a record before / around / after the call without the developer writing boilerplate.

Three options:

- **Spring AOP advisor**: a `Pointcut` matching `@AiActLog`, an `Around` advice that wraps the call.
- **AspectJ load-time weaving**: bytecode rewriting at classload, no proxy involved.
- **Java agent / instrumentation**: rewrite the method bytecode directly.

## Decision

A Spring AOP advisor (`AiActLoggingAspect`) registered as a starter bean. The pointcut matches `@AiActLog`. An `Around` advice serializes input + output via `PayloadHasher`, calls `AuditLogService.append`, and returns the original return value.

## Consequences

- Adopters do nothing beyond adding the starter and the annotation. No build configuration, no agent flag, no AspectJ weaver.
- The advisor uses the standard Spring proxy mechanism: CGLIB for class-based beans, JDK dynamic proxy for interface-based ones. Self-invocation (`this.scoreInside(...)` from another method of the same class) bypasses the proxy and the advice does not fire. This is the long-standing Spring AOP limitation. We document it in the README operational notes; in practice the public scoring entry point on a `@Service` is always called from outside.
- Performance overhead is one method indirection plus the bookkeeping of `PayloadHasher` and `AuditLogService.append`. JMH benchmarks pin the cost.
- The library does not pull in AspectJ runtime, keeping the dependency surface minimal.

## Alternatives considered

**AspectJ load-time weaving.** Solves self-invocation but requires the adopter to launch the JVM with the AspectJ weaver agent. Operationally invasive for a one-line "I just imported the starter" flow.

**Java agent with Byte Buddy.** Same operational cost as AspectJ LTW, with the added cost of authoring and shipping a custom agent.

**Compile-time annotation processor that rewrites bytecode.** Possible (Lombok-style) but breaks reproducibility expectations: adopters auditing the compiled classes would see code that does not match `src/main/java`. Bad signal for a compliance library.
