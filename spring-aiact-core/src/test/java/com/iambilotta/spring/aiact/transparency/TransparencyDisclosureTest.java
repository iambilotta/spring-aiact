/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.transparency;

import com.iambilotta.spring.aiact.annotation.AiActTransparency;
import com.iambilotta.spring.aiact.annotation.TransparencyKind;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TransparencyDisclosureTest {

    @AiActTransparency(kind = TransparencyKind.AI_INTERACTION)
    static class Chatbot {
    }

    @AiActTransparency(kind = TransparencyKind.AI_GENERATED_CONTENT,
            disclosure = "This summary was produced by an AI system.")
    static class Summarizer {
    }

    static class NotDeclared {
    }

    @Test
    void resolvesAnInteractionDisclosureFromTheAnnotation() {
        TransparencyDisclosure d = TransparencyDisclosure.forType(Chatbot.class);

        assertThat(d).isNotNull();
        assertThat(d.kind()).isEqualTo(TransparencyKind.AI_INTERACTION);
        // Article 50(1): default disclosure when none is overridden.
        assertThat(d.message()).containsIgnoringCase("AI");
        // Machine-detectable header used by a downstream agent / client.
        assertThat(d.headerName()).isEqualTo("X-AiAct-Transparency");
        assertThat(d.headerValue()).contains("ai-interaction").contains("article=50");
    }

    @Test
    void usesTheDeclaredDisclosureMessageWhenProvided() {
        TransparencyDisclosure d = TransparencyDisclosure.forType(Summarizer.class);

        assertThat(d.kind()).isEqualTo(TransparencyKind.AI_GENERATED_CONTENT);
        assertThat(d.message()).isEqualTo("This summary was produced by an AI system.");
        assertThat(d.headerValue()).contains("ai-generated-content");
    }

    @Test
    void returnsNullWhenTheTypeDoesNotDeclareTransparency() {
        assertThat(TransparencyDisclosure.forType(NotDeclared.class)).isNull();
    }
}
