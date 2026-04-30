/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.audit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HmacChainTest {

    @Test
    void firstRecordSeedsFromAllZeroes() {
        HmacChain chain = HmacChain.fromUtf8("test-secret");
        String mac = chain.chain(null, "{\"event_id\":\"a\"}");
        assertThat(mac).hasSize(64);
    }

    @Test
    void identicalInputsAndKeyProduceIdenticalOutput() {
        HmacChain a = HmacChain.fromUtf8("k1");
        HmacChain b = HmacChain.fromUtf8("k1");
        String payload = "{\"event_id\":\"abc\"}";
        assertThat(a.chain(HmacChain.CHAIN_SEED, payload))
                .isEqualTo(b.chain(HmacChain.CHAIN_SEED, payload));
    }

    @Test
    void differentKeysProduceDifferentOutput() {
        HmacChain a = HmacChain.fromUtf8("k1");
        HmacChain b = HmacChain.fromUtf8("k2");
        String payload = "x";
        assertThat(a.chain(HmacChain.CHAIN_SEED, payload))
                .isNotEqualTo(b.chain(HmacChain.CHAIN_SEED, payload));
    }

    @Test
    void chainPropagatesPrevHmac() {
        HmacChain chain = HmacChain.fromUtf8("k");
        String first = chain.chain(HmacChain.CHAIN_SEED, "{\"i\":1}");
        String second = chain.chain(first, "{\"i\":2}");
        String alt = chain.chain(HmacChain.CHAIN_SEED, "{\"i\":2}");
        assertThat(second).isNotEqualTo(alt);
    }

    @Test
    void verifyDetectsTampering() {
        HmacChain chain = HmacChain.fromUtf8("k");
        String payload = "{\"i\":1}";
        String mac = chain.chain(HmacChain.CHAIN_SEED, payload);
        assertThat(chain.verify(HmacChain.CHAIN_SEED, payload, mac)).isTrue();
        assertThat(chain.verify(HmacChain.CHAIN_SEED, "{\"i\":2}", mac)).isFalse();
        assertThat(chain.verify(HmacChain.CHAIN_SEED, payload, "deadbeef")).isFalse();
    }

    @Test
    void emptyKeyRejected() {
        assertThatThrownBy(() -> HmacChain.fromUtf8(""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
