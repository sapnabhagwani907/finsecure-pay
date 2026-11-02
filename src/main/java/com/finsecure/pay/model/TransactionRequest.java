package com.finsecure.pay.model;

import java.math.BigDecimal;

/**
 * Incoming transaction to validate. Fields are nullable on purpose so the
 * validator can report every problem at once instead of failing on the first.
 */
public record TransactionRequest(
        String transactionId,
        String sourceAccount,
        String destinationAccount,
        BigDecimal amount,
        String currency) {
}
