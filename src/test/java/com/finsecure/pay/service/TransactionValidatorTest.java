package com.finsecure.pay.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.finsecure.pay.model.TransactionRequest;
import com.finsecure.pay.model.ValidationResult;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TransactionValidatorTest {

    private TransactionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TransactionValidator(new BigDecimal("100000"));
    }

    private static TransactionRequest request(String amount, String currency) {
        return new TransactionRequest("TX-1001", "ACC12345678", "ACC87654321",
                amount == null ? null : new BigDecimal(amount), currency);
    }

    // ---------- success cases ----------

    @Test
    @DisplayName("A well-formed transaction is valid")
    void validTransaction() {
        ValidationResult result = validator.validate(request("250.50", "USD"));

        assertTrue(result.isValid());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    @DisplayName("An amount exactly at the limit is valid")
    void amountAtLimitIsValid() {
        assertTrue(validator.validate(request("100000", "EUR")).isValid());
    }

    @ParameterizedTest
    @ValueSource(strings = {"USD", "EUR", "GBP", "INR"})
    @DisplayName("All supported currencies are accepted")
    void supportedCurrencies(String currency) {
        assertTrue(validator.validate(request("10.00", currency)).isValid());
    }

    // ---------- failure cases ----------

    @Test
    @DisplayName("Zero amount is rejected")
    void zeroAmount() {
        ValidationResult result = validator.validate(request("0", "USD"));

        assertFalse(result.isValid());
        assertTrue(result.errors().contains("amount must be greater than zero"));
    }

    @Test
    @DisplayName("Negative amount is rejected")
    void negativeAmount() {
        ValidationResult result = validator.validate(request("-5.00", "USD"));

        assertFalse(result.isValid());
        assertTrue(result.errors().contains("amount must be greater than zero"));
    }

    @Test
    @DisplayName("Missing amount is rejected")
    void missingAmount() {
        ValidationResult result = validator.validate(request(null, "USD"));

        assertFalse(result.isValid());
        assertTrue(result.errors().contains("amount is required"));
    }

    @Test
    @DisplayName("Amount over the per-transaction limit is rejected")
    void amountOverLimit() {
        ValidationResult result = validator.validate(request("100000.01", "USD"));

        assertFalse(result.isValid());
        assertTrue(result.errors().get(0).startsWith("amount exceeds the maximum"));
    }

    @Test
    @DisplayName("More than two decimal places is rejected")
    void tooManyDecimals() {
        ValidationResult result = validator.validate(request("10.999", "USD"));

        assertFalse(result.isValid());
        assertTrue(result.errors().contains("amount must have at most 2 decimal places"));
    }

    @Test
    @DisplayName("Unsupported currency is rejected")
    void unsupportedCurrency() {
        ValidationResult result = validator.validate(request("10.00", "XYZ"));

        assertFalse(result.isValid());
        assertTrue(result.errors().get(0).contains("'XYZ' is not supported"));
    }

    @Test
    @DisplayName("Missing currency is rejected")
    void missingCurrency() {
        ValidationResult result = validator.validate(request("10.00", " "));

        assertFalse(result.isValid());
        assertTrue(result.errors().contains("currency is required"));
    }

    @Test
    @DisplayName("Missing transactionId is rejected")
    void missingTransactionId() {
        TransactionRequest req = new TransactionRequest(null, "ACC12345678", "ACC87654321",
                new BigDecimal("10"), "USD");

        assertTrue(validator.validate(req).errors().contains("transactionId is required"));
    }

    @Test
    @DisplayName("Same source and destination account is rejected")
    void sameAccounts() {
        TransactionRequest req = new TransactionRequest("TX-1", "ACC12345678", "acc12345678",
                new BigDecimal("10"), "USD");

        assertTrue(validator.validate(req).errors()
                .contains("sourceAccount and destinationAccount must be different"));
    }

    @Test
    @DisplayName("Malformed account numbers are rejected")
    void malformedAccount() {
        TransactionRequest req = new TransactionRequest("TX-1", "SHORT", "ACC-8765-4321",
                new BigDecimal("10"), "USD");

        ValidationResult result = validator.validate(req);

        assertTrue(result.errors().contains("sourceAccount must be 8-20 alphanumeric characters"));
        assertTrue(result.errors().contains("destinationAccount must be 8-20 alphanumeric characters"));
    }

    @Test
    @DisplayName("All problems are reported together, not just the first")
    void multipleErrorsReported() {
        TransactionRequest req = new TransactionRequest("", "", "", new BigDecimal("-1"), "XYZ");

        ValidationResult result = validator.validate(req);

        assertFalse(result.isValid());
        assertEquals(5, result.errors().size());
    }

    @Test
    @DisplayName("Null request body is rejected")
    void nullRequest() {
        ValidationResult result = validator.validate(null);

        assertFalse(result.isValid());
        assertEquals(1, result.errors().size());
    }

    @Test
    @DisplayName("The limit is configurable")
    void configurableLimit() {
        TransactionValidator strict = new TransactionValidator(new BigDecimal("100"));

        assertFalse(strict.validate(request("100.01", "USD")).isValid());
        assertTrue(strict.validate(request("100.00", "USD")).isValid());
    }
}
