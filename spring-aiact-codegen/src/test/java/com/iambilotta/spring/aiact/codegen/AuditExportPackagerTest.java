/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.codegen;

import com.iambilotta.spring.aiact.audit.HmacChain;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class AuditExportPackagerTest {

    @Test
    void packageContainsAllEntriesPlusSignedManifest() throws Exception {
        HmacChain hmac = HmacChain.fromUtf8("k");
        AuditExportPackager packager = new AuditExportPackager(hmac);
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("technical-file.md", "# Annex IV".getBytes(StandardCharsets.UTF_8));
        entries.put("audit.ndjson", "{\"event_id\":\"x\"}\n".getBytes(StandardCharsets.UTF_8));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        packager.packageTo(baos, entries);

        Map<String, String> contents = readZip(baos.toByteArray());

        assertThat(contents).containsKeys("technical-file.md", "audit.ndjson", "MANIFEST.txt", "MANIFEST.hmac");
        String manifest = contents.get("MANIFEST.txt");
        assertThat(manifest).contains("technical-file.md");
        assertThat(manifest).contains("audit.ndjson");
        // Each entry line is "<sha256> <name>"
        String[] lines = manifest.split("\n");
        long entryLines = java.util.Arrays.stream(lines)
                .filter(l -> !l.startsWith("#") && !l.isBlank())
                .count();
        assertThat(entryLines).isEqualTo(2);
    }

    @Test
    void manifestHmacVerifiesAgainstTheManifestText() throws Exception {
        HmacChain hmac = HmacChain.fromUtf8("k");
        AuditExportPackager packager = new AuditExportPackager(hmac);
        Map<String, byte[]> entries = Map.of(
                "doc.pdf", new byte[]{1, 2, 3},
                "audit.ndjson", "{}\n".getBytes(StandardCharsets.UTF_8));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        packager.packageTo(baos, entries);

        Map<String, String> contents = readZip(baos.toByteArray());
        String manifest = contents.get("MANIFEST.txt");
        String mac = contents.get("MANIFEST.hmac").trim();

        assertThat(hmac.verify(HmacChain.CHAIN_SEED, manifest, mac)).isTrue();
    }

    private Map<String, String> readZip(byte[] bytes) throws Exception {
        Map<String, String> out = new LinkedHashMap<>();
        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                ByteArrayOutputStream b = new ByteArrayOutputStream();
                zin.transferTo(b);
                out.put(e.getName(), b.toString(StandardCharsets.UTF_8));
            }
        }
        return out;
    }
}
