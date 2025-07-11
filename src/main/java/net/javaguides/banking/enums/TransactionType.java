package net.javaguides.banking.enums;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;


public enum TransactionType {
    DEPOSIT,
    WITHDRAW,
    TRANSFER_IN,
    TRANSFER_OUT
}
