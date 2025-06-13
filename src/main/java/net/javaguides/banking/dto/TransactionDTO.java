package net.javaguides.banking.dto;

import java.time.LocalDateTime;

public record TransactionDTO( Long id,
                              Long accountId,
                              Double amount,
                              String transactionType,
                              LocalDateTime timestamp) {}
