package com.finsecure.pay.service;

import com.finsecure.pay.model.TransactionRequest;
import com.finsecure.pay.model.ValidationResult;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Business rules for a payment transaction:
 * <ol>
 *   <li>transactionId is required</li>
 *   <li>source and destination accounts are required, alphanumeric, 8-20 chars</li>
 *   <li>source and destination accounts must differ</li>
 *   <li>amount is required, greater than zero, at most 2 decimal places</li>
 *   <li>amount must not exceed the configured per-transaction limit</li>
 *   <li>currency must be one of the supported ISO codes</li>
 * </ol>
 * The limit is configurable (app.validation.max-amount) so it can be set from a
 * Kubernetes ConfigMap without rebuilding the image.
 */
@Service
public class TransactionValidator {

    static final Set<String> SUPPORTED_CURRENCIES = Set.of("USD", "EUR", "GBP", "INR");
    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("^[A-Za-z0-9]{8,20}$");

    private final BigDecimal maxAmount;

    public TransactionValidator(@Value("${app.validation.max-amount:100000}") BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    public ValidationResult validate(TransactionRequest request) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            return ValidationResult.invalid(List.of("Request body is required"));
        }

        if (isBlank(request.transactionId())) {
            errors.add("transactionId is required");
        }

        validateAccount("sourceAccount", request.sourceAccount(), errors);
        validateAccount("destinationAccount", request.destinationAccount(), errors);

        if (!isBlank(request.sourceAccount())
                && request.sourceAccount().equalsIgnoreCase(request.destinationAccount())) {
            errors.add("sourceAccount and destinationAccount must be different");
        }

        validateAmount(request.amount(), errors);

        if (isBlank(request.currency())) {
            errors.add("currency is required");
        } else if (!SUPPORTED_CURRENCIES.contains(request.currency())) {
            errors.add("currency '" + request.currency() + "' is not supported; allowed: "
                    + SUPPORTED_CURRENCIES.stream().sorted().toList());
        }

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors);
    }

    private void validateAccount(String field, String value, List<String> errors) {
        if (isBlank(value)) {
            errors.add(field + " is required");
        } else if (!ACCOUNT_PATTERN.matcher(value).matches()) {
            errors.add(field + " must be 8-20 alphanumeric characters");
        }
    }

    private void validateAmount(BigDecimal amount, List<String> errors) {
        if (amount == null) {
            errors.add("amount is required");
            return;
        }
        if (amount.signum() <= 0) {
            errors.add("amount must be greater than zero");
            return;
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            errors.add("amount must have at most 2 decimal places");
        }
        if (amount.compareTo(maxAmount) > 0) {
            errors.add("amount exceeds the maximum allowed per transaction (" + maxAmount.toPlainString() + ")");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
