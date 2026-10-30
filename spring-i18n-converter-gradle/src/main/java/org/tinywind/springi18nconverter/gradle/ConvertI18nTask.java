package org.tinywind.springi18nconverter.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.UntrackedTask;
import org.tinywind.springi18nconverter.Launcher;
import org.tinywind.springi18nconverter.jaxb.Configuration;
import org.tinywind.springi18nconverter.jaxb.Source;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Runs the converters selected by an XML configuration.
 */
@UntrackedTask(because = "The XML configuration selects source files and output locations dynamically.")
public abstract class ConvertI18nTask extends DefaultTask {
    /**
     * Configures the task's group and skip condition.
     */
    public ConvertI18nTask() {
        setGroup("i18n");
        setDescription("Converts Spring message properties to JavaScript or Excel and back.");
        onlyIf("i18n conversion is enabled", task -> !getSkip().get());
    }

    /**
     * Returns the XML converter configuration file.
     *
     * @return the configuration file
     */
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getConfigurationFile();

    /**
     * Returns whether conversion is skipped.
     *
     * @return the skip flag
     */
    @Input
    public abstract Property<Boolean> getSkip();

    /**
     * Returns the consumer project directory used to resolve paths in the XML file.
     *
     * @return the consumer project directory
     */
    @Internal
    public abstract DirectoryProperty getProjectDirectory();

    /**
     * Loads the XML configuration and converts its sources.
     */
    @TaskAction
    public void convert() {
        File configurationFile = getConfigurationFile().get().getAsFile();
        try (InputStream input = Files.newInputStream(configurationFile.toPath())) {
            Configuration configuration = Launcher.load(input);
            for (Source source : configuration.getSources()) {
                String sourceDirectory = source.getSourceDir();
                if (sourceDirectory != null && !sourceDirectory.isBlank()) {
                    source.setSourceDir(resolvePath(sourceDirectory));
                }
                source.setTargetDir(resolvePath(source.getTargetDir() == null ? "" : source.getTargetDir()));
            }
            Launcher.generate(configuration);
        } catch (IOException | RuntimeException exception) {
            throw new GradleException("Could not convert i18n configuration '" + configurationFile + "'.", exception);
        }
    }

    private String resolvePath(String value) {
        Path path = Path.of(value);
        if (!path.isAbsolute()) {
            path = getProjectDirectory().get().getAsFile().toPath().resolve(path);
        }
        return path.normalize().toString();
    }
}
