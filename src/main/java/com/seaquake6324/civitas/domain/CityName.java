package com.seaquake6324.civitas.domain;

import java.util.Locale;

public final class CityName {
    private CityName() {}

    public static Validation validate(String input) {
        if (input == null) return new Validation(false, "", "civitas.error.name_length");
        String normalized = input.strip();
        int length = normalized.codePointCount(0, normalized.length());
        if (length < 2 || length > 20) return new Validation(false, normalized, "civitas.error.name_length");
        for (int offset = 0; offset < normalized.length();) {
            int codePoint = normalized.codePointAt(offset);
            if (codePoint == '\u00A7' || Character.isISOControl(codePoint) ||
                    !(Character.isLetterOrDigit(codePoint) || codePoint == ' ')) {
                return new Validation(false, normalized, "civitas.error.name_characters");
            }
            offset += Character.charCount(codePoint);
        }
        return new Validation(true, normalized, "");
    }

    public static String uniquenessKey(String name) {
        return name.strip().toLowerCase(Locale.ROOT);
    }

    public record Validation(boolean valid, String normalized, String errorKey) {}
}
