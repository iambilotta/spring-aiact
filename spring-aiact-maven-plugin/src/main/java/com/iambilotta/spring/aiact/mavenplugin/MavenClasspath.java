/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.mavenplugin;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves the compile + runtime dependency jars for a Maven project, used to construct an
 * isolated {@link ClasspathScanner} classloader.
 */
final class MavenClasspath {

    private MavenClasspath() {
    }

    static List<Path> compileClasspath(MavenProject project) {
        List<Path> jars = new ArrayList<>();
        for (Artifact art : project.getArtifacts()) {
            if (art.getFile() != null) {
                jars.add(art.getFile().toPath());
            }
        }
        return jars;
    }
}
