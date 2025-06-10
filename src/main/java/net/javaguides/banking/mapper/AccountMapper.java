package net.javaguides.banking.mapper;

import net.javaguides.banking.dto.AccountDto;
import net.javaguides.banking.entity.Account;

public class AccountMapper {

    public static Account mapTOAccount(AccountDto accountDto){
     return  new Account(accountDto.getId(),accountDto.getAccountHolderName(),accountDto.getBalance());
    }

    public static AccountDto mapTOAccountDto(Account account){
        return new AccountDto(account.getId(),account.getAccountHolderName(),account.getBalance());
    }

}
