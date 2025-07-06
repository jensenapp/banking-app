package net.javaguides.banking.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record AmountRequestDto(@NotNull(message = "Amount cannot be nul")
                               @Positive(message = "Amount must be a positive value")
                               BigDecimal amount) {
}
