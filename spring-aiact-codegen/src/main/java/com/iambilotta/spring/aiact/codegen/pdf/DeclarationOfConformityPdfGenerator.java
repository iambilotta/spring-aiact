/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.codegen.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;

/**
 * Renders an {@link DeclarationOfConformity} into an Article 47 PDF, with a signature placeholder.
 * The PDF is intentionally simple: black-and-white, A4, two pages maximum, no images. The intent is
 * to ship a document a notary or compliance officer can sign on paper or counter-sign digitally,
 * not to ship a marketing brochure.
 */
public final class DeclarationOfConformityPdfGenerator {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMMM yyyy").withZone(ZoneOffset.UTC);

    public void render(DeclarationOfConformity doc, OutputStream out) {
        Document pdf = new Document();
        try {
            PdfWriter.getInstance(pdf, out);
            pdf.open();
            pdf.add(title("EU Declaration of Conformity"));
            pdf.add(subtitle("AI Act Article 47, Annex V"));
            pdf.add(spacer());

            pdf.add(infoTable(doc));
            pdf.add(spacer());

            pdf.add(heading("1. Object of the declaration"));
            pdf.add(body("The provider declares under its sole responsibility that the AI system "
                    + doc.systemName() + " (system id " + safe(doc.systemId()) + ", version "
                    + safe(doc.systemVersion()) + ") complies with the requirements of "
                    + "Regulation (EU) 2024/1689, in particular with Articles 8 to 15 and "
                    + "the relevant provisions of Annex IV."));
            pdf.add(spacer());

            pdf.add(heading("2. Annex III classification"));
            pdf.add(body("The system is classified under " + safe(doc.annexIIIReference()) + "."));
            pdf.add(spacer());

            pdf.add(heading("3. Harmonized standards applied"));
            if (doc.appliedStandards() == null || doc.appliedStandards().isEmpty()) {
                pdf.add(body("No harmonized standards were declared by the provider."));
            } else {
                for (String s : doc.appliedStandards()) {
                    pdf.add(bullet(s));
                }
            }
            pdf.add(spacer());

            pdf.add(heading("4. Notified body"));
            pdf.add(body(doc.notifiedBody() == null || doc.notifiedBody().isBlank()
                    ? "Not applicable, internal control conformity assessment."
                    : doc.notifiedBody()));
            pdf.add(spacer());

            if (doc.additionalDeclarations() != null) {
                for (String s : doc.additionalDeclarations()) {
                    pdf.add(body(s));
                }
                if (!doc.additionalDeclarations().isEmpty()) pdf.add(spacer());
            }

            pdf.add(heading("Signature"));
            String sigLine = doc.signatureLine() == null || doc.signatureLine().isBlank()
                    ? "[signature line, to be filled by the authorized signer]"
                    : doc.signatureLine();
            pdf.add(body(sigLine));
            pdf.add(spacer());
            pdf.add(body("Place: " + safe(doc.placeOfDeclaration())));
            pdf.add(body("Date: "
                    + (doc.dateOfDeclaration() == null ? "_____________" : DATE_FMT.format(doc.dateOfDeclaration()))));
            pdf.add(spacer());
            pdf.add(body("________________________________________"));
            pdf.add(body("Authorized signer (name, role, handwritten signature or digital seal)"));
        } catch (RuntimeException e) {
            throw e;
        } finally {
            try {
                if (pdf.isOpen()) pdf.close();
            } catch (RuntimeException ignored) { /* close can throw if writer disposed */ }
        }
    }

    private Paragraph title(String s) {
        Paragraph p = new Paragraph(s, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK));
        p.setAlignment(Element.ALIGN_CENTER);
        return p;
    }

    private Paragraph subtitle(String s) {
        Paragraph p = new Paragraph(s, FontFactory.getFont(FontFactory.HELVETICA, 11, Color.GRAY));
        p.setAlignment(Element.ALIGN_CENTER);
        return p;
    }

    private Paragraph heading(String s) {
        Paragraph p = new Paragraph(s, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK));
        p.setSpacingBefore(6);
        return p;
    }

    private Paragraph body(String s) {
        return new Paragraph(s, FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK));
    }

    private Paragraph bullet(String s) {
        return new Paragraph("- " + s, FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK));
    }

    private Paragraph spacer() {
        return new Paragraph(" ");
    }

    private PdfPTable infoTable(DeclarationOfConformity doc) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        addRow(table, "Provider", safe(doc.providerName()));
        addRow(table, "Provider address", safe(doc.providerAddress()));
        addRow(table, "System name", safe(doc.systemName()));
        addRow(table, "System id", safe(doc.systemId()));
        addRow(table, "System version", safe(doc.systemVersion()));
        addRow(table, "Annex III reference", safe(doc.annexIIIReference()));
        return table;
    }

    private void addRow(PdfPTable table, String label, String value) {
        Font label_font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
        Font value_font = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
        PdfPCell labelCell = new PdfPCell(new Phrase(label, label_font));
        PdfPCell valueCell = new PdfPCell(new Phrase(value, value_font));
        labelCell.setPadding(4);
        valueCell.setPadding(4);
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private static String safe(String s) {
        return s == null || s.isBlank() ? "[unspecified]" : s;
    }
}
