package org.tinywind.springi18nconverter.gradle;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;

/**
 * Configures XML-based conversion of Spring message properties.
 */
public abstract class SpringI18nExtension {
    /**
     * Returns the XML configuration file, defaulting to the project's configuration.xml.
     *
     * @return the converter configuration file
     */
    public abstract RegularFileProperty getConfigurationFile();

    /**
     * Returns whether conversion is skipped, defaulting to false.
     *
     * @return the skip flag
     */
    public abstract Property<Boolean> getSkip();
}
