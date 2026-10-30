package org.tinywind.springi18nconverter;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringAdapter extends XmlAdapter<String, String> {
    private static final Pattern PROPERTY = Pattern.compile("\\$\\{(.*?)\\}");

    @Override
    public String unmarshal(String value) {
        if (value == null) return null;
        String result = value.trim();
        Matcher matcher = PROPERTY.matcher(result);
        while (matcher.find()) {
            result = result.replace(matcher.group(), System.getProperty(matcher.group(1), matcher.group()));
        }
        return result;
    }

    @Override
    public String marshal(String value) {
        return value == null ? null : value.trim();
    }
}
