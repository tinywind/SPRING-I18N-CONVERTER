package org.tinywind.springi18nconverter.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;
import org.tinywind.springi18nconverter.jaxb.Source;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PluginTest {
    @Test
    void skipDoesNotRequireSources() {
        Plugin plugin = new Plugin();
        plugin.skip = true;

        assertDoesNotThrow(plugin::execute);
    }

    @Test
    void invalidConverterFailsTheMavenGoal() {
        Plugin plugin = new Plugin();
        Source source = new Source();
        source.setConverter("missing.Converter");
        source.setSourceDir("src/main/resources");
        plugin.sources = List.of(source);

        MojoExecutionException failure = assertThrows(MojoExecutionException.class, plugin::execute);
        assertNotNull(failure.getCause());
    }

    @Test
    void missingSourceDirectoryFailsTheMavenGoal() {
        Plugin plugin = new Plugin();
        Source source = new Source();
        source.setSourceDir("src/test/missing-i18n-fixture");
        plugin.sources = List.of(source);

        MojoExecutionException failure = assertThrows(MojoExecutionException.class, plugin::execute);
        assertInstanceOf(IOException.class, failure.getCause());
    }

    @Test
    void missingSourcesFailTheMavenGoal() {
        Plugin plugin = new Plugin();

        MojoExecutionException failure = assertThrows(MojoExecutionException.class, plugin::execute);
        assertNotNull(failure.getCause());
    }
}
