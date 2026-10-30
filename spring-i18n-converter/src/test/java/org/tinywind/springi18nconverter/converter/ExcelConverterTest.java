package org.tinywind.springi18nconverter.converter;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.tinywind.springi18nconverter.ProjectTempDirFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExcelConverterTest {
    @TempDir(factory = ProjectTempDirFactory.class)
    public Path temporary;

    @Test
    public void roundTripsPropertiesThroughXlsx() throws Exception {
        File workbook = new File(temporary.toFile(), "messages.xlsx");
        ExcelConverter converter = new ExcelConverter();
        converter.encode("greeting=Hello\nanswer=42\n", "en", workbook, "UTF-8", true);
        converter.decode(workbook, temporary.toFile().getPath(), "UTF-8", true);
        assertEquals("greeting=Hello\nanswer=42\n", new String(Files.readAllBytes(
                new File(temporary.toFile(), "messages_en.properties").toPath()), StandardCharsets.UTF_8));
    }

    @Test
    public void decodesLegacyXlsWorkbooks() throws Exception {
        File file = new File(temporary.toFile(), "messages.xls");
        try (HSSFWorkbook workbook = new HSSFWorkbook(); FileOutputStream output = new FileOutputStream(file)) {
            Row row = workbook.createSheet().createRow(0);
            row.createCell(0).setCellValue("greeting");
            row.createCell(1).setCellValue("en");
            row.createCell(2).setCellValue("Hello");
            workbook.write(output);
        }
        new ExcelConverter().decode(file, temporary.toFile().getPath(), "UTF-8", true);
        assertEquals("greeting=Hello\n", new String(Files.readAllBytes(
                new File(temporary.toFile(), "messages_en.properties").toPath()), StandardCharsets.UTF_8));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void roundTripsLiteralBackslashPaths(boolean describeByNative) throws Exception {
        Path properties = temporary.resolve("messages.properties");
        Files.writeString(properties, "greeting=Hello\npath=C:\\\\temp\\\\files\nquote=It's ready\n");
        Path file = temporary.resolve("messages.xlsx");
        ExcelConverter converter = new ExcelConverter();
        converter.encode(Files.readString(properties), "default", file.toFile(), "UTF-8", describeByNative);
        try (Workbook workbook = WorkbookFactory.create(file.toFile())) {
            assertEquals(describeByNative ? "C:\\temp\\files" : "C:\\\\temp\\\\files",
                    workbook.getSheetAt(0).getRow(1).getCell(2).getStringCellValue());
        }
        Path restored = Files.createDirectory(temporary.resolve("restored"));
        converter.decode(file.toFile(), restored.toString(), "UTF-8", describeByNative);
        Properties messages = new Properties();
        try (Reader reader = Files.newBufferedReader(restored.resolve("messages.properties"), StandardCharsets.UTF_8)) {
            messages.load(reader);
        }
        assertEquals("Hello", messages.getProperty("greeting"));
        assertEquals("C:\\temp\\files", messages.getProperty("path"));
        assertEquals("It's ready", messages.getProperty("quote"));
    }
}
