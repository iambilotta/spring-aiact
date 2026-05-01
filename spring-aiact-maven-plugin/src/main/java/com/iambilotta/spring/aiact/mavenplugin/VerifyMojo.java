/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.mavenplugin;

import com.iambilotta.spring.aiact.annotation.AiActDataset;
import com.iambilotta.spring.aiact.annotation.AiActHighRiskSystem;
import com.iambilotta.spring.aiact.annotation.AiActIntendedPurpose;
import com.iambilotta.spring.aiact.annotation.AiActOversight;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Fails the build when classes annotated {@code @AiActHighRiskSystem} are missing the
 * companion annotations the technical file requires:
 *
 * <ul>
 *   <li>{@code @AiActIntendedPurpose} for Article 13 instructions for use,</li>
 *   <li>{@code @AiActOversight} for Article 14 oversight,</li>
 *   <li>at least one {@code @AiActDataset} declaration for Article 10 data governance.</li>
 * </ul>
 *
 * Bind the goal to {@code verify} (or earlier) in your build to catch missing claims at CI time.
 */
@Mojo(name = "verify",
        defaultPhase = LifecyclePhase.VERIFY,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        threadSafe = true)
public class VerifyMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /** When {@code true}, missing annotations only log a warning. Default fails the build. */
    @Parameter(property = "aiact.verify.warningOnly", defaultValue = "false")
    private boolean warningOnly;

    /** When {@code true}, datasets are not enforced (use for placeholder builds). */
    @Parameter(property = "aiact.verify.datasetOptional", defaultValue = "false")
    private boolean datasetOptional;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Path classesDir = Paths.get(project.getBuild().getOutputDirectory());
        List<Path> deps = MavenClasspath.compileClasspath(project);
        try (ClasspathScanner scanner = new ClasspathScanner(classesDir, deps).asCloseable()) {
            List<Class<?>> classes = scanner.loadAllClasses();
            List<String> violations = new ArrayList<>();
            for (Class<?> type : classes) {
                AiActHighRiskSystem hr = type.getAnnotation(AiActHighRiskSystem.class);
                if (hr == null) continue;
                if (type.getAnnotation(AiActIntendedPurpose.class) == null) {
                    violations.add(type.getName() + ": missing @AiActIntendedPurpose (Article 13).");
                }
                if (type.getAnnotation(AiActOversight.class) == null) {
                    violations.add(type.getName() + ": missing @AiActOversight (Article 14).");
                }
                if (!datasetOptional
                        && type.getAnnotationsByType(AiActDataset.class).length == 0
                        && !classpathHasDataset(classes, hr)) {
                    violations.add(type.getName()
                            + ": missing @AiActDataset (Article 10) on this class or in the same package.");
                }
            }
            report(violations);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to scan classpath", e);
        }
    }

    private boolean classpathHasDataset(List<Class<?>> classes, AiActHighRiskSystem hr) {
        for (Class<?> c : classes) {
            if (c.getAnnotationsByType(AiActDataset.class).length > 0) return true;
        }
        return false;
    }

    private void report(List<String> violations) throws MojoFailureException {
        if (violations.isEmpty()) {
            getLog().info("spring-aiact: all @AiActHighRiskSystem classes carry the required companion annotations.");
            return;
        }
        for (String v : violations) {
            if (warningOnly) {
                getLog().warn("spring-aiact: " + v);
            } else {
                getLog().error("spring-aiact: " + v);
            }
        }
        if (!warningOnly) {
            throw new MojoFailureException(
                    "spring-aiact verify failed: " + violations.size()
                            + " missing annotation(s). See errors above.");
        }
    }
}
