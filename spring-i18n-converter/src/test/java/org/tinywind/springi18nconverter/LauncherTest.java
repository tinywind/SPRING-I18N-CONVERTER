package org.tinywind.springi18nconverter;

import jakarta.xml.bind.JAXBContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.tinywind.springi18nconverter.converter.ExcelConverter;
import org.tinywind.springi18nconverter.jaxb.Configuration;
import org.tinywind.springi18nconverter.jaxb.Source;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LauncherTest {
    private static final String NAMESPACE = "https://raw.githubusercontent.com/tinywind/SPRING-I18N-CONVERTER/"
            + "master/spring-i18n-converter/src/main/resources/xsd/spring-i18n-converter-0.1.xsd";

    @TempDir(factory = ProjectTempDirFactory.class)
    public Path temporary;

    @Test
    public void exposesConfigurationLoadingToBuildAdapters() {
        assertDoesNotThrow(() -> Launcher.class.getMethod("load", InputStream.class));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void loadsExistingXmlWithDefaultsAndTrimmedUnicodePaths(boolean namespaced) throws Exception {
        String root = namespaced ? "<configuration xmlns=\"" + NAMESPACE + "\">" : "<configuration>";
        Configuration configuration = load(root + "<sources><source><sourceDir> messages/한국어 </sourceDir>"
                + "</source></sources></configuration>");
        Source source = configuration.getSources().get(0);
        assertEquals("messages/한국어", source.getSourceDir());
        assertEquals("org.tinywind.springi18nconverter.converter.JavascriptConverter", source.getConverter());
        assertEquals("UTF-8", source.getSourceEncoding());
        assertEquals("UTF-8", source.getTargetEncoding());
        assertEquals("", source.getTargetDir());
        assertEquals("", source.getExcludes());
        assertEquals(Boolean.TRUE, source.isOverwrite());
        assertEquals(Boolean.FALSE, source.isDescribeByNative());
        assertEquals(Boolean.FALSE, source.isToMessageProperties());
    }

    @Test
    public void roundTripsTheExistingFlattenedListAndFluentModel() throws Exception {
        Source first = new Source().withSourceDir("messages").withTargetDir("output");
        Source second = new Source().withSourceDir("other");
        Configuration configuration = new Configuration().withSources(first, second);
        assertSame(first, configuration.getSources().get(0));
        List<Source> sources = new ArrayList<>(List.of(first));
        configuration.setSources(sources);
        assertSame(sources, configuration.getSources());
        assertSame(configuration, configuration.withSources(new ArrayList<>(List.of(second))));
        assertEquals(List.of(second), configuration.getSources());
        assertSame(configuration, configuration.withSources((java.util.Collection<Source>) List.of(first)));
        StringWriter output = new StringWriter();
        JAXBContext.newInstance(Configuration.class).createMarshaller().marshal(configuration, output);
        Configuration restored = load(output.toString());
        assertEquals(2, restored.getSources().size());
        assertEquals("other", restored.getSources().get(0).getSourceDir());
        assertEquals("output", restored.getSources().get(1).getTargetDir());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "<configuration><sources><source/></sources></configuration>",
            "<configuration><sources><source><sourceDir>messages</sourceDir><overwrite>invalid</overwrite></source></sources></configuration>",
            "<configuration><sources><source><sourceDir>messages</sourceDir><unknown>value</unknown></source></sources></configuration>"
    })
    public void rejectsInvalidXmlRatherThanIgnoringSchemaErrors(String xml) {
        assertThrows(IOException.class, () -> load(xml));
    }

    @Test
    public void rejectsConfigurationsWithoutSources() {
        assertThrows(IllegalArgumentException.class, () -> Launcher.generate(new Configuration()));
    }

    @Test
    public void rejectsConfigurationsWithoutSourceDirectories() {
        assertThrows(IllegalArgumentException.class, () -> Launcher.generate(new Configuration().withSources(new Source())));
    }

    @Test
    public void reportsMissingSourceDirectories() {
        Source source = new Source().withSourceDir(temporary.resolve("missing").toString());
        assertThrows(IOException.class, () -> Launcher.generate(new Configuration().withSources(source)));
    }

    @Test
    public void reportsBlockedTargetDirectories() throws Exception {
        Path input = Files.createDirectory(temporary.resolve("input"));
        Path target = Files.writeString(temporary.resolve("target"), "occupied");
        Source source = new Source().withSourceDir(input.toString()).withTargetDir(target.toString());
        assertThrows(IOException.class, () -> Launcher.generate(new Configuration().withSources(source)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing.Converter", "java.lang.String"})
    public void reportsInvalidConverters(String converter) throws Exception {
        Path input = Files.createDirectory(temporary.resolve("input"));
        Source source = new Source().withSourceDir(input.toString())
                .withTargetDir(temporary.resolve("output").toString()).withConverter(converter);
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> Launcher.generate(new Configuration().withSources(source)));
        assertNotNull(failure.getCause());
    }

    @Test
    public void reportsSourceReadFailures() throws Exception {
        Path input = Files.createDirectory(temporary.resolve("input"));
        Files.createDirectory(input.resolve("messages.properties"));
        Source source = new Source().withSourceDir(input.toString()).withTargetDir(temporary.resolve("output").toString());
        assertThrows(IOException.class, () -> Launcher.generate(new Configuration().withSources(source)));
    }

    @Test
    public void reportsDecoderFailures() throws Exception {
        Path input = Files.createDirectory(temporary.resolve("input"));
        Files.writeString(input.resolve("en.xlsx"), "not a workbook");
        Source source = new Source().withSourceDir(input.toString()).withTargetDir(temporary.resolve("output").toString())
                .withConverter(ExcelConverter.class.getName()).withToMessageProperties(true);
        assertThrows(IOException.class, () -> Launcher.generate(new Configuration().withSources(source)));
    }

    @Test
    public void createsMissingTargetsWithoutOverwritingExistingTargets() throws Exception {
        Path input = Files.createDirectory(temporary.resolve("input"));
        Path output = Files.createDirectory(temporary.resolve("output"));
        Files.writeString(input.resolve("messages.properties"), "greeting=Hello\n");
        Files.writeString(input.resolve("messages_ko.properties"), "greeting=안녕하세요\n");
        Files.writeString(output.resolve("ko.js"), "preserved");
        Source source = new Source().withSourceDir(input.toString()).withTargetDir(output.toString()).withOverwrite(false);
        Launcher.generate(new Configuration().withSources(source));
        assertEquals("preserved", Files.readString(output.resolve("ko.js")));
        assertTrue(Files.readString(output.resolve("default.js")).contains("['greeting']='Hello'"));
    }

    @Test
    public void restoresExplicitlyNullDefaultsBeforeConverting() throws Exception {
        Path input = Files.createDirectory(temporary.resolve("input"));
        Path output = temporary.resolve("output");
        Files.writeString(input.resolve("messages.properties"), "greeting=Hello\n");
        Source source = new Source().withSourceDir(input.toString()).withTargetDir(output.toString())
                .withConverter(null).withSourceEncoding(null).withTargetEncoding(null)
                .withOverwrite(null).withDescribeByNative(null).withToMessageProperties(null).withExcludes(null);
        Launcher.generate(new Configuration().withSources(source));
        assertTrue(Files.readString(output.resolve("default.js")).contains("['greeting']='Hello'"));
    }

    private Configuration load(String xml) throws IOException {
        return Launcher.load(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }
}
