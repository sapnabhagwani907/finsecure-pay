package com.finsecure.pay.model;

import java.util.List;

public record ValidationResult(String status, List<String> errors) {

    public static ValidationResult valid() {
        return new ValidationResult("VALID", List.of());
    }

    public static ValidationResult invalid(List<String> errors) {
        return new ValidationResult("INVALID", List.copyOf(errors));
    }

    public boolean isValid() {
        return "VALID".equals(status);
    }
}
