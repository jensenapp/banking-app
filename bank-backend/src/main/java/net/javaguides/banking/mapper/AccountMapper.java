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

        // 修改為使用無參數建構子 + Setter，確保資料正確寫入
        Account account = new Account();
        account.setId(accountDto.id());
        account.setBalance(accountDto.balance());
        // accountHolderName 和 user 會在 Service 層中設定，這裡先忽略
        return account;
    }

    public  AccountDto mapTOAccountDto(Account account){
        return new AccountDto(account.getId(),account.getAccountHolderName(),account.getBalance());
    }

}
