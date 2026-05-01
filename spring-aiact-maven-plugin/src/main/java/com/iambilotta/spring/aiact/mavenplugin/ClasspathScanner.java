/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.spring.aiact.mavenplugin;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Walks {@code target/classes} of a Maven project and loads every class file via a child
 * classloader. The plugin then filters the loaded classes by annotation. The classloader is
 * isolated, so the build classpath is not polluted.
 */
final class ClasspathScanner implements AutoCloseable {

    private final Path classesRoot;
    private final URLClassLoader classLoader;

    ClasspathScanner(Path classesRoot, List<Path> dependencies) throws IOException {
        this.classesRoot = classesRoot;
        List<URL> urls = new ArrayList<>();
        urls.add(classesRoot.toUri().toURL());
        for (Path dep : dependencies) urls.add(dep.toUri().toURL());
        this.classLoader = new URLClassLoader(urls.toArray(new URL[0]),
                ClasspathScanner.class.getClassLoader());
    }

    @Override
    public void close() throws IOException {
        classLoader.close();
    }

    /** Convenience for {@code try-with-resources} when the call site already has a builder chain. */
    ClasspathScanner asCloseable() {
        return this;
    }

    List<Class<?>> loadAllClasses() throws IOException {
        if (!Files.isDirectory(classesRoot)) return List.of();
        List<Class<?>> classes = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(classesRoot)) {
            walk
                .filter(p -> p.toString().endsWith(".class"))
                .filter(p -> !p.getFileName().toString().equals("module-info.class"))
                .forEach(p -> {
                    String name = toFqcn(classesRoot, p);
                    try {
                        classes.add(Class.forName(name, false, classLoader));
                    } catch (Throwable ignored) {
                        // class with missing transitive dep, skip silently
                    }
                });
        }
        return classes;
    }

    URLClassLoader classLoader() {
        return classLoader;
    }

    private static String toFqcn(Path root, Path classFile) {
        String relative = root.relativize(classFile).toString();
        if (relative.endsWith(".class")) {
            relative = relative.substring(0, relative.length() - ".class".length());
        }
        return relative.replace(java.io.File.separatorChar, '.');
    }
}
