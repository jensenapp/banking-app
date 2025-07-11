package net.javaguides.banking.dto;

import net.javaguides.banking.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionDTO(Long id,
                             Long accountId,
                             BigDecimal amount,
                             TransactionType transactionType,
                             LocalDateTime timestamp) {}
