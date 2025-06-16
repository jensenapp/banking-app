package net.javaguides.banking.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionDTO(Long id,
                             Long accountId,
                             BigDecimal amount,
                             String transactionType,
                             LocalDateTime timestamp) {}
