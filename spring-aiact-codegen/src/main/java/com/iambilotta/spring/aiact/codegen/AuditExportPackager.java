/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.codegen;

import com.iambilotta.spring.aiact.audit.HmacChain;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Packages the conformity assessment artifacts (technical file, declaration of conformity,
 * dataset datasheets, audit log slice) into a ZIP file ready for submission to a notified body
 * under AI Act Annex VII.
 * <p>
 * A {@code MANIFEST.txt} at the root of the archive lists every file together with its SHA-256.
 * The manifest itself is signed by the configured HMAC chain so the recipient can verify the
 * archive has not been tampered with after generation.
 */
public final class AuditExportPackager {

    private final HmacChain hmac;

    public AuditExportPackager(HmacChain hmac) {
        this.hmac = hmac;
    }

    public void packageTo(OutputStream out, Map<String, byte[]> entries) throws IOException {
        Map<String, byte[]> ordered = new LinkedHashMap<>(entries);
        try (ZipOutputStream zos = new ZipOutputStream(out)) {
            StringBuilder manifest = new StringBuilder(256);
            manifest.append("# spring-aiact audit export\n");
            manifest.append("# format: <sha256-hex> <relative-path>\n");
            for (Map.Entry<String, byte[]> e : ordered.entrySet()) {
                String name = e.getKey();
                byte[] content = e.getValue();
                ZipEntry zip = new ZipEntry(name);
                zos.putNextEntry(zip);
                zos.write(content);
                zos.closeEntry();
                manifest.append(HmacChain.sha256Hex(new String(content, StandardCharsets.UTF_8)))
                        .append("  ").append(name).append('\n');
            }
            String manifestText = manifest.toString();
            String mac = hmac.chain(HmacChain.CHAIN_SEED, manifestText);
            byte[] manifestBytes = manifestText.getBytes(StandardCharsets.UTF_8);
            ZipEntry manifestEntry = new ZipEntry("MANIFEST.txt");
            zos.putNextEntry(manifestEntry);
            zos.write(manifestBytes);
            zos.closeEntry();
            ZipEntry sigEntry = new ZipEntry("MANIFEST.hmac");
            zos.putNextEntry(sigEntry);
            zos.write(mac.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
    }
}
