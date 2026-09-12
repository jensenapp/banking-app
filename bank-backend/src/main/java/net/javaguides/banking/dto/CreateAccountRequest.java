package net.javaguides.banking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record  CreateAccountRequest(
        @NotNull(message = "Balance cannot be null")
        @DecimalMin(value = "0.0", message = "Initial balance cannot be negative")
        BigDecimal balance,Long userId
) {
}