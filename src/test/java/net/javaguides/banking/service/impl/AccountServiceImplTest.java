package net.javaguides.banking.service.impl;

import net.javaguides.banking.dto.TransferFundDTO;
import net.javaguides.banking.entity.Account;
import net.javaguides.banking.entity.Transaction;
import net.javaguides.banking.exception.InsufficientAmountException;
import net.javaguides.banking.mapper.AccountMapper;
import net.javaguides.banking.repository.AccountRepository;
import net.javaguides.banking.repository.TransactionRepository;
import net.javaguides.banking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AccountMapper accountMapper;

    @InjectMocks
    private AccountServiceImpl accountService;


    private Account fromAccount;
    private Account toAccount;

    @BeforeEach
    void setUp() {
        // ... 其他既有設定 ...

        // 準備兩個帳戶用於轉帳測試
        fromAccount = new Account();
        fromAccount.setId(1L);
        fromAccount.setBalance(new BigDecimal("1000.00"));
        fromAccount.setAccountHolderName("Sender");

        toAccount = new Account();
        toAccount.setId(2L);
        toAccount.setBalance(new BigDecimal("500.00"));
        toAccount.setAccountHolderName("Receiver");
    }




    @Test
    @DisplayName("測試-轉帳-餘額不足拋出例外")
    void testTransferFunds_InsufficientAmount_ThrowsException(){

        //Arrange
        Long fromAccountId = 1L;
        Long toAccountId = 2L;
        BigDecimal transferAmount = new BigDecimal("200.00");

        // 建立轉帳請求的 DTO
        TransferFundDTO transferFundDTO = new TransferFundDTO(fromAccountId, toAccountId, transferAmount);

        fromAccount.setBalance(new BigDecimal("100.00"));

        when(accountRepository.findByIdForUpdate(fromAccountId)).thenReturn(Optional.of(fromAccount));

        when(accountRepository.findByIdForUpdate(toAccountId)).thenReturn(Optional.of(toAccount));

        //Act//Assert

        InsufficientAmountException insufficientAmountException =
                assertThrows(InsufficientAmountException.class, () -> accountService.transferFunds(transferFundDTO), "拋出例外有誤");

        assertEquals("Insufficient amount",insufficientAmountException.getMessage(),"例外錯誤訊息不一致");

    }



    @Test
    @DisplayName("測試-轉帳成功")
    void testTransferFunds_Success() {
        //Arrange
        Long fromAccountId = 1L;
        Long toAccountId = 2L;
        BigDecimal transferAmount = new BigDecimal("200.00");

        // 1. 建立轉帳請求的 DTO
        TransferFundDTO transferFundDTO = new TransferFundDTO(fromAccountId, toAccountId, transferAmount);
// 2. 模擬 Repository 的行為
        //    當使用 findByIdForUpdate 尋找這兩個帳戶時，都回傳我們準備好的 Account 物件
        given(accountRepository.findByIdForUpdate(fromAccountId)).willReturn(Optional.of(fromAccount));
        given(accountRepository.findByIdForUpdate(toAccountId)).willReturn(Optional.of(toAccount));

        //Act//Assert

        accountService.transferFunds(transferFundDTO);

        ArgumentCaptor<Account> accountArgumentCaptor =
                ArgumentCaptor.forClass(Account.class);

        verify(accountRepository, times(2)).save(accountArgumentCaptor.capture());

        List<Account> allValues = accountArgumentCaptor.getAllValues();

        Account savedFromAccount = null;
        Account savedToAccount = null;

        for (Account allValue : allValues) {
            if (allValue.getId().equals(fromAccountId)) {
                savedFromAccount = allValue;
            } else if (allValue.getId().equals(toAccountId)) {
                savedToAccount = allValue;
            }
        }

        assertNotNull(savedFromAccount, "沒有捕獲到轉出帳戶");
        assertNotNull(savedToAccount, "沒有捕獲到轉入帳戶");

        assertEquals(0, new BigDecimal("800").compareTo(savedFromAccount.getBalance()));
        assertEquals(0, new BigDecimal("700").compareTo(savedToAccount.getBalance()));

        ArgumentCaptor<Transaction> transactionArgumentCaptor =
                ArgumentCaptor.forClass(Transaction.class);

        verify(transactionRepository,times(2)).save(transactionArgumentCaptor.capture());

        List<Transaction> allValues1 = transactionArgumentCaptor.getAllValues();

        Transaction fromTransaction=null;
        Transaction toTransaction=null;


        for (Transaction transaction : allValues1) {
            if (transaction.getAccountId().equals(fromAccountId)) {
                fromTransaction=transaction;
            }else if(transaction.getAccountId().equals(toAccountId)){
                toTransaction=transaction;
            }
        }

        assertNotNull(fromTransaction,"沒有捕獲到轉出交易紀錄");
        assertNotNull(toTransaction,"沒有捕獲到轉入交易紀錄");

        assertEquals(0,new BigDecimal("200.00").compareTo(fromTransaction.getAmount()));
        assertEquals(0,new BigDecimal("200.00").compareTo(toTransaction.getAmount()));

    }

}