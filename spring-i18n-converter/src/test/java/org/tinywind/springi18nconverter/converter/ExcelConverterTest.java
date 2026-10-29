package org.tinywind.springi18nconverter.converter;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;

public class ExcelConverterTest {
    @Rule
    public TemporaryFolder temporary = new TemporaryFolder(new File("target"));

    @Test
    public void roundTripsPropertiesThroughXlsx() throws Exception {
        File workbook = new File(temporary.getRoot(), "messages.xlsx");
        ExcelConverter converter = new ExcelConverter();
        converter.encode("greeting=Hello\nanswer=42\n", "en", workbook, "UTF-8", true);
        converter.decode(workbook, temporary.getRoot().getPath(), "UTF-8", true);
        assertEquals("greeting=Hello\nanswer=42\n", new String(Files.readAllBytes(
                new File(temporary.getRoot(), "messages_en.properties").toPath()), StandardCharsets.UTF_8));
    }

    @Test
    public void decodesLegacyXlsWorkbooks() throws Exception {
        File file = new File(temporary.getRoot(), "messages.xls");
        try (HSSFWorkbook workbook = new HSSFWorkbook(); FileOutputStream output = new FileOutputStream(file)) {
            Row row = workbook.createSheet().createRow(0);
            row.createCell(0).setCellValue("greeting");
            row.createCell(1).setCellValue("en");
            row.createCell(2).setCellValue("Hello");
            workbook.write(output);
        }
        new ExcelConverter().decode(file, temporary.getRoot().getPath(), "UTF-8", true);
        assertEquals("greeting=Hello\n", new String(Files.readAllBytes(
                new File(temporary.getRoot(), "messages_en.properties").toPath()), StandardCharsets.UTF_8));
    }
}
