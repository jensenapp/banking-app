package net.javaguides.banking.service;

import net.javaguides.banking.dto.AccountDto;
import net.javaguides.banking.dto.TransactionDTO;
import net.javaguides.banking.dto.TransferFundDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {

    AccountDto createAccount(AccountDto accountDto);

    AccountDto getAccountById(Long id);

    AccountDto deposit(Long id, BigDecimal amount);

    AccountDto withdraw(Long id, BigDecimal amount);

    Page<AccountDto> getAllAccounts(Pageable pageable);

    void deleteAccount(Long id);

    void transferFunds(TransferFundDTO transferFundDTO);

    Page<TransactionDTO> getAccountTransactions(Long accountId, Pageable pageable);
}
