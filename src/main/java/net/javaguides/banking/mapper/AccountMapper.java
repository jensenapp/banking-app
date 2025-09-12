package net.javaguides.banking.mapper;

import jakarta.persistence.Column;
import net.javaguides.banking.dto.AccountDto;
import net.javaguides.banking.entity.Account;
import net.javaguides.banking.entity.User;
import net.javaguides.banking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {


    public  Account mapTOAccount(AccountDto accountDto){

     return new Account(accountDto.id(),accountDto.accountHolderName(),accountDto.balance(),null);
    }

    public  AccountDto mapTOAccountDto(Account account){
        return new AccountDto(account.getId(),account.getAccountHolderName(),account.getBalance());
    }

}
