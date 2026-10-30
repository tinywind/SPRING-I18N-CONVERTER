package org.tinywind.springi18nconverter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.tinywind.springi18nconverter.converter.AbstractConverter;
import org.tinywind.springi18nconverter.converter.ExcelConverter;
import org.tinywind.springi18nconverter.converter.JavascriptConverter;
import org.tinywind.springi18nconverter.jaxb.Configuration;
import org.tinywind.springi18nconverter.jaxb.Source;

import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConversionIntegrationTest {
    @TempDir(factory = ProjectTempDirFactory.class)
    public Path temporary;

    @ParameterizedTest
    @ValueSource(classes = {JavascriptConverter.class, ExcelConverter.class})
    public void reconstructedMessagesWorkWithSpringFrameworkMessageSource(Class<? extends AbstractConverter> converter) throws Exception {
        Path input = Files.createDirectory(temporary.resolve("input"));
        Path encoded = temporary.resolve("encoded");
        Path restored = temporary.resolve("restored");
        Files.writeString(input.resolve("messages.properties"), "greeting=Hello, {0}\nfallback=Base value\n");
        Files.writeString(input.resolve("messages_ko_KR.properties"), "greeting=안녕하세요, {0}\n");
        Launcher.generate(new Configuration().withSources(new Source().withSourceDir(input.toString())
                .withTargetDir(encoded.toString()).withConverter(converter.getName()).withDescribeByNative(true)));
        Launcher.generate(new Configuration().withSources(new Source().withSourceDir(encoded.toString())
                .withTargetDir(restored.toString()).withConverter(converter.getName())
                .withToMessageProperties(true).withDescribeByNative(true)));

        ReloadableResourceBundleMessageSource messages = new ReloadableResourceBundleMessageSource();
        messages.setBasename(restored.resolve("messages").toUri().toString());
        messages.setDefaultEncoding("UTF-8");
        messages.setFallbackToSystemLocale(false);
        assertEquals("안녕하세요, Mina", messages.getMessage("greeting", new Object[]{"Mina"}, Locale.KOREA));
        assertEquals("Hello, Mina", messages.getMessage("greeting", new Object[]{"Mina"}, Locale.GERMANY));
        assertEquals("Base value", messages.getMessage("fallback", null, Locale.KOREA));
    }

    @Test
    public void javascriptRoundTripHonorsSeparateSourceAndTargetEncodings() throws Exception {
        Path input = Files.createDirectory(temporary.resolve("input"));
        Path encoded = temporary.resolve("encoded");
        Path restored = temporary.resolve("restored");
        Files.writeString(input.resolve("messages_fr.properties"), "greeting=Café\n", Charset.forName("UTF-16"));
        Launcher.generate(new Configuration().withSources(new Source().withSourceDir(input.toString())
                .withSourceEncoding("UTF-16").withTargetDir(encoded.toString()).withTargetEncoding("ISO-8859-1")));
        Launcher.generate(new Configuration().withSources(new Source().withSourceDir(encoded.toString())
                .withSourceEncoding("ISO-8859-1").withTargetDir(restored.toString()).withTargetEncoding("UTF-16")
                .withToMessageProperties(true)));
        Properties messages = new Properties();
        try (Reader reader = Files.newBufferedReader(restored.resolve("messages_fr.properties"), Charset.forName("UTF-16"))) {
            messages.load(reader);
        }
        assertEquals("Café", messages.getProperty("greeting"));
    }

    @ParameterizedTest
    @ValueSource(classes = {JavascriptConverter.class, ExcelConverter.class})
    public void reverseConversionCreatesMissingTargetsAndPreservesExistingTargets(Class<? extends AbstractConverter> converter) throws Exception {
        Path input = Files.createDirectory(temporary.resolve("input"));
        Path encoded = temporary.resolve("encoded");
        Path restored = Files.createDirectory(temporary.resolve("restored"));
        Files.writeString(input.resolve("messages.properties"), "greeting=Hello\n");
        Files.writeString(input.resolve("messages_ko.properties"), "greeting=안녕하세요\n");
        Launcher.generate(new Configuration().withSources(new Source().withSourceDir(input.toString())
                .withTargetDir(encoded.toString()).withConverter(converter.getName())));
        Files.writeString(restored.resolve("messages_ko.properties"), "preserved");
        Launcher.generate(new Configuration().withSources(new Source().withSourceDir(encoded.toString())
                .withTargetDir(restored.toString()).withConverter(converter.getName())
                .withToMessageProperties(true).withOverwrite(false)));
        assertEquals("preserved", Files.readString(restored.resolve("messages_ko.properties")));
        assertEquals("greeting=Hello\n", Files.readString(restored.resolve("messages.properties")));
    }
}
