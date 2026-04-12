package net.javaguides.banking.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransferFundDTO(@NotNull(message = "fromAccountId can not be null") Long fromAccountId,
                              @NotNull(message = "toAccount id can not be null") Long toAccountId,
                              @Positive(message = "Transfer amount must be positive") BigDecimal amount) {
}
