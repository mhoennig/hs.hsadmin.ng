package net.hostsharing.test;

import lombok.experimental.UtilityClass;

import javax.validation.constraints.NotNull;
import java.util.Map;

import static liquibase.repackaged.org.apache.commons.text.StringSubstitutor.replace;
import static org.apache.commons.lang3.StringUtils.stripEnd;

@UtilityClass
public class StringTemplater {

    @SafeVarargs
    public static String indentedMultilineTemplate(final String template, final Map.Entry<String, String>... properties) {
        return stripEnd(replace(template, Map.ofEntries(properties)).indent(4), null);
    }

    public static Map.Entry<String, String> property(final String name, final String value) {
        return Map.entry(name, value);
    }

    public static Map.Entry<String, String> property(final String name, @NotNull final Object value) {
        return Map.entry(name, value.toString());
    }

}
