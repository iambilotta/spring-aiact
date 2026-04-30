/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a dataset used to train, validate or test a high-risk AI system. The build-time
 * generator emits one datasheet per dataset, following the Datasheets for Datasets pattern
 * (Gebru et al.) restricted to the AI Act Article 10 disclosure requirements.
 * <p>
 * The annotation can be placed on the DAO, repository or loader class that owns the data,
 * or repeated on a configuration holder. Bias declarations should not be padded: if no bias
 * has been formally measured, leave the array empty rather than write filler text.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
public @interface AiActDataset {

    /**
     * Stable dataset id, kebab-case recommended. Used as the file name of the generated datasheet.
     */
    String id();

    /**
     * Human-readable name.
     */
    String name();

    /**
     * Phase the dataset is used for: {@code training}, {@code validation}, {@code test},
     * {@code production-inference}. The value is forwarded verbatim to the datasheet,
     * keeping the annotation free of opinion.
     */
    String phase();

    /**
     * Source URI or a free description of the provenance, used in the datasheet provenance section.
     */
    String source();

    /**
     * Approximate size as a string (for example {@code "12,500 records"} or {@code "2.3 GB"}).
     */
    String size();

    /**
     * License identifier (SPDX preferred when applicable).
     */
    String license();

    /**
     * Documented biases or representational gaps. Empty array when none are formally measured.
     */
    String[] biases() default {};

    /**
     * Whether the dataset contains personal data within the meaning of GDPR Article 4(1).
     */
    boolean personalData() default false;
}
