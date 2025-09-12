package net.javaguides.banking.service.impl;

import net.javaguides.banking.dto.AccountDto;
import net.javaguides.banking.dto.TransactionDTO;
import net.javaguides.banking.dto.TransferFundDTO;
import net.javaguides.banking.entity.Account;
import net.javaguides.banking.entity.Transaction;
import net.javaguides.banking.entity.User;
import net.javaguides.banking.enums.TransactionType;
import net.javaguides.banking.exception.AccountException;
import net.javaguides.banking.exception.AccountNotFoundException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * AccountServiceImpl 的單元測試類別。
 * @ExtendWith(MockitoExtension.class) 啟用 Mockito 擴充功能，讓我們可以使用 @Mock, @InjectMocks 等註解。
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    // @Mock: 建立一個模擬 (Mock) 物件。Mockito 會為這個介面或類別建立一個假實作。
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AccountMapper accountMapper;

    // @InjectMocks: 建立 AccountServiceImpl 的一個實例，並將上面用 @Mock 標記的模擬物件自動注入進去。
    @InjectMocks
    private AccountServiceImpl accountService;

    private User user;
    private Account account;
    private AccountDto accountDto;

    /**
     * @BeforeEach: 這個註解標記的方法會在每個測試方法執行前運行一次。
     * 非常適合用來初始化所有測試都會用到的通用物件。
     */
    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(1L);
        user.setUserName("testUser");

        account = new Account();
        account.setId(1L);
        account.setAccountHolderName("testUser");
        account.setBalance(new BigDecimal("1000.00"));
        account.setUser(user);
        account.setVersion(0L); // 初始化版本號

        accountDto = new AccountDto(1L, "testUser", new BigDecimal("1000.00"));
    }

    @DisplayName("測試 - 成功建立帳戶")
    @Test
    void testCreateAccount_Success() {
        // Arrange (準備)
        // 設定當 userRepository.findByUserName 被呼叫時，回傳我們準備好的 user 物件。
        given(userRepository.findByUserName(anyString())).willReturn(Optional.of(user));
        // 設定當 accountMapper.mapTOAccount 被呼叫時，回傳 account 物件（忽略 user 屬性，因為它在 service 中被設定）。
        given(accountMapper.mapTOAccount(accountDto)).willReturn(new Account(null, accountDto.accountHolderName(), accountDto.balance(), null));
        // 設定當 accountRepository.save 被呼叫時，回傳我們準備好的 account 物件。
        given(accountRepository.save(any(Account.class))).willReturn(account);
        // 設定當 accountMapper.mapTOAccountDto 被呼叫時，回傳 accountDto。
        given(accountMapper.mapTOAccountDto(account)).willReturn(accountDto);

        // Act (執行)
        AccountDto savedAccountDto = accountService.createAccount(accountDto);

        // Assert (斷言)
        assertNotNull(savedAccountDto);
        assertEquals("testUser", savedAccountDto.accountHolderName());
        // 驗證 accountRepository.save 方法確實被呼叫了 1 次。
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @DisplayName("測試 - 建立帳戶失敗 (使用者不存在)")
    @Test
    void testCreateAccount_UserNotFound_ThrowsException() {
        // Arrange
        // 設定當 userRepository.findByUserName 被呼叫時，回傳空 Optional，模擬找不到使用者的情況。
        given(userRepository.findByUserName(anyString())).willReturn(Optional.empty());

        // Act & Assert
        // 斷言當執行 createAccount 時，會拋出 RuntimeException。
        assertThrows(RuntimeException.class, () -> {
            accountService.createAccount(accountDto);
        });

        // 驗證 accountRepository.save 方法從未被呼叫。
        verify(accountRepository, never()).save(any(Account.class));
    }

    @DisplayName("測試 - 成功根據 ID 取得帳戶")
    @Test
    void testGetAccountById_Success() {
        // Arrange
        given(accountRepository.findById(1L)).willReturn(Optional.of(account));
        given(accountMapper.mapTOAccountDto(account)).willReturn(accountDto);

        // Act
        AccountDto foundAccountDto = accountService.getAccountById(1L);

        // Assert
        assertNotNull(foundAccountDto);
        assertEquals(1L, foundAccountDto.id());
    }

    @DisplayName("測試 - 根據 ID 取得帳戶失敗 (帳戶不存在)")
    @Test
    void testGetAccountById_NotFound_ThrowsException() {
        // Arrange
        given(accountRepository.findById(1L)).willReturn(Optional.empty());

        // Act & Assert
        assertThrows(AccountNotFoundException.class, () -> {
            accountService.getAccountById(1L);
        });
    }

    @DisplayName("測試 - 成功存款")
    @Test
    void testDeposit_Success() {
        // Arrange
        BigDecimal depositAmount = new BigDecimal("200.00");
        BigDecimal expectedBalance = new BigDecimal("1200.00");

        given(accountRepository.findById(1L)).willReturn(Optional.of(account));
        // 儲存後，account 物件的餘額會被更新。
        given(accountRepository.save(any(Account.class))).willAnswer(invocation -> invocation.getArgument(0));
        // 映射器應回傳更新後的 DTO。
        when(accountMapper.mapTOAccountDto(any(Account.class))).thenAnswer(invocation -> {
            Account savedAccount = invocation.getArgument(0);
            return new AccountDto(savedAccount.getId(), savedAccount.getAccountHolderName(), savedAccount.getBalance());
        });

        // Act
        AccountDto updatedAccountDto = accountService.deposit(1L, depositAmount);

        // Assert
        assertNotNull(updatedAccountDto);
        // 使用 compareTo 比較 BigDecimal 的值
        assertEquals(0, expectedBalance.compareTo(updatedAccountDto.balance()));
        verify(accountRepository, times(1)).save(account);
        // 驗證交易紀錄也被儲存了 1 次。
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @DisplayName("測試 - 成功提款")
    @Test
    void testWithdraw_Success() {
        // Arrange
        BigDecimal withdrawAmount = new BigDecimal("300.00");
        BigDecimal expectedBalance = new BigDecimal("700.00");

        given(accountRepository.findById(1L)).willReturn(Optional.of(account));
        given(accountRepository.save(any(Account.class))).willAnswer(invocation -> invocation.getArgument(0));
        when(accountMapper.mapTOAccountDto(any(Account.class))).thenAnswer(invocation -> {
            Account savedAccount = invocation.getArgument(0);
            return new AccountDto(savedAccount.getId(), savedAccount.getAccountHolderName(), savedAccount.getBalance());
        });

        // Act
        AccountDto updatedAccountDto = accountService.withdraw(1L, withdrawAmount);

        // Assert
        assertNotNull(updatedAccountDto);
        assertEquals(0, expectedBalance.compareTo(updatedAccountDto.balance()));
        verify(accountRepository, times(1)).save(account);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @DisplayName("測試 - 提款失敗 (餘額不足)")
    @Test
    void testWithdraw_InsufficientAmount_ThrowsException() {
        // Arrange
        BigDecimal withdrawAmount = new BigDecimal("1500.00"); // 比帳戶餘額 1000.00 多
        given(accountRepository.findById(1L)).willReturn(Optional.of(account));

        // Act & Assert
        assertThrows(InsufficientAmountException.class, () -> {
            accountService.withdraw(1L, withdrawAmount);
        });

        // 驗證因為餘額不足，save 操作從未被執行。
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @DisplayName("測試 - 成功刪除帳戶")
    @Test
    void testDeleteAccount_Success() {
        // Arrange
        given(accountRepository.findById(1L)).willReturn(Optional.of(account));
        // 對於回傳 void 的方法，我們可以使用 doNothing() 來設定模擬行為。
        doNothing().when(accountRepository).deleteById(1L);

        // Act
        accountService.deleteAccount(1L);

        // Assert
        // 驗證 deleteById 方法確實被帶有正確 ID 的參數呼叫了 1 次。
        verify(accountRepository, times(1)).deleteById(1L);
    }

    @DisplayName("測試 - 刪除帳戶失敗 (帳戶不存在)")
    @Test
    void testDeleteAccount_NotFound_ThrowsException() {
        // Arrange
        given(accountRepository.findById(1L)).willReturn(Optional.empty());

        // Act & Assert
        assertThrows(AccountNotFoundException.class, () -> {
            accountService.deleteAccount(1L);
        });

        // 驗證因為找不到帳戶，deleteById 從未被呼叫。
        verify(accountRepository, never()).deleteById(anyLong());
    }

    @DisplayName("測試 - 成功轉帳")
    @Test
    void testTransferFunds_Success() {
        // Arrange
        Long fromAccountId = 1L;
        Long toAccountId = 2L;
        BigDecimal transferAmount = new BigDecimal("500.00");

        Account fromAccount = account; // 初始餘額 1000
        Account toAccount = new Account();
        toAccount.setId(toAccountId);
        toAccount.setAccountHolderName("toUser");
        toAccount.setBalance(new BigDecimal("500.00"));

        TransferFundDTO transferFundDTO = new TransferFundDTO(fromAccountId, toAccountId, transferAmount);

        // 模擬悲觀鎖的查詢
        given(accountRepository.findByIdForUpdate(fromAccountId)).willReturn(Optional.of(fromAccount));
        given(accountRepository.findByIdForUpdate(toAccountId)).willReturn(Optional.of(toAccount));

        // Act
        accountService.transferFunds(transferFundDTO);

        // Assert
        // 驗證兩個帳戶的 save 方法都被呼叫了
        verify(accountRepository, times(1)).save(fromAccount);
        verify(accountRepository, times(1)).save(toAccount);

        // 驗證轉出帳戶的餘額是否正確
        assertEquals(0, new BigDecimal("500.00").compareTo(fromAccount.getBalance()));
        // 驗證轉入帳戶的餘額是否正確
        assertEquals(0, new BigDecimal("1000.00").compareTo(toAccount.getBalance()));

        // 使用 ArgumentCaptor 來捕獲傳遞給 transactionRepository.save 的參數
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(transactionCaptor.capture());

        List<Transaction> capturedTransactions = transactionCaptor.getAllValues();
        // 驗證產生了兩筆交易紀錄
        assertEquals(2, capturedTransactions.size());

        // 檢查一筆是 TRANSFER_OUT，另一筆是 TRANSFER_IN
        assertTrue(capturedTransactions.stream().anyMatch(t -> t.getTransactionType() == TransactionType.TRANSFER_OUT && t.getAccountId().equals(fromAccountId)));
        assertTrue(capturedTransactions.stream().anyMatch(t -> t.getTransactionType() == TransactionType.TRANSFER_IN && t.getAccountId().equals(toAccountId)));
    }

    @DisplayName("測試 - 轉帳失敗 (轉出帳戶餘額不足)")
    @Test
    void testTransferFunds_InsufficientAmount_ThrowsException() {
        // Arrange
        Long fromAccountId = 1L;
        Long toAccountId = 2L;
        BigDecimal transferAmount = new BigDecimal("2000.00"); // 超出餘額

        Account fromAccount = account; // 初始餘額 1000
        Account toAccount = new Account();
        toAccount.setId(toAccountId);

        TransferFundDTO transferFundDTO = new TransferFundDTO(fromAccountId, toAccountId, transferAmount);

        given(accountRepository.findByIdForUpdate(fromAccountId)).willReturn(Optional.of(fromAccount));
        given(accountRepository.findByIdForUpdate(toAccountId)).willReturn(Optional.of(toAccount));

        // Act & Assert
        assertThrows(InsufficientAmountException.class, () -> {
            accountService.transferFunds(transferFundDTO);
        });

        // 驗證因為餘額不足，沒有任何帳戶或交易被儲存
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @DisplayName("測試 - 轉帳失敗 (轉帳到相同帳戶)")
    @Test
    void testTransferFunds_ToSameAccount_ThrowsException() {
        // Arrange
        TransferFundDTO transferFundDTO = new TransferFundDTO(1L, 1L, new BigDecimal("100.00"));

        // Act & Assert
        AccountException exception = assertThrows(AccountException.class, () -> {
            accountService.transferFunds(transferFundDTO);
        });

        assertEquals("不能轉帳到相同帳戶", exception.getMessage());

        // 驗證沒有任何資料庫互動
        verify(accountRepository, never()).findByIdForUpdate(anyLong());
    }

    @DisplayName("測試 - 成功取得帳戶交易紀錄 (分頁)")
    @Test
    void testGetAccountTransactions_Success() {
        // Arrange
        Long accountId = 1L;
        Pageable pageable = Pageable.ofSize(10);

        Transaction transaction = new Transaction(1L, accountId, new BigDecimal("100"), TransactionType.DEPOSIT, LocalDateTime.now());
        Page<Transaction> transactionPage = new PageImpl<>(Collections.singletonList(transaction));

        TransactionDTO transactionDTO = new TransactionDTO(1L, accountId, new BigDecimal("100"), TransactionType.DEPOSIT, transaction.getTimestamp());

        given(transactionRepository.findByAccountIdOrderByTimestampDesc(accountId, pageable)).willReturn(transactionPage);

        // Act
        Page<TransactionDTO> resultPage = accountService.getAccountTransactions(accountId, pageable);

        // Assert
        assertNotNull(resultPage);
        assertEquals(1, resultPage.getTotalElements());
        assertEquals(transaction.getAmount(), resultPage.getContent().get(0).amount());
        verify(transactionRepository, times(1)).findByAccountIdOrderByTimestampDesc(accountId, pageable);
    }
}