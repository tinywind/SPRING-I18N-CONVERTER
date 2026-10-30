package org.tinywind.springi18nconverter;

import org.junit.jupiter.api.extension.AnnotatedElementContext;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.io.TempDirFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ProjectTempDirFactory implements TempDirFactory {
    @Override
    public Path createTempDirectory(AnnotatedElementContext elementContext, ExtensionContext extensionContext) throws IOException {
        Path root = Path.of("").toAbsolutePath();
        if (!Files.isDirectory(root.resolve("spring-i18n-converter"))) {
            root = root.getParent();
        }
        Path temporary = Files.createDirectories(root.resolve(".tmp/core-tests"));
        return Files.createTempDirectory(temporary, "test-");
    }
}
