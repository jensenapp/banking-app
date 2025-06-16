package net.javaguides.banking.dto;

import java.math.BigDecimal;

public record TransferFundDTO(Long fromAccountId,
                              Long toAccountId,
                              BigDecimal amount) {
}
