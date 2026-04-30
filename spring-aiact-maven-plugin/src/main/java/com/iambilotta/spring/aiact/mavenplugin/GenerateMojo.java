/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.mavenplugin;

import com.iambilotta.spring.aiact.codegen.datasheet.DatasetDatasheetRenderer;
import com.iambilotta.spring.aiact.codegen.discovery.AnnotationModelCollector;
import com.iambilotta.spring.aiact.codegen.markdown.TechnicalFileMarkdownRenderer;
import com.iambilotta.spring.aiact.codegen.model.TechnicalFileModel;
import com.iambilotta.spring.aiact.codegen.pdf.DeclarationOfConformity;
import com.iambilotta.spring.aiact.codegen.pdf.DeclarationOfConformityPdfGenerator;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;

/**
 * Generates the Annex IV Markdown technical file, the Article 47 DoC PDF placeholder and one
 * datasheet per declared dataset for every {@code @AiActHighRiskSystem} class found in
 * {@code target/classes}. Outputs go to {@code target/generated-docs/} by default.
 */
@Mojo(name = "generate",
        defaultPhase = LifecyclePhase.PROCESS_CLASSES,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        threadSafe = true)
public class GenerateMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(property = "aiact.outputDirectory",
            defaultValue = "${project.build.directory}/generated-docs")
    private String outputDirectory;

    /** Optional git revision string injected into the technical file. */
    @Parameter(property = "aiact.gitSha", defaultValue = "${git.commit.id.abbrev:-}")
    private String gitSha;

    /** Free architecture description forwarded to section 2 of the technical file. */
    @Parameter(property = "aiact.architecture", defaultValue = "")
    private String architectureDescription;

    @Override
    public void execute() throws MojoExecutionException {
        Path classesDir = Paths.get(project.getBuild().getOutputDirectory());
        if (!Files.isDirectory(classesDir)) {
            getLog().info("spring-aiact: no classes to scan, skipping generation.");
            return;
        }
        List<Path> deps = MavenClasspath.compileClasspath(project);
        Path outDir = Paths.get(outputDirectory);
        try {
            Files.createDirectories(outDir);
            try (ClasspathScanner scanner = new ClasspathScanner(classesDir, deps)) {
                List<Class<?>> classes = scanner.loadAllClasses();
                AnnotationModelCollector.CollectionContext ctx =
                        new AnnotationModelCollector.CollectionContext(
                                project.getVersion(),
                                gitSha == null || gitSha.isBlank() || "-".equals(gitSha) ? "" : gitSha,
                                Instant.now(),
                                architectureDescription == null ? "" : architectureDescription,
                                List.of(), List.of(), List.of(), List.of(), List.of()
                        );
                List<TechnicalFileModel> models = new AnnotationModelCollector().collect(classes, ctx);
                if (models.isEmpty()) {
                    getLog().info("spring-aiact: no @AiActHighRiskSystem classes found, nothing to generate.");
                    return;
                }
                TechnicalFileMarkdownRenderer mdRenderer = new TechnicalFileMarkdownRenderer();
                DeclarationOfConformityPdfGenerator pdfGen = new DeclarationOfConformityPdfGenerator();
                DatasetDatasheetRenderer datasheetRenderer = new DatasetDatasheetRenderer();
                for (TechnicalFileModel m : models) {
                    writeTechnicalFile(outDir, m, mdRenderer);
                    writeDeclarationOfConformity(outDir, m, pdfGen);
                    writeDatasheets(outDir, m, datasheetRenderer);
                }
                getLog().info("spring-aiact: generated " + models.size() + " technical file(s) under " + outDir);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to generate AI Act artifacts", e);
        }
    }

    private void writeTechnicalFile(Path outDir, TechnicalFileModel m, TechnicalFileMarkdownRenderer r)
            throws IOException {
        Path file = outDir.resolve(m.systemId() + "-technical-file.md");
        Files.writeString(file, r.render(m), StandardCharsets.UTF_8);
        getLog().info("spring-aiact: wrote " + file.getFileName());
    }

    private void writeDeclarationOfConformity(Path outDir, TechnicalFileModel m,
                                              DeclarationOfConformityPdfGenerator g) throws IOException {
        Path file = outDir.resolve(m.systemId() + "-doc.pdf");
        DeclarationOfConformity doc = new DeclarationOfConformity(
                m.systemName(), m.systemId(), m.provider(), "[provider postal address]",
                m.version(), m.category().reference(),
                m.harmonizedStandards().stream().map(s -> s.reference() + ", " + s.title()).toList(),
                "",
                "[signer name and role]",
                "[place]",
                m.generatedAt(),
                List.of()
        );
        try (OutputStream os = Files.newOutputStream(file)) {
            g.render(doc, os);
        }
        getLog().info("spring-aiact: wrote " + file.getFileName());
    }

    private void writeDatasheets(Path outDir, TechnicalFileModel m, DatasetDatasheetRenderer r)
            throws IOException {
        for (TechnicalFileModel.DatasetEntry d : m.datasets()) {
            Path file = outDir.resolve(m.systemId() + "-dataset-" + d.id() + ".md");
            Files.writeString(file, r.render(d, m.systemId()), StandardCharsets.UTF_8);
            getLog().info("spring-aiact: wrote " + file.getFileName());
        }
    }
}
