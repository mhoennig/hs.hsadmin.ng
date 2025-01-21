package net.hostsharing.hsadminng.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.SanitizableData;
import org.springframework.boot.actuate.endpoint.SanitizingFunction;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// HOWTO: exclude sensitive values, like passwords and other secrets, from being show by actuator endpoints:
//  either use: add your custom keys to management.endpoint.additionalKeysToSanitize,
//  or, if you need more heuristics, amend this code down here.
@Component
public class ActuatorSanitizer implements SanitizingFunction {

    private static final String[] REGEX_PARTS = {"*", "$", "^", "+"};

    private static final Set<String> DEFAULT_KEYS_TO_SANITIZE = Set.of(
            "password", "secret", "token", ".*credentials.*", "vcap_services", "^vcap\\.services.*$", "sun.java.command", "^spring[._]application[._]json$"
    );

    private static final Set<String> URI_USERINFO_KEYS = Set.of(
            "uri", "uris", "url", "urls", "address", "addresses"
    );

    private static final Pattern URI_USERINFO_PATTERN = Pattern.compile("^\\[?[A-Za-z][A-Za-z0-9\\+\\.\\-]+://.+:(.*)@.+$");

    private final List<Pattern> keysToSanitize = new ArrayList<>();

    public ActuatorSanitizer(@Value("${management.endpoint.additionalKeysToSanitize:}") final List<String> additionalKeysToSanitize) {
        addKeysToSanitize(DEFAULT_KEYS_TO_SANITIZE);
        addKeysToSanitize(URI_USERINFO_KEYS);
        addKeysToSanitize(additionalKeysToSanitize);
    }

    @Override
    public SanitizableData apply(final SanitizableData data) {
        if (data.getValue() == null) {
            return data;
        }

        for (final Pattern pattern : keysToSanitize) {
            if (pattern.matcher(data.getKey()).matches()) {
                if (keyIsUriWithUserInfo(pattern)) {
                    return data.withValue(sanitizeUris(data.getValue().toString()));
                }

                return data.withValue(SanitizableData.SANITIZED_VALUE);
            }
        }

        return data;
    }

    private void addKeysToSanitize(final Collection<String> keysToSanitize) {
        for (final String key : keysToSanitize) {
            this.keysToSanitize.add(getPattern(key));
        }
    }

    private Pattern getPattern(final String value) {
        if (isRegex(value)) {
            return Pattern.compile(value, Pattern.CASE_INSENSITIVE);
        }
        return Pattern.compile(".*" + value + "$", Pattern.CASE_INSENSITIVE);
    }

    private boolean isRegex(final String value) {
        for (final String part : REGEX_PARTS) {
            if (value.contains(part)) {
                return true;
            }
        }
        return false;
    }

    private boolean keyIsUriWithUserInfo(final Pattern pattern) {
        for (String uriKey : URI_USERINFO_KEYS) {
            if (pattern.matcher(uriKey).matches()) {
                return true;
            }
        }
        return false;
    }

    private Object sanitizeUris(final String value) {
        return Arrays.stream(value.split(",")).map(this::sanitizeUri).collect(Collectors.joining(","));
    }

    private String sanitizeUri(final String value) {
        final var matcher = URI_USERINFO_PATTERN.matcher(value);
        final var  password = matcher.matches() ? matcher.group(1) : null;
        if (password != null) {
            return StringUtils.replace(value, ":" + password + "@", ":" + SanitizableData.SANITIZED_VALUE + "@");
        }
        return value;
    }
}
