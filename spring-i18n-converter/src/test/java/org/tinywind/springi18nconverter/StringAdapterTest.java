package org.tinywind.springi18nconverter;

import org.junit.jupiter.api.Test;
import org.tinywind.springi18nconverter.jaxb.Configuration;

import jakarta.xml.bind.JAXBContext;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class StringAdapterTest {
    @Test
    public void preservesTrimmingAndNullValues() {
        StringAdapter adapter = new StringAdapter();
        assertEquals("messages", adapter.unmarshal("  messages  "));
        assertEquals("messages", adapter.marshal("  messages  "));
        assertNull(adapter.unmarshal(null));
        assertNull(adapter.marshal(null));
    }

    @Test
    public void unmarshalsConfigurationUsingTheStringAdapter() throws Exception {
        String xml = "<configuration xmlns=\"https://raw.githubusercontent.com/tinywind/SPRING-I18N-CONVERTER/"
                + "master/spring-i18n-converter/src/main/resources/xsd/spring-i18n-converter-0.1.xsd\">"
                + "<sources><source><sourceDir> messages </sourceDir></source></sources></configuration>";
        Configuration configuration = (Configuration) JAXBContext.newInstance(Configuration.class)
                .createUnmarshaller().unmarshal(new StringReader(xml));
        assertEquals("messages", configuration.getSources().get(0).getSourceDir());
    }

    @Test
    public void resolvesSystemPropertiesInConfigurationPaths() {
        String key = "springi18nconverter.test.directory";
        String previous = System.setProperty(key, "/var/messages");
        try {
            assertEquals("/var/messages/en.properties", new StringAdapter().unmarshal(
                    "  ${springi18nconverter.test.directory}/en.properties  "));
        } finally {
            if (previous == null) System.clearProperty(key);
            else System.setProperty(key, previous);
        }
    }
}
