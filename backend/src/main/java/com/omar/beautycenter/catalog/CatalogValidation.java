package com.omar.beautycenter.catalog;

import com.omar.beautycenter.common.error.BusinessRuleException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.regex.Pattern;

final class CatalogValidation {

    private static final Pattern CODE_PATTERN = Pattern.compile("[A-Z0-9_]{1,50}");

    private CatalogValidation() {
    }

    static String code(String value) {
        String normalized = value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
        if (!CODE_PATTERN.matcher(normalized).matches()) {
            throw new BusinessRuleException(
                    "Code must contain 1 to 50 letters, numbers, or underscores.");
        }
        return normalized;
    }

    static String requiredText(String value, String field, int maximumLength) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isEmpty() || normalized.length() > maximumLength) {
            throw new BusinessRuleException(
                    field + " must contain 1 to " + maximumLength + " characters.");
        }
        return normalized;
    }

    static String optionalText(String value, String field, int maximumLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.length() > maximumLength) {
            throw new BusinessRuleException(
                    field + " must not exceed " + maximumLength + " characters.");
        }
        return normalized;
    }

    static BigDecimal money(BigDecimal value, String field) {
        if (value == null || value.signum() < 0) {
            throw new BusinessRuleException(field + " must be zero or greater.");
        }
        try {
            return value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new BusinessRuleException(field + " must have at most 2 decimal digits.");
        }
    }
}
