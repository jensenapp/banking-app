package net.javaguides.banking.service.impl;

import jakarta.transaction.Transactional;
import net.javaguides.banking.dto.AccountDto;
import net.javaguides.banking.dto.TransferFundDTO;
import net.javaguides.banking.entity.Account;
import net.javaguides.banking.entity.Transaction;
import net.javaguides.banking.entity.User;
import net.javaguides.banking.enums.TransactionType;
import net.javaguides.banking.exception.AccountNotFoundException;
import net.javaguides.banking.exception.InsufficientAmountException;
import net.javaguides.banking.repository.AccountRepository;
import net.javaguides.banking.repository.TransactionRepository;
import net.javaguides.banking.repository.UserRepository;
import net.javaguides.banking.service.AccountService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AccountServiceIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    @DisplayName("Service Integration Test - 轉帳成功")
    void transfer_success(){
        //arrange
        User fromUser = new User();
        fromUser.setUsername("Tom");
        fromUser.setEmail("tom@tom.com");
        fromUser.setRealName("tom lee");

        User toUser = new User();
        toUser.setUsername("Leo");
        toUser.setEmail("leo@leo.com");
        toUser.setRealName("tom lee");

        userRepository.save(fromUser);

        userRepository.save(toUser);

        Account fromAccount = new Account();
        fromAccount.setUser(fromUser);
        fromAccount.setBalance(new BigDecimal("1000.00"));

        Account toAccount = new Account();
        toAccount.setUser(toUser);
        toAccount.setBalance(new BigDecimal("2000.00"));

        accountRepository.save(fromAccount);

        accountRepository.save(toAccount);


        TransferFundDTO transferFundDTO = new TransferFundDTO(fromAccount.getId(),toAccount.getId(),new BigDecimal("500.00"),"key");


        //act
        accountService.transferFunds(transferFundDTO);
        Account savedFromAccount = accountRepository.findById(fromAccount.getId()).orElseThrow();

        Account savedToAccount = accountRepository.findById(toAccount.getId()).orElseThrow();

        List<Transaction> from = transactionRepository.findByAccountId(fromAccount.getId());
        List<Transaction> to = transactionRepository.findByAccountId(toAccount.getId());

        //assert

        assertEquals(0,new BigDecimal("500.00").compareTo(savedFromAccount.getBalance()),"轉出成功訊息不一致");
        assertEquals(0,new BigDecimal("2500.00").compareTo(savedToAccount.getBalance()),"轉入成功訊息不一致");

        assertEquals(1,from.size());
        assertEquals(1,to.size());

        assertEquals(TransactionType.TRANSFER_OUT,from.get(0).getTransactionType());
        assertEquals(TransactionType.TRANSFER_OUT,from.get(0).getTransactionType());

        assertEquals(
                0,
                new BigDecimal("500.00")
                        .compareTo(from.get(0).getAmount()));

        assertEquals(
                0,
                new BigDecimal("500.00")
                        .compareTo(to.get(0).getAmount()));
    }

    @Test
    @DisplayName("Service Integration Test - 取款失敗-查無帳戶")
    void withDraw_accountNotFound(){
        //arrange
        //act
        AccountNotFoundException accountNotFoundException = assertThrows(AccountNotFoundException.class, () -> accountService.withdraw(11L, new BigDecimal("5000.00")));
        //assert
        assertNotNull(accountNotFoundException,"帳戶不存在");
        assertEquals("Account does not exist",accountNotFoundException.getMessage(),"帳戶不存在,拋出訊息不一致");

    }


    @Test
    @DisplayName("Service Integration Test - 取款失敗-餘額不足")
    void withDraw_insufficientBalance(){
        //arrange
        User user = new User();
        user.setUsername("tom");
        user.setEmail("tom@tom.com");
        user.setRealName("tom lee");

        userRepository.save(user);

        Account account = new Account();
        account.setUser(user);
        account.setBalance(new BigDecimal("1000.00"));

        accountRepository.save(account);
        //act //assert
        assertThrows(InsufficientAmountException.class,()->accountService.withdraw(account.getId(), new BigDecimal("1500.00")));
        Account savedAccount = accountRepository.findById(account.getId()).orElseThrow();
        assertEquals(
                0,
                new BigDecimal("1000.00")
                        .compareTo(savedAccount.getBalance()),
                "餘額不足時，Account balance 不應該被扣除"
        );

        List<Transaction> byAccountId = transactionRepository.findByAccountId(account.getId());
        assertTrue(byAccountId.isEmpty(),"餘額不足時,不應該建立Transaction");
    }

    @Test
    @DisplayName("Service Integration Test - 取款成功")
    void withdraw_success(){

        //arrange
        User user = new User();
        user.setUsername("tom");
        user.setEmail("tom@tom.com");
        user.setRealName("tom lee");

        userRepository.save(user);

        Account account = new Account();
        account.setUser(user);
        account.setBalance(new BigDecimal("1000.00"));

        accountRepository.save(account);
        //act
        accountService.withdraw(account.getId(), new BigDecimal("500.00"));

        //assert
        Account saveAccount = accountRepository.findById(account.getId()).orElseThrow();
        assertEquals(0,new BigDecimal("500.00").compareTo(saveAccount.getBalance()),"取款後金額不一致");
        // 再從真正 DB 查 Transaction
        List<Transaction> saveTransaction = transactionRepository.findByAccountId(account.getId());
        // 驗證真的建立 WITHDRAW record
        assertEquals(TransactionType.WITHDRAW,saveTransaction.get(0).getTransactionType(),"轉帳形式不一致");
        assertEquals(0,new BigDecimal("500.00").compareTo(saveTransaction.get(0).getAmount()));
    }


    @Test
    @DisplayName("Service Integration Test - 存款成功")
    void deposit_success() {

        // Arrange
        // 建立 User
        // 建立 Account
        // 存進真正 DB

        User user = new User();
        user.setUsername("tom");
        user.setEmail("tom@tom.com");
        user.setRealName("tom lee");

        userRepository.save(user);

        Account account = new Account();
        account.setUser(user);
        account.setBalance(new BigDecimal("1000.00"));

        accountRepository.save(account);

        // Act
        accountService.deposit(
                account.getId(),
                new BigDecimal("500.00")
        );

        // Assert
        // 從真正 DB 再查一次 Account
        // 驗證 balance = 1500
        Account saveAccount = accountRepository.findById(account.getId()).orElseThrow();
        assertEquals(0,new BigDecimal("1500.00").compareTo(saveAccount.getBalance()),"存款後金額不一致");
        // 再從真正 DB 查 Transaction
        List<Transaction> saveTransaction = transactionRepository.findByAccountId(account.getId());
        // 驗證真的建立 DEPOSIT record
        assertEquals(TransactionType.DEPOSIT,saveTransaction.get(0).getTransactionType(),"轉帳形式不一致");
        assertEquals(0,new BigDecimal("500.00").compareTo(saveTransaction.get(0).getAmount()));
    }
}
