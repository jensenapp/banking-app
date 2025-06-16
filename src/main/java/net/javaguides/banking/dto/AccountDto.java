package net.javaguides.banking.dto;

import java.math.BigDecimal;

//@Data
//@AllArgsConstructor
//@NoArgsConstructor
//public class AccountDto {
//    private Long id;
//    private String accountHolderName;
//    private double balance;
//}
public record AccountDto(Long id, String accountHolderName, BigDecimal balance){

}


