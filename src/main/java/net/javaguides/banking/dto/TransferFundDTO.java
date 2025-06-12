package net.javaguides.banking.dto;

public record TransferFundDTO(Long fromAccountId,
                              Long toAccountId,
                              Double amount) {
}
