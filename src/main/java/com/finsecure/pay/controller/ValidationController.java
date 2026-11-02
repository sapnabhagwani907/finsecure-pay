package com.finsecure.pay.controller;

import com.finsecure.pay.model.TransactionRequest;
import com.finsecure.pay.model.ValidationResult;
import com.finsecure.pay.service.TransactionValidator;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ValidationController {

    private static final Logger log = LoggerFactory.getLogger(ValidationController.class);

    private final TransactionValidator validator;
    private final String appVersion;

    public ValidationController(TransactionValidator validator,
                                @Value("${app.version:1.0.0}") String appVersion) {
        this.validator = validator;
        this.appVersion = appVersion;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "version", appVersion);
    }

    @PostMapping("/validate")
    public ResponseEntity<ValidationResult> validate(@RequestBody TransactionRequest request) {
        ValidationResult result = validator.validate(request);
        String txId = request == null ? "n/a" : request.transactionId();

        if (result.isValid()) {
            log.info("Transaction {} validated successfully", txId);
            return ResponseEntity.ok(result);
        }
        log.warn("Transaction {} failed validation: {}", txId, result.errors());
        return ResponseEntity.badRequest().body(result);
    }
}
