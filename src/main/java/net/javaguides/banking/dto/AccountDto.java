package net.javaguides.banking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

//@Data
//@AllArgsConstructor
//@NoArgsConstructor
//public class AccountDto {
//    private Long id;
//    private String accountHolderName;
//    private double balance;
//}
public record AccountDto(Long id,

                         String accountHolderName,
                         @NotNull(message = "Balance cannot be null")
                         @DecimalMin(value = "0.0",message = "Initial balance cannot be negative")
                         BigDecimal balance){

}


