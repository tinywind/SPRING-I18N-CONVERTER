package org.tinywind.springi18nconverter.converter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.tinywind.springi18nconverter.ProjectTempDirFactory;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;

import javax.script.ScriptEngine;

import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class JavascriptConverterTest {
    @TempDir(factory = ProjectTempDirFactory.class)
    public Path temporary;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void roundTripsUnicodeQuotesAndBackslashes(boolean describeByNative) throws Exception {
        Path script = temporary.resolve("ko.js");
        JavascriptConverter converter = new JavascriptConverter();
        converter.encode("greeting=\\uC548\\uB155\\uD558\\uC138\\uC694\n"
                        + "quote=It's \"ready\"\npath=C:\\\\messages\\\\new\n",
                "ko", script.toFile(), "UTF-8", describeByNative);
        ScriptEngine engine = new NashornScriptEngineFactory().getScriptEngine();
        try (Reader reader = Files.newBufferedReader(script, StandardCharsets.UTF_8)) {
            engine.eval(reader);
        }
        assertEquals("안녕하세요", engine.eval("i18n.ko.greeting"));
        assertEquals("C:\\messages\\new", engine.eval("i18n.ko.path"));
        converter.decode(script.toFile(), temporary.toString(), "UTF-8", describeByNative);
        Properties messages = new Properties();
        try (Reader reader = Files.newBufferedReader(temporary.resolve("messages_ko.properties"), StandardCharsets.UTF_8)) {
            messages.load(reader);
        }
        assertEquals("안녕하세요", messages.getProperty("greeting"));
        assertEquals("It's \"ready\"", messages.getProperty("quote"));
        assertEquals("C:\\messages\\new", messages.getProperty("path"));
    }

    @Test
    public void decodesWhenTheContextClassLoaderCannotDiscoverScriptEngines() throws Exception {
        Path script = Files.writeString(temporary.resolve("default.js"), "var i18n={default:{greeting:'Hello'}};");
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(ClassLoader.getPlatformClassLoader());
            new JavascriptConverter().decode(script.toFile(), temporary.toString(), "UTF-8", false);
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
        assertEquals("greeting=Hello\n", Files.readString(temporary.resolve("messages.properties")));
    }

    @Test
    public void reportsInvalidJavascript() throws Exception {
        Path script = Files.writeString(temporary.resolve("default.js"), "var i18n = {;");
        assertThrows(IllegalArgumentException.class,
                () -> new JavascriptConverter().decode(script.toFile(), temporary.toString(), "UTF-8", false));
    }

    @Test
    public void reportsOutputWriteFailures() throws Exception {
        Path script = Files.writeString(temporary.resolve("default.js"), "var i18n={default:{greeting:'Hello'}};");
        Path blocked = Files.writeString(temporary.resolve("blocked"), "occupied");
        assertThrows(UncheckedIOException.class,
                () -> new JavascriptConverter().decode(script.toFile(), blocked.toString(), "UTF-8", false));
    }
}
