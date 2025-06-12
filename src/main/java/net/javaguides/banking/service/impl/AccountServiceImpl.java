package net.javaguides.banking.service.impl;

import net.javaguides.banking.dto.AccountDto;
import net.javaguides.banking.entity.Account;
import net.javaguides.banking.exception.AccountException;
import net.javaguides.banking.mapper.AccountMapper;
import net.javaguides.banking.repository.AccountRepository;
import net.javaguides.banking.service.AccountService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class AccountServiceImpl implements AccountService {

    private AccountRepository accountRepository;

    public AccountServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public AccountDto createAccount(AccountDto accountDto) {
        Account account = AccountMapper.mapTOAccount(accountDto);
        Account saveAccount = accountRepository.save(account);
        AccountDto accountDto1 = AccountMapper.mapTOAccountDto(saveAccount);
        return accountDto1;
    }

    @Override
    public AccountDto getAccountById(Long id) {
        Account account = accountRepository.findById(id).orElseThrow(() -> new AccountException("Account does not exist"));
        return AccountMapper.mapTOAccountDto(account);
    }

    @Override
    public AccountDto deposit(Long id, Double amount) {

        Account account = accountRepository.
                findById(id).orElseThrow(() -> new AccountException("Account does not exist"));

        account.setBalance(account.getBalance() + amount);

        Account saveAccount = accountRepository.save(account);

        AccountDto accountDto = AccountMapper.mapTOAccountDto(saveAccount);

        return accountDto;
    }

    @Override
    public AccountDto withdraw(Long id, Double amount) {
        Account account = accountRepository.findById(id).orElseThrow(() -> new AccountException("Account does not exist"));

        if (account.getBalance() < amount) {
            throw new AccountException("Insufficient amount");
        }


        account.setBalance(account.getBalance() - amount);
        accountRepository.save(account);
        AccountDto accountDto = AccountMapper.mapTOAccountDto(account);
        return accountDto;
    }

    @Override
    public List<AccountDto> getAllAccounts() {
        List<Account> accounts = accountRepository.findAll();

        List<AccountDto> accountDtoList =
                accounts.stream().
                        map(account -> AccountMapper.mapTOAccountDto(account)).
                        collect(Collectors.toList());
        return accountDtoList;
    }

    @Override
    public void deleteAccount(Long id) {
        Account account = accountRepository.findById(id).orElseThrow(() -> new AccountException("Account does not exist"));
        accountRepository.deleteById(id);
    }
}
