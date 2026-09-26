package net.javaguides.banking.service.impl;

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
import net.javaguides.banking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

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

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private IdempotencyRepository idempotencyRepository;

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

        lenient().when(accountRepository.saveAndFlush(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("測試-提款成功")
    void testWithdraw_Success() {
        // Arrange
        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));

        // Act
        AccountDto accountDto = accountService.withdraw(1L, new BigDecimal("100.00"));

        // Assert
        ArgumentCaptor<Account> accountArgumentCaptor = ArgumentCaptor.forClass(Account.class);

        verify(accountRepository, times(1)).saveAndFlush(accountArgumentCaptor.capture());

        assertEquals(
                0,
                new BigDecimal("900.00").compareTo(accountArgumentCaptor.getValue().getBalance())
        );

        ArgumentCaptor<Transaction> transactionArgumentCaptor = ArgumentCaptor.forClass(Transaction.class);

        verify(transactionRepository, times(1)).save(transactionArgumentCaptor.capture());

        assertEquals(
                0,
                new BigDecimal("100.00").compareTo(transactionArgumentCaptor.getValue().getAmount())
        );

        assertEquals(TransactionType.WITHDRAW, transactionArgumentCaptor.getValue().getTransactionType());
    }

    @Test
    @DisplayName("測試-存款找不到帳戶-拋出例外")
    void testDeposit_AccountNotFound_ThrowsException() {
        // Arrange
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        AccountNotFoundException accountNotFoundException =
                assertThrows(
                        AccountNotFoundException.class,
                        () -> accountService.deposit(1L, new BigDecimal("1000.00"))
                );

        assertEquals(
                "Account does not exist",
                accountNotFoundException.getMessage(),
                "回傳錯誤訊息不一致"
        );
    }

    @Test
    @DisplayName("測試-存款鎖定衝突持續存在-拋出例外")
    void testDeposit_WhenLockingConflictPersists_ThrowsException() {
        // Arrange
        Account account1 = new Account();
        account1.setId(1L);
        account1.setAccountHolderName("tom");
        account1.setBalance(new BigDecimal("1000.00"));

        when(accountRepository.findById(1L))
                .thenReturn(Optional.of(account1));

        when(accountRepository.saveAndFlush(any(Account.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Account.class, 1L));

        // Act & Assert
        AccountException accountException =
                assertThrows(
                        AccountException.class,
                        () -> accountService.deposit(1L, new BigDecimal("1000"))
                );

        assertEquals(
                "存款操作因高併發衝突而失敗，請稍後再試。",
                accountException.getMessage(),
                "例外錯誤訊息不一致"
        );

        verify(accountRepository, times(3)).findById(1L);
        verify(accountRepository, times(3)).saveAndFlush(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("測試-存款鎖定衝突發生-重試後成功")
    void testDeposit_WhenLockingConflictOccurs_ShouldRetryAndSucceed() {
        // Arrange
        Account account1 = new Account();
        account1.setId(1L);
        account1.setAccountHolderName("tom");
        account1.setBalance(new BigDecimal("1000.00"));

        Account account2 = new Account();
        account2.setId(1L);
        account2.setAccountHolderName("tom");
        account2.setBalance(new BigDecimal("1000.00"));

        // 第一次呼叫 saveAndFlush() 時丟出樂觀鎖例外，第二次才成功
        when(accountRepository.findById(1L))
                .thenReturn(Optional.of(account1))
                .thenReturn(Optional.of(account2));

        when(accountRepository.saveAndFlush(any(Account.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Account.class, 1L))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AccountDto resultDto = accountService.deposit(1L, new BigDecimal("500.00"));

        // Assert
        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);

        verify(accountRepository, times(2)).saveAndFlush(accountCaptor.capture());

        Account lastSavedAccount = accountCaptor.getValue();

        assertEquals(
                0,
                new BigDecimal("1500.00").compareTo(lastSavedAccount.getBalance()),
                "存款後餘額有誤"
        );

        // 驗證交易紀錄仍然只會被建立一次
        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);

        verify(transactionRepository, times(1)).save(txCaptor.capture());

        Transaction savedTx = txCaptor.getValue();

        assertEquals(new BigDecimal("500.00"), savedTx.getAmount(), "交易金額錯誤");
        assertEquals(TransactionType.DEPOSIT, savedTx.getTransactionType(), "交易類型錯誤");
    }

    @Test
    @DisplayName("測試-存款成功")
    void testDeposit_Success() {
        // Arrange
        Account account = new Account();
        account.setId(1L);
        account.setAccountHolderName("tom");
        account.setBalance(new BigDecimal("1000.00"));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        // Act
        AccountDto accountDto = accountService.deposit(1L, new BigDecimal("1000.00"));

        // Assert
        ArgumentCaptor<Account> accountArgumentCaptor = ArgumentCaptor.forClass(Account.class);

        verify(accountRepository, times(1)).saveAndFlush(accountArgumentCaptor.capture());

        Account savedAccount = accountArgumentCaptor.getValue();

        assertEquals(
                0,
                new BigDecimal("2000.00").compareTo(savedAccount.getBalance()),
                "存款結算後金額有誤"
        );

        ArgumentCaptor<Transaction> transactionArgumentCaptor = ArgumentCaptor.forClass(Transaction.class);

        verify(transactionRepository, times(1)).save(transactionArgumentCaptor.capture());

        Transaction savedTransaction = transactionArgumentCaptor.getValue();

        assertEquals(new BigDecimal("1000.00"), savedTransaction.getAmount(), "存款紀錄金額有誤");
        assertEquals(TransactionType.DEPOSIT, savedTransaction.getTransactionType(), "存款紀錄標記錯誤");
    }

    @Test
    @DisplayName("測試-轉帳-找不到帳戶拋出例外")
    void testTransferFunds_FromAccountNotFound_ThrowsException() {
        // Arrange
        Long fromAccountId = 1L;
        Long toAccountId = 2L;
        BigDecimal transferAmount = new BigDecimal("200.00");

        TransferFundDTO transferFundDTO =
                new TransferFundDTO(fromAccountId, toAccountId, transferAmount, "test-key-not-found");

        when(accountRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(fromAccount));

        when(accountRepository.findByIdForUpdate(2L))
                .thenThrow(new AccountNotFoundException("Account does not exist"));

        // Act & Assert
        AccountNotFoundException accountNotFoundException =
                assertThrows(
                        AccountNotFoundException.class,
                        () -> accountService.transferFunds(transferFundDTO)
                );

        assertEquals(
                "Account does not exist",
                accountNotFoundException.getMessage(),
                "錯誤訊息不一致"
        );

        verify(idempotencyRepository, times(1)).saveAndFlush(any(IdempotencyRecord.class));
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("測試-轉帳-相同帳戶拋出例外")
    void testTransferFunds_ToSameAccount_ThrowsException() {
        // Arrange
        Long fromAccountId = 1L;
        Long toAccountId = 1L;
        BigDecimal transferAmount = new BigDecimal("200.00");

        TransferFundDTO transferFundDTO =
                new TransferFundDTO(fromAccountId, toAccountId, transferAmount, "test-key-same-account");

        // Act & Assert
        AccountException accountException =
                assertThrows(
                        AccountException.class,
                        () -> accountService.transferFunds(transferFundDTO)
                );

        assertEquals(
                "不能轉帳給自己",
                accountException.getMessage(),
                "錯誤訊息不一致"
        );

        // 因為同帳戶會在 service 最前面就被擋下，不應該寫入 idempotency record
        verify(idempotencyRepository, times(1)).saveAndFlush(any(IdempotencyRecord.class));
        verify(accountRepository, never()).findById(any(Long.class));
        verify(accountRepository, never()).findByIdForUpdate(any(Long.class));
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("測試-轉帳-餘額不足拋出例外")
    void testTransferFunds_InsufficientAmount_ThrowsException() {
        // Arrange
        Long fromAccountId = 1L;
        Long toAccountId = 2L;
        BigDecimal transferAmount = new BigDecimal("200.00");

        TransferFundDTO transferFundDTO =
                new TransferFundDTO(fromAccountId, toAccountId, transferAmount, "test-key-insufficient");

        fromAccount.setBalance(new BigDecimal("100.00"));

        when(accountRepository.findByIdForUpdate(fromAccountId))
                .thenReturn(Optional.of(fromAccount));

        when(accountRepository.findByIdForUpdate(toAccountId))
                .thenReturn(Optional.of(toAccount));

        // Act & Assert
        AccountException accountException =
                assertThrows(
                        AccountException.class,
                        () -> accountService.transferFunds(transferFundDTO),
                        "拋出例外有誤"
                );

        assertEquals(
                "餘額不足,無法轉帳",
                accountException.getMessage(),
                "例外錯誤訊息不一致"
        );

        verify(idempotencyRepository, times(1)).saveAndFlush(any(IdempotencyRecord.class));
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("測試-轉帳成功")
    void testTransferFunds_Success() {
        // Arrange
        Long fromAccountId = 1L;
        Long toAccountId = 2L;
        BigDecimal transferAmount = new BigDecimal("200.00");

        TransferFundDTO transferFundDTO =
                new TransferFundDTO(fromAccountId, toAccountId, transferAmount, "test-key-success");

        given(accountRepository.findByIdForUpdate(fromAccountId))
                .willReturn(Optional.of(fromAccount));

        given(accountRepository.findByIdForUpdate(toAccountId))
                .willReturn(Optional.of(toAccount));

        // Act
        accountService.transferFunds(transferFundDTO);

        // Assert
        verify(idempotencyRepository, times(1)).saveAndFlush(any(IdempotencyRecord.class));

        ArgumentCaptor<Account> accountArgumentCaptor = ArgumentCaptor.forClass(Account.class);

        verify(accountRepository, times(2)).save(accountArgumentCaptor.capture());

        List<Account> savedAccounts = accountArgumentCaptor.getAllValues();

        Account savedFromAccount = null;
        Account savedToAccount = null;

        for (Account account : savedAccounts) {
            if (account.getId().equals(fromAccountId)) {
                savedFromAccount = account;
            } else if (account.getId().equals(toAccountId)) {
                savedToAccount = account;
            }
        }

        assertNotNull(savedFromAccount, "沒有捕獲到轉出帳戶");
        assertNotNull(savedToAccount, "沒有捕獲到轉入帳戶");

        assertEquals(
                0,
                new BigDecimal("800.00").compareTo(savedFromAccount.getBalance()),
                "轉出帳戶餘額錯誤"
        );

        assertEquals(
                0,
                new BigDecimal("700.00").compareTo(savedToAccount.getBalance()),
                "轉入帳戶餘額錯誤"
        );

        ArgumentCaptor<Transaction> transactionArgumentCaptor =
                ArgumentCaptor.forClass(Transaction.class);

        verify(transactionRepository, times(2)).save(transactionArgumentCaptor.capture());

        List<Transaction> savedTransactions = transactionArgumentCaptor.getAllValues();

        Transaction fromTransaction = null;
        Transaction toTransaction = null;

        for (Transaction transaction : savedTransactions) {
            if (transaction.getAccountId().equals(fromAccountId)) {
                fromTransaction = transaction;
            } else if (transaction.getAccountId().equals(toAccountId)) {
                toTransaction = transaction;
            }
        }

        assertNotNull(fromTransaction, "沒有捕獲到轉出交易紀錄");
        assertNotNull(toTransaction, "沒有捕獲到轉入交易紀錄");

        assertEquals(
                0,
                new BigDecimal("200.00").compareTo(fromTransaction.getAmount()),
                "轉出交易金額錯誤"
        );

        assertEquals(
                0,
                new BigDecimal("200.00").compareTo(toTransaction.getAmount()),
                "轉入交易金額錯誤"
        );

        assertEquals(TransactionType.TRANSFER_OUT, fromTransaction.getTransactionType(), "轉出交易類型錯誤");
        assertEquals(TransactionType.TRANSFER_IN, toTransaction.getTransactionType(), "轉入交易類型錯誤");
    }


}
