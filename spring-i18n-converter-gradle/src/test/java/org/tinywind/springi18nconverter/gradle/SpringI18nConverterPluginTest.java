package org.tinywind.springi18nconverter.gradle;

import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringI18nConverterPluginTest {
    private static final String NAMESPACE = "https://raw.githubusercontent.com/tinywind/SPRING-I18N-CONVERTER/master/spring-i18n-converter/src/main/resources/xsd/spring-i18n-converter-0.1.xsd";

    @TempDir
    Path projectDir;

    @BeforeEach
    void setUp() throws IOException {
        write("settings.gradle", "rootProject.name = 'consumer'\n");
        write("build.gradle", """
                plugins {
                    id 'org.tinywind.spring-i18n-converter'
                }
                springI18n {
                    configurationFile = layout.projectDirectory.file('config/converter.xml')
                }
                """);
    }

    @ParameterizedTest
    @CsvSource({"JavascriptConverter,js", "ExcelConverter,xlsx"})
    void convertsUnicodePropertiesAndBackUsingProjectRelativePaths(String converter, String extension) throws IOException {
        write("번역 자료/messages_ko.properties", "greeting=안녕하세요\n");
        write("번역 자료/messages.properties", "greeting=Hello\n");
        configure(converter, "번역 자료", "생성 파일", false);

        BuildResult encoded = runner().build();

        assertEquals(TaskOutcome.SUCCESS, encoded.task(":convertI18n").getOutcome());
        Path converted = projectDir.resolve("생성 파일/ko." + extension);
        if (extension.equals("js")) {
            assertTrue(Files.readString(converted).contains("i18n['ko']['greeting']='안녕하세요';"));
        } else {
            try (var workbook = WorkbookFactory.create(converted.toFile())) {
                var row = workbook.getSheetAt(0).getRow(0);
                assertEquals("greeting", row.getCell(0).getStringCellValue());
                assertEquals("ko", row.getCell(1).getStringCellValue());
                assertEquals("안녕하세요", row.getCell(2).getStringCellValue());
            }
        }
        assertTrue(Files.isRegularFile(projectDir.resolve("생성 파일/default." + extension)));

        configure(converter, "생성 파일", "복원 자료", true);
        BuildResult decoded = runner().build();

        assertEquals(TaskOutcome.SUCCESS, decoded.task(":convertI18n").getOutcome());
        assertEquals("안녕하세요", properties("복원 자료/messages_ko.properties").getProperty("greeting"));
        assertEquals("Hello", properties("복원 자료/messages.properties").getProperty("greeting"));
    }

    @Test
    void usesDefaultConfigurationFileAndResolvesEmptyTargetToProjectDirectory() throws IOException {
        write("build.gradle", "plugins { id 'org.tinywind.spring-i18n-converter' }\n");
        write("messages/messages_ko.properties", "greeting=안녕하세요\n");
        write("configuration.xml", configuration("JavascriptConverter", "messages", "", false));

        BuildResult result = runner().build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":convertI18n").getOutcome());
        assertTrue(Files.readString(projectDir.resolve("ko.js")).contains("안녕하세요"));
    }

    @Test
    void resolvesRelativePathsWithinTheAppliedSubproject() throws IOException {
        write("settings.gradle", "rootProject.name = 'consumer'\ninclude 'translations'\n");
        write("translations/build.gradle", Files.readString(projectDir.resolve("build.gradle")));
        write("build.gradle", "");
        write("translations/input/messages_en.properties", "greeting=Hello from subproject\n");
        write("translations/config/converter.xml", configuration("JavascriptConverter", "input", "output", false));

        BuildResult result = runner(":translations:convertI18n").build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":translations:convertI18n").getOutcome());
        assertTrue(Files.readString(projectDir.resolve("translations/output/en.js")).contains("Hello from subproject"));
        assertFalse(Files.exists(projectDir.resolve("output")));
    }

    @Test
    void rerunsConversionAfterMessagePropertiesChange() throws IOException {
        write("input/messages_en.properties", "greeting=First\n");
        configure("JavascriptConverter", "input", "output", false);
        runner().build();
        write("input/messages_en.properties", "greeting=Second\n");

        BuildResult result = runner().build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":convertI18n").getOutcome());
        String javascript = Files.readString(projectDir.resolve("output/en.js"));
        assertTrue(javascript.contains("Second"));
        assertFalse(javascript.contains("First"));
    }

    @Test
    void failsTheBuildForMalformedXml() throws IOException {
        write("config/converter.xml", "<configuration><sources>");

        BuildResult result = runner().buildAndFail();

        assertEquals(TaskOutcome.FAILED, result.task(":convertI18n").getOutcome());
        assertTrue(result.getOutput().contains("converter.xml"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "<sourceDir/>", "<sourceDir> </sourceDir>"})
    void failsTheBuildForInvalidConfiguration(String source) throws IOException {
        write("config/converter.xml", "<configuration xmlns=\"" + NAMESPACE + "\"><sources><source>" + source + "</source></sources></configuration>");

        BuildResult result = runner().buildAndFail();

        assertEquals(TaskOutcome.FAILED, result.task(":convertI18n").getOutcome());
        assertTrue(result.getOutput().contains("converter.xml"));
    }

    @Test
    void propagatesConverterFailuresToTheBuild() throws IOException {
        write("input/messages_en.properties", "greeting=Hello\n");
        configure("MissingConverter", "input", "output", false);

        BuildResult result = runner().buildAndFail();

        assertEquals(TaskOutcome.FAILED, result.task(":convertI18n").getOutcome());
        assertTrue(result.getOutput().contains("MissingConverter"));
    }

    @ParameterizedTest
    @CsvSource({
            "JavascriptConverter,js,java.lang.IllegalArgumentException",
            "ExcelConverter,xlsx,java.io.IOException"
    })
    void propagatesInvalidReverseConversionInput(String converter, String extension, String exceptionType) throws IOException {
        write("input/ko." + extension, "This is not valid JavaScript or an Excel workbook.\n");
        configure(converter, "input", "output", true);

        BuildResult result = runner().buildAndFail();

        assertEquals(TaskOutcome.FAILED, result.task(":convertI18n").getOutcome());
        assertTrue(result.getOutput().contains("Caused by: " + exceptionType), result.getOutput());
    }

    @Test
    void propagatesOutputWriteFailuresToTheBuild() throws IOException {
        write("input/messages_en.properties", "greeting=Hello\n");
        write("blocked", "This file cannot be an output directory.\n");
        configure("JavascriptConverter", "input", "blocked", false);

        BuildResult result = runner().buildAndFail();

        assertEquals(TaskOutcome.FAILED, result.task(":convertI18n").getOutcome());
        assertTrue(result.getOutput().contains("blocked"));
    }

    @Test
    void skipsWithoutRequiringAConfigurationFile() throws IOException {
        write("build.gradle", """
                plugins {
                    id 'org.tinywind.spring-i18n-converter'
                }
                springI18n {
                    skip = true
                }
                """);

        BuildResult result = runner().build();

        assertEquals(TaskOutcome.SKIPPED, result.task(":convertI18n").getOutcome());
    }

    private void configure(String converter, String source, String target, boolean reverse) throws IOException {
        write("config/converter.xml", configuration(converter, source, target, reverse));
    }

    private String configuration(String converter, String source, String target, boolean reverse) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <configuration xmlns="%s">
                    <sources>
                        <source>
                            <converter>org.tinywind.springi18nconverter.converter.%s</converter>
                            <toMessageProperties>%s</toMessageProperties>
                            <sourceDir>%s</sourceDir>
                            <sourceEncoding>UTF-8</sourceEncoding>
                            <describeByNative>true</describeByNative>
                            <targetDir>%s</targetDir>
                            <targetEncoding>UTF-8</targetEncoding>
                            <overwrite>true</overwrite>
                        </source>
                    </sources>
                </configuration>
                """.formatted(NAMESPACE, converter, reverse, source, target);
    }

    private GradleRunner runner() {
        return runner("convertI18n");
    }

    private GradleRunner runner(String task) {
        return GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withTestKitDir(Path.of(System.getProperty("gradle.testkit.dir")).toFile())
                .withPluginClasspath()
                .withArguments(task, "--stacktrace", "--offline");
    }

    private Properties properties(String path) throws IOException {
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(projectDir.resolve(path), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }

    private void write(String path, String content) throws IOException {
        Path file = projectDir.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }
}
