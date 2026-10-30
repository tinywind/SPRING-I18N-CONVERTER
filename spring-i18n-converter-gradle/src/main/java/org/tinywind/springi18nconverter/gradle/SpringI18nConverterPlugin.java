package org.tinywind.springi18nconverter.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Registers the springI18n extension and convertI18n task.
 */
public final class SpringI18nConverterPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        SpringI18nExtension extension = project.getExtensions().create("springI18n", SpringI18nExtension.class);
        extension.getConfigurationFile().convention(project.getLayout().getProjectDirectory().file("configuration.xml"));
        extension.getSkip().convention(false);

        project.getTasks().register("convertI18n", ConvertI18nTask.class, task -> {
            task.getConfigurationFile().set(extension.getConfigurationFile());
            task.getSkip().set(extension.getSkip());
            task.getProjectDirectory().set(project.getLayout().getProjectDirectory());
        });
    }
}
