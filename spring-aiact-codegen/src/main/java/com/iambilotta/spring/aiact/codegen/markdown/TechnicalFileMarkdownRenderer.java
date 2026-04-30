/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.codegen.markdown;

import com.iambilotta.spring.aiact.codegen.model.TechnicalFileModel;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Renders a {@link TechnicalFileModel} into the Annex IV Markdown technical file.
 * <p>
 * The output follows the nine-section Annex IV structure verbatim. The renderer never invents
 * content: when a section is empty it prints the heading followed by the literal placeholder
 * {@code _Not declared in code. Add the corresponding annotation to populate this section._}
 * so the reviewer can see the gap rather than a hallucinated paragraph.
 */
public final class TechnicalFileMarkdownRenderer {

    private static final String NOT_DECLARED =
            "_Not declared in code. Add the corresponding annotation to populate this section._";

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT;

    public String render(TechnicalFileModel model) {
        StringBuilder sb = new StringBuilder(8 * 1024);
        title(sb, model);
        section1GeneralDescription(sb, model);
        section2Design(sb, model);
        section3Dataset(sb, model);
        section4TrainingMethodology(sb, model);
        section5Validation(sb, model);
        section6Accuracy(sb, model);
        section7Cybersecurity(sb, model);
        section8LifecycleChanges(sb, model);
        section9HarmonizedStandards(sb, model);
        footer(sb, model);
        return sb.toString();
    }

    private void title(StringBuilder sb, TechnicalFileModel m) {
        sb.append("# Technical File, AI Act Annex IV\n\n");
        sb.append("**System:** ").append(m.systemName()).append("  \n");
        sb.append("**System id:** `").append(m.systemId()).append("`  \n");
        sb.append("**Provider:** ").append(m.provider()).append("  \n");
        sb.append("**Version:** ").append(m.version()).append("  \n");
        if (m.gitSha() != null && !m.gitSha().isBlank()) {
            sb.append("**Git revision:** `").append(m.gitSha()).append("`  \n");
        }
        sb.append("**Annex III category:** ").append(m.category().reference())
                .append(" (").append(m.category().name()).append(")");
        if (m.annexSubpoint() != null && !m.annexSubpoint().isBlank()) {
            sb.append(", sub-point ").append(m.annexSubpoint());
        }
        sb.append("  \n");
        sb.append("**Generated at:** ").append(ISO.format(m.generatedAt())).append("\n\n");
        sb.append("> This file is generated from `@AiAct*` annotations by the spring-aiact build-time\n")
          .append("> generator. It is evidence-as-code for an AI Act conformity assessment dossier,\n")
          .append("> not a certification. The provider remains responsible for the assessment under\n")
          .append("> Articles 16 and 47.\n\n");
    }

    private void section1GeneralDescription(StringBuilder sb, TechnicalFileModel m) {
        sb.append("## 1. General description\n\n");
        sb.append("**Intended purpose (summary).** ").append(safe(m.intendedPurpose())).append("\n\n");
        TechnicalFileModel.IntendedPurposeDetails d = m.intendedPurposeDetails();
        if (d == null) {
            sb.append(NOT_DECLARED).append("\n\n");
            return;
        }
        sb.append("**Deployment context.** ").append(safe(d.deploymentContext())).append("\n\n");
        bullets(sb, "Intended user categories", d.users());
        bullets(sb, "Foreseeable misuse", d.foreseeableMisuse());
        bullets(sb, "Geographies", d.geographies());
        bullets(sb, "Languages", d.languages());
    }

    private void section2Design(StringBuilder sb, TechnicalFileModel m) {
        sb.append("## 2. Design and development\n\n");
        if (m.architectureDescription() != null && !m.architectureDescription().isBlank()) {
            sb.append(m.architectureDescription()).append("\n\n");
        } else {
            sb.append(NOT_DECLARED).append("\n\n");
        }
        if (!m.loggedOperations().isEmpty()) {
            sb.append("### Logged operations (Article 12 attribution)\n\n");
            sb.append("| Class | Method | Operation | Model id | Hash | Input | Output |\n");
            sb.append("|---|---|---|---|---|---|---|\n");
            for (TechnicalFileModel.LoggedOperation op : m.loggedOperations()) {
                sb.append("| `").append(op.declaringClass()).append("` ");
                sb.append("| `").append(op.method()).append("` ");
                sb.append("| ").append(safe(op.operation())).append(" ");
                sb.append("| `").append(safe(op.modelId())).append("` ");
                sb.append("| ").append(safe(op.hashAlgorithm())).append(" ");
                sb.append("| ").append(op.captureInput() ? "yes" : "no").append(" ");
                sb.append("| ").append(op.captureOutput() ? "yes" : "no").append(" |\n");
            }
            sb.append('\n');
        }
        if (m.oversight() != null) {
            sb.append("### Article 14 human oversight\n\n");
            sb.append("- **Level:** ").append(m.oversight().level().name()).append('\n');
            sb.append("- **Override role:** ").append(safe(m.oversight().overrideRole())).append('\n');
            sb.append("- **Description:** ").append(safe(m.oversight().description())).append("\n\n");
        }
    }

