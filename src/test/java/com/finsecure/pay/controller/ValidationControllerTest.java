package com.finsecure.pay.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.finsecure.pay.service.TransactionValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ValidationController.class)
@Import(TransactionValidator.class)
@TestPropertySource(properties = {"app.validation.max-amount=100000", "app.version=1.0.0-test"})
class ValidationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /health returns UP")
    void healthEndpoint() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.version").value("1.0.0-test"));
    }

    @Test
    @DisplayName("POST /validate returns 200 for a valid transaction")
    void validTransaction() throws Exception {
        String body = """
                {"transactionId":"TX-1001","sourceAccount":"ACC12345678",
                 "destinationAccount":"ACC87654321","amount":250.50,"currency":"USD"}
                """;

        mockMvc.perform(post("/validate").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALID"))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    @DisplayName("POST /validate returns 400 with errors for an invalid transaction")
    void invalidTransaction() throws Exception {
        String body = """
                {"transactionId":"TX-1002","sourceAccount":"ACC12345678",
                 "destinationAccount":"ACC87654321","amount":-10,"currency":"XYZ"}
                """;

        mockMvc.perform(post("/validate").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("INVALID"))
                .andExpect(jsonPath("$.errors.length()").value(2));
    }

    @Test
    @DisplayName("POST /validate returns 400 for malformed JSON")
    void malformedJson() throws Exception {
        mockMvc.perform(post("/validate").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
    }
}
