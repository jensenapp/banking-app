package net.javaguides.banking.service.impl;

import jdk.jfr.Name;
import net.javaguides.banking.dto.AccountDto;
import net.javaguides.banking.dto.TransferFundDTO;
import net.javaguides.banking.entity.Account;
import net.javaguides.banking.entity.IdempotencyRecord;
import net.javaguides.banking.entity.Transaction;
import net.javaguides.banking.enums.TransactionType;
import net.javaguides.banking.exception.AccountException;
import net.javaguides.banking.exception.AccountNotFoundException;
import net.javaguides.banking.exception.InsufficientAmountException;
import net.javaguides.banking.mapper.AccountMapper;
import net.javaguides.banking.repository.AccountRepository;
import net.javaguides.banking.repository.IdempotencyRepository;
import net.javaguides.banking.repository.TransactionRepository;
import net.javaguides.banking.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountServicePracticeTest {
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private IdempotencyRepository idempotencyRepository;

    @Mock
    private AccountMapper accountMapper;

    @InjectMocks
    private AccountServiceImpl accountService;

    private Account fromAccount;
    private Account toAccount;


    @BeforeEach
    void setUp() {
        fromAccount = new Account();
        fromAccount.setId(1L);
        fromAccount.setBalance(new BigDecimal("1000.00"));
        fromAccount.setAccountHolderName("Sender");

        toAccount = new Account();
        toAccount.setId(2L);
        toAccount.setBalance(new BigDecimal("500.00"));
        toAccount.setAccountHolderName("Receiver");

        // 讓 TransactionTemplate.execute(...) 真的執行 callback 內的程式碼
        lenient().when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(invocation -> {
                    TransactionCallback<?> callback = invocation.getArgument(0);
                    return callback.doInTransaction(null);
                });

        // 讓 idempotencyRepository.saveAndFlush(...) 回傳傳入的物件，避免 NullPointerException
        lenient().when(idempotencyRepository.saveAndFlush(any(IdempotencyRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

//        lenient().when(accountRepository.saveAndFlush(any(Account.class)))
//                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("測試-重複Idempotency Key-拋出例外")
    void testTransfer_DuplicateIdempotencyKey_ThrowsException() {

        TransferFundDTO dto =
                new TransferFundDTO(
                        1L,
                        2L,
                        new BigDecimal("500.00"),
                        "duplicate-key"
                );

        when(idempotencyRepository.saveAndFlush(any(IdempotencyRecord.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate key"));

        AccountException exception =
                assertThrows(
                        AccountException.class,
                        () -> accountService.transferFunds(dto)
                );

        assertEquals("轉帳重複請求", exception.getMessage());

        verify(accountRepository, never())
                .findByIdForUpdate(anyLong());

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }

    @Test
    @DisplayName("測試-轉帳餘額不足拋出例外")
    void testTransfer_InsufficientAmount_ThrowsException(){
        //arrange
        TransferFundDTO transferFundDTO = new TransferFundDTO(1L,2L,new BigDecimal("5500.00"),"key");
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(toAccount));
        //act //assert
        AccountException accountException = assertThrows(AccountException.class, () -> accountService.transferFunds(transferFundDTO));
        assertEquals("餘額不足,無法轉帳",accountException.getMessage(),"餘額不足拋出例外訊息不一致");
        verify(accountRepository,never()).save(any(Account.class));
        verify(transactionRepository,never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("測試-轉帳相同帳戶拋出例外")
    void testTransfer_ToSameAccount_ThrowsException(){
        //arrange
        TransferFundDTO transferFundDTO = new TransferFundDTO(1L,1L,new BigDecimal("500.00"),"key");
        //act

        //assert
        AccountException accountException = assertThrows(AccountException.class, () -> accountService.transferFunds(transferFundDTO));
        assertEquals("不能轉帳給自己",accountException.getMessage(),"轉帳相同帳戶拋出訊息不一致");
        verify(accountRepository,never()).save(any(Account.class));
        verify(accountRepository, never())
                .findByIdForUpdate(anyLong());

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }

    @Test
    @DisplayName("測試-轉帳-查無轉出帳戶拋出例外")
    void testTransfer_AaccountNotFound_ThrowsException(){
        //arrange
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());
        //act //assert
        TransferFundDTO transferFundDTO = new TransferFundDTO(1L,2L,new BigDecimal("500.00"),"key");
        assertThrows(AccountNotFoundException.class,()->accountService.transferFunds(transferFundDTO));
        verify(accountRepository,never()).saveAndFlush(any(Account.class));
    }

    @Test
    @DisplayName("測試-轉帳成功")
    void testTransfer_Success(){
        //arrange
        TransferFundDTO transferFundDTO = new TransferFundDTO(1L,2L,new BigDecimal("500.00"),"key");

        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(toAccount));
        //act
        accountService.transferFunds(transferFundDTO);
        //assert
        ArgumentCaptor<Account> accountArgumentCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository,times(2)).save(accountArgumentCaptor.capture());

        List<Account> allValues = accountArgumentCaptor.getAllValues();

        Account saveFromAccount=null;
        Account saveToAccount=null;

        for (Account allValue : allValues) {
            if (allValue.getId().equals(1L)){
                saveFromAccount=allValue;
            }else if(allValue.getId().equals(2L)){
                saveToAccount=allValue;
            }
        }

        assertNotNull(saveFromAccount,"沒有捕獲到轉出帳戶");
        assertNotNull(saveToAccount,"沒有捕獲到轉入帳戶");

        assertEquals(0,new BigDecimal("500.00").compareTo(saveFromAccount.getBalance()),"轉出帳戶餘額錯誤");
        assertEquals(0,new BigDecimal("1000.00").compareTo(saveToAccount.getBalance()),"轉入帳戶餘額錯誤");

        ArgumentCaptor<Transaction> transactionArgumentCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository,times(2)).save(transactionArgumentCaptor.capture());
        List<Transaction> allValues1 = transactionArgumentCaptor.getAllValues();

        Transaction fromTransaction=null;
        Transaction toTransaction=null;

        for (Transaction transaction : allValues1) {
            if (transaction.getAccountId().equals(1L)){
                fromTransaction=transaction;
            }else if (transaction.getAccountId().equals(2L)){
                toTransaction=transaction;
            }
        }
        assertNotNull(fromTransaction,"沒有捕獲到轉出交易紀錄");
        assertNotNull(toTransaction,"沒有捕獲到轉入交易紀錄");

        assertEquals(TransactionType.TRANSFER_OUT,fromTransaction.getTransactionType(),"轉出交易類別不一致");
        assertEquals(TransactionType.TRANSFER_IN,toTransaction.getTransactionType(),"轉入交易類別不一致");

        ArgumentCaptor<IdempotencyRecord> idempotencyRecordArgumentCaptor = ArgumentCaptor.forClass(IdempotencyRecord.class);
        verify(idempotencyRepository,times(1)).saveAndFlush(idempotencyRecordArgumentCaptor.capture());
    }

    @Test
    @DisplayName("測試-提款成功")
    void testWithDraw_Success(){

        //arrange
        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        //act
         accountService.withdraw(1L,new BigDecimal("100.00"));
        //assert
        ArgumentCaptor<Account> accountArgumentCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository,times(1)).saveAndFlush(accountArgumentCaptor.capture());
        assertEquals(0,new BigDecimal("900.00").compareTo(accountArgumentCaptor.getValue().getBalance()));

        ArgumentCaptor<Transaction> transactionArgumentCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository,times(1)).save(transactionArgumentCaptor.capture());
        assertEquals(0,new BigDecimal("100.00").compareTo(transactionArgumentCaptor.getValue().getAmount()));
        assertEquals(TransactionType.WITHDRAW,transactionArgumentCaptor.getValue().getTransactionType());

    }

    @Test
    @DisplayName("測試-提款找不到帳戶-拋出例外")
    void testWithDraw_AccountNotFound_throwsEsception(){
        //arange
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());
        //act assert
        AccountNotFoundException accountNotFoundException =
                assertThrows(AccountNotFoundException.class, () -> accountService.withdraw(1L, new BigDecimal("100.00")));

        assertEquals("Account does not exist",accountNotFoundException.getMessage(),"回傳訊息錯誤不一致");

    }

    @Test
    @DisplayName("測試-提款餘額不足")
    void testWithdraw_InsufficientAmount(){

        //arrange
        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));

        //act
        InsufficientAmountException insufficientAmountException =
                assertThrows(InsufficientAmountException.class, () -> accountService.withdraw(1L, new BigDecimal("2000.00")));

        //assert
        assertEquals("Insufficient amount",insufficientAmountException.getMessage(),"回傳訊息錯誤不一致");
//        assertEquals(0,new BigDecimal("1000.00").compareTo(fromAccount.getBalance()),"提款失敗後餘額不一致");
        verify(accountRepository,never()).saveAndFlush(any(Account.class));
    }

    @Test
    @DisplayName("測試-存款成功")
    void testDeposit_success(){
        //arrange
        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        //act
       accountService.deposit(1L, new BigDecimal("500.00"));
        //assert
        ArgumentCaptor<Account> accountArgumentCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository,times(1)).saveAndFlush(accountArgumentCaptor.capture());
        BigDecimal balance = accountArgumentCaptor.getValue().getBalance();
        assertEquals(0,new BigDecimal("1500.00").compareTo(balance));

        ArgumentCaptor<Transaction> transactionArgumentCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository,times(1)).save(transactionArgumentCaptor.capture());
        assertEquals(TransactionType.DEPOSIT,transactionArgumentCaptor.getValue().getTransactionType());
        assertEquals(0,new BigDecimal("500.00").compareTo(transactionArgumentCaptor.getValue().getAmount()));
    }

    @Test
    @DisplayName("測試-存款-帳號不存在拋出例外")
    void test_Deposit_accountNotFound_throwException(){
        //arrange
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());
        //act //assert
        AccountNotFoundException accountNotFoundException = assertThrows(AccountNotFoundException.class, () -> accountService.deposit(1L, new BigDecimal("500.00")));
        assertEquals("Account does not exist",accountNotFoundException.getMessage(),"拋出例外訊息不一致");
        verify(accountRepository,never()).saveAndFlush(any(Account.class));
    }
}