    private void section3Dataset(StringBuilder sb, TechnicalFileModel m) {
        sb.append("## 3. Datasets and data governance\n\n");
        if (m.datasets().isEmpty()) {
            sb.append(NOT_DECLARED).append("\n\n");
            return;
        }
        sb.append("| Id | Name | Phase | Source | Size | License | Personal data | Documented biases |\n");
        sb.append("|---|---|---|---|---|---|---|---|\n");
        for (TechnicalFileModel.DatasetEntry d : m.datasets()) {
            sb.append("| `").append(d.id()).append("` ");
            sb.append("| ").append(safe(d.name())).append(" ");
            sb.append("| ").append(safe(d.phase())).append(" ");
            sb.append("| ").append(safe(d.source())).append(" ");
            sb.append("| ").append(safe(d.size())).append(" ");
            sb.append("| ").append(safe(d.license())).append(" ");
            sb.append("| ").append(d.personalData() ? "yes" : "no").append(" ");
            sb.append("| ").append(d.biases().isEmpty() ? "_none formally measured_"
                    : String.join("; ", d.biases())).append(" |\n");
        }
        sb.append("\n_Per-dataset datasheets are emitted as separate files._\n\n");
    }

    private void section4TrainingMethodology(StringBuilder sb, TechnicalFileModel m) {
        sb.append("## 4. Training methodology\n\n");
        if (m.trainingMethodologies().isEmpty()) {
            sb.append(NOT_DECLARED).append("\n\n");
        } else {
            for (String s : m.trainingMethodologies()) sb.append("- ").append(s).append('\n');
            sb.append('\n');
        }
    }

    private void section5Validation(StringBuilder sb, TechnicalFileModel m) {
        sb.append("## 5. Validation and testing\n\n");
        if (m.validationStrategies().isEmpty()) {
            sb.append(NOT_DECLARED).append("\n\n");
        } else {
            for (String s : m.validationStrategies()) sb.append("- ").append(s).append('\n');
            sb.append('\n');
        }
    }

    private void section6Accuracy(StringBuilder sb, TechnicalFileModel m) {
        sb.append("## 6. Accuracy, robustness and Article 15 metrics\n\n");
        if (m.accuracyMetrics().isEmpty()) {
            sb.append(NOT_DECLARED).append("\n\n");
            return;
        }
        sb.append("| Metric | Threshold | Population | Harness |\n");
        sb.append("|---|---|---|---|\n");
        for (TechnicalFileModel.AccuracyMetricEntry e : m.accuracyMetrics()) {
            String label = e.metric().name();
            if (e.metric().name().equals("CUSTOM") && e.name() != null && !e.name().isBlank()) {
                label = "CUSTOM(" + e.name() + ")";
            }
            sb.append("| ").append(label).append(' ');
            sb.append("| `").append(safe(e.threshold())).append("` ");
            sb.append("| ").append(safe(e.population())).append(' ');
            sb.append("| ").append(safe(e.harness())).append(" |\n");
        }
        sb.append('\n');
    }

    private void section7Cybersecurity(StringBuilder sb, TechnicalFileModel m) {
        sb.append("## 7. Cybersecurity\n\n");
        if (m.cybersecurityMeasures().isEmpty()) {
            sb.append(NOT_DECLARED).append("\n\n");
        } else {
            for (String s : m.cybersecurityMeasures()) sb.append("- ").append(s).append('\n');
            sb.append('\n');
        }
    }

    private void section8LifecycleChanges(StringBuilder sb, TechnicalFileModel m) {
        sb.append("## 8. Lifecycle changes and post-market monitoring\n\n");
        if (m.lifecycleChanges().isEmpty()) {
            sb.append(NOT_DECLARED).append("\n\n");
            return;
        }
        sb.append("| When | Version | Summary |\n");
        sb.append("|---|---|---|\n");
        for (TechnicalFileModel.LifecycleChange c : m.lifecycleChanges()) {
            sb.append("| ").append(ISO.format(c.when())).append(' ');
            sb.append("| ").append(safe(c.version())).append(' ');
            sb.append("| ").append(safe(c.summary())).append(" |\n");
        }
        sb.append('\n');
    }

    private void section9HarmonizedStandards(StringBuilder sb, TechnicalFileModel m) {
        sb.append("## 9. Harmonized standards and references\n\n");
        if (m.harmonizedStandards().isEmpty()) {
            sb.append(NOT_DECLARED).append("\n\n");
            return;
        }
        sb.append("| Reference | Title |\n|---|---|\n");
        for (TechnicalFileModel.HarmonizedStandard s : m.harmonizedStandards()) {
            sb.append("| ").append(safe(s.reference())).append(" ");
            sb.append("| ").append(safe(s.title())).append(" |\n");
        }
        sb.append('\n');
    }

    private void footer(StringBuilder sb, TechnicalFileModel m) {
        sb.append("---\n\n");
        sb.append("_Generated by spring-aiact. The Annex IV technical file is part of the\n");
        sb.append("conformity assessment dossier under AI Act Article 11. Submission to a\n");
        sb.append("notified body and the resulting certification remain the responsibility of\n");
        sb.append("the provider._\n");
    }

    private static String safe(String s) {
        return s == null || s.isBlank() ? "_unspecified_" : s.replace("|", "\\|");
    }

    private static void bullets(StringBuilder sb, String label, List<String> items) {
        if (items == null || items.isEmpty()) return;
        sb.append("**").append(label).append(":**\n\n");
        for (String s : items) sb.append("- ").append(s).append('\n');
        sb.append('\n');
    }
}
