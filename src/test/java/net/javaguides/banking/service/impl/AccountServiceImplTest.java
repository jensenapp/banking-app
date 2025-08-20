// file: src/test/java/net/javaguides/banking/service/impl/AccountServiceImplTest.java
package net.javaguides.banking.service.impl;

import net.javaguides.banking.dto.AccountDto;
import net.javaguides.banking.dto.TransferFundDTO;
import net.javaguides.banking.entity.Account;
import net.javaguides.banking.exception.AccountException;
import net.javaguides.banking.exception.AccountNotFoundException;
import net.javaguides.banking.exception.InsufficientAmountException;
import net.javaguides.banking.mapper.AccountMapper;
import net.javaguides.banking.repository.AccountRepository;
import net.javaguides.banking.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class) 是 JUnit 5 的註解，用於整合 Mockito 框架。
// 這使得我們可以在測試類別中使用 @Mock、@InjectMocks 等 Mockito 註解。
@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    // @Mock 註解用於建立一個模擬(Mock)物件。
    // 在單元測試中，我們不希望實際與資料庫互動，所以我們模擬 Repository 層。
    // Mockito 會攔截所有對這個物件的方法呼叫。
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private TransactionRepository transactionRepository;

    // @InjectMocks 註解會建立 AccountServiceImpl 的一個實例，
    // 並且會自動將標有 @Mock 的模擬物件 (accountRepository 和 transactionRepository) 注入到這個實例中。
    // 這樣我們就可以測試 accountService 的邏輯，而它的依賴項都已經被模擬物件取代。
    @InjectMocks
    private AccountServiceImpl accountService;

    // 定義一些在多個測試中都會用到的共用變數。
    private Account account;
    private AccountDto accountDto;

    // @BeforeEach 註解表示這個 setUp() 方法會在 "每一個" 測試方法 (@Test) 執行之前都執行一次。
    // 這確保了每個測試案例都是在一個乾淨、獨立的環境下開始，不會被其他測試案例的結果影響。
    @BeforeEach
    void setUp() {
        // 準備一個 Account 實體作為測試資料
        account = new Account(1L, "John Doe", new BigDecimal("10000"),null);
        // 將 Account 實體轉換為 AccountDto
        accountDto = accountMapper.mapTOAccountDto(account);
    }

    // @DisplayName 為測試案例提供一個可讀性更高的名稱，會顯示在測試報告中。
    @DisplayName("測試建立新帳戶 - 成功案例")
    @Test
    void givenAccountDto_whenCreateAccount_thenReturnAccountDto() {
        // AAA 測試模式: Arrange, Act, Assert (或 Given, When, Then)

        // Given (給定) - 設定測試的前提條件，主要是設定模擬物件的行為。
        // given(...).willReturn(...) 是 Mockito 的語法。
        // 這行程式碼的意思是：當 accountRepository 的 save 方法被呼叫，且傳入的參數是任何 Account 物件 (any(Account.class)) 時，
        // 就回傳我們預先準備好的 account 物件。
        given(accountRepository.save(any(Account.class))).willReturn(account);

        // When (當) - 執行我們要測試的程式碼，也就是呼叫 service 的方法。
        AccountDto savedAccountDto = accountService.createAccount(accountDto);

        // Then (則) - 驗證執行的結果是否符合預期。
        // assertNotNull 斷言回傳的物件不應該是 null。
        assertNotNull(savedAccountDto);
        // assertEquals 斷言回傳的帳戶持有人姓名應該是 "John Doe"。
        assertEquals("John Doe", savedAccountDto.accountHolderName());
        // verify 是 Mockito 的行為驗證語法。
        // 這行程式碼驗證 accountRepository 的 save 方法是否被以正確的方式呼叫。
        // times(1) 表示我們預期它被呼叫了 "恰好一次"。
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @DisplayName("測試依 ID 查詢帳戶 - 成功案例")
    @Test
    void givenAccountId_whenGetAccountById_thenReturnAccountDto() {
        // Given - 設定前提：當 findById 方法被呼叫且參數為 1L 時，
        // 回傳一個包含我們預設 account 物件的 Optional (模擬成功找到帳戶)。
        given(accountRepository.findById(1L)).willReturn(Optional.of(account));

        // When - 呼叫 getAccountById 方法。
        AccountDto foundAccountDto = accountService.getAccountById(1L);

        // Then - 驗證結果。
        assertNotNull(foundAccountDto);
        assertEquals(1L, foundAccountDto.id());
    }

    @DisplayName("測試依 ID 查詢帳戶 - 帳戶不存在案例")
    @Test
    void givenNonExistentAccountId_whenGetAccountById_thenThrowAccountNotFoundException() {
        // Given - 設定前提：當 findById 方法被呼叫且參數是任何 long 型別時 (anyLong())，
        // 回傳一個空的 Optional (模擬找不到帳戶)。
        given(accountRepository.findById(anyLong())).willReturn(Optional.empty());

        // When & Then - 執行並驗證。
        // assertThrows 用於驗證特定的程式碼區塊是否會拋出預期的例外。
        // 我們預期呼叫 getAccountById(99L) 時，會拋出 AccountNotFoundException。
        assertThrows(AccountNotFoundException.class, () -> {
            accountService.getAccountById(99L);
        });

        // 驗證 findById 方法確實被呼叫了一次，且傳入的參數是 99L。
        verify(accountRepository, times(1)).findById(99L);
    }

    @DisplayName("測試存款功能 - 成功案例")
    @Test
    void givenAccountIdAndAmount_whenDeposit_thenReturnUpdatedAccountDto() {
        // Given
        Long accountId = 1L;
        BigDecimal depositAmount = new BigDecimal("500");
        // 計算預期的存款後餘額
        BigDecimal expectedBalance = account.getBalance().add(depositAmount);

        // 模擬：當 findById 被呼叫時，回傳我們的測試帳戶。
        given(accountRepository.findById(accountId)).willReturn(Optional.of(account));
        // 模擬：當 save 方法被呼叫時，直接回傳傳入的那個 Account 物件。
        // 這讓我們可以驗證物件在 service 層中被修改後的狀態。
        given(accountRepository.save(any(Account.class))).willAnswer(invocation -> invocation.getArgument(0));

        // When - 執行存款操作。
        AccountDto updatedAccountDto = accountService.deposit(accountId, depositAmount);

        // Then - 驗證結果。
        assertNotNull(updatedAccountDto);
        // 斷言更新後的餘額是否與預期相符。
        assertEquals(expectedBalance, updatedAccountDto.balance());
        // 驗證 findById 和 save 方法都被呼叫了一次。
        verify(accountRepository, times(1)).findById(accountId);
        verify(accountRepository, times(1)).save(any(Account.class));
        // 驗證 transactionRepository 的 save 方法也被呼叫了一次，確保交易紀錄有被儲存。
        verify(transactionRepository, times(1)).save(any());
    }

    @DisplayName("測試提款功能 - 成功案例")
    @Test
    void givenAccountIdAndAmount_whenWithdraw_thenReturnUpdatedAccountDto() {
        // Given
        Long accountId = 1L;
        BigDecimal withdrawAmount = new BigDecimal("500");
        BigDecimal expectedBalance = account.getBalance().subtract(withdrawAmount);

        given(accountRepository.findById(accountId)).willReturn(Optional.of(account));
        given(accountRepository.save(any(Account.class))).willAnswer(invocation -> invocation.getArgument(0));

        // When
        AccountDto updatedAccountDto = accountService.withdraw(accountId, withdrawAmount);

        // Then
        assertNotNull(updatedAccountDto);
        assertEquals(expectedBalance, updatedAccountDto.balance());
    }

    @DisplayName("測試提款功能 - 餘額不足案例")
    @Test
    void givenAccountIdAndAmount_whenWithdrawWithInsufficientBalance_thenThrowInsufficientAmountException() {
        // Given - 準備一個比帳戶餘額還大的提款金額。
        Long accountId = 1L;
        BigDecimal withdrawAmount = new BigDecimal("20000"); // 20000 > 10000

        // 模擬：可以找到帳戶。
        given(accountRepository.findById(accountId)).willReturn(Optional.of(account));

        // When & Then - 斷言執行提款操作時，會拋出 InsufficientAmountException。
        assertThrows(InsufficientAmountException.class, () -> {
            accountService.withdraw(accountId, withdrawAmount);
        });

        // 重要驗證：使用 never() 來確保 save 方法 "從未" 被呼叫。
        // 因為在餘額不足的情況下，程式應該在拋出例外後就停止，不應該去更新資料庫。
        verify(accountRepository, never()).save(any(Account.class));
    }

    @DisplayName("測試刪除帳戶 - 成功案例")
    @Test
    void givenAccountId_whenDeleteAccount_thenDeleteSuccessfully() {
        // Given
        Long accountId = 1L;
        // 先模擬能找到要刪除的帳戶。
        given(accountRepository.findById(accountId)).willReturn(Optional.of(account));
        // 對於沒有回傳值 (void) 的方法，我們使用 doNothing().when(...) 的語法。
        // 這裡設定：當 deleteById 被呼叫時，什麼事都不做 (因為它是模擬物件)。
        doNothing().when(accountRepository).deleteById(accountId);

        // When - 執行刪除操作。
        accountService.deleteAccount(accountId);

        // Then - 驗證 deleteById 方法確實被呼叫了一次。
        verify(accountRepository, times(1)).deleteById(accountId);
    }

    @DisplayName("測試轉帳功能 - 成功案例")
    @Test
    void givenTransferFundDto_whenTransferFunds_thenCompleteSuccessfully() {
        // Given - 準備轉出方、轉入方帳戶及轉帳金額。
        Long fromAccountId = 1L;
        Long toAccountId = 2L;
        BigDecimal amount = new BigDecimal("1000");

        Account fromAccount = new Account(fromAccountId, "Sender", new BigDecimal("5000"),null);
        Account toAccount = new Account(toAccountId, "Receiver", new BigDecimal("2000"),null);

        TransferFundDTO transferFundDTO = new TransferFundDTO(fromAccountId, toAccountId, amount);

        // 模擬：當使用悲觀鎖查詢時，能找到對應的帳戶。
        given(accountRepository.findByIdForUpdate(fromAccountId)).willReturn(Optional.of(fromAccount));
        given(accountRepository.findByIdForUpdate(toAccountId)).willReturn(Optional.of(toAccount));

        // When - 執行轉帳操作。
        accountService.transferFunds(transferFundDTO);

        // Then - 驗證轉帳後的結果。
        // ArgumentCaptor 是一個強大的工具，用於捕獲傳遞給模擬物件方法的 "實際參數"。
        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);

        // 驗證 save 方法被呼叫了 "兩次" (一次轉出方，一次轉入方)，並捕獲這兩次呼叫時傳入的 Account 物件。
        verify(accountRepository, times(2)).save(accountCaptor.capture());

        // 從捕獲的參數列表中，找出轉出方和轉入方的 Account 物件。
        Account savedFromAccount = accountCaptor.getAllValues().stream().filter(a -> a.getId().equals(fromAccountId)).findFirst().get();
        Account savedToAccount = accountCaptor.getAllValues().stream().filter(a -> a.getId().equals(toAccountId)).findFirst().get();

        // 斷言轉出方和轉入方的餘額是否正確更新。
        assertEquals(new BigDecimal("4000"), savedFromAccount.getBalance()); // 5000 - 1000
        assertEquals(new BigDecimal("3000"), savedToAccount.getBalance()); // 2000 + 1000

        // 驗證交易紀錄也被儲存了兩次。
        verify(transactionRepository, times(2)).save(any());
    }

    @DisplayName("測試轉帳功能 - 轉入相同帳戶案例")
    @Test
    void givenTransferToSameAccount_whenTransferFunds_thenThrowAccountException() {
        // Given - 準備一個轉出方和轉入方 ID 相同的轉帳請求。
        Long accountId = 1L;
        TransferFundDTO transferFundDTO = new TransferFundDTO(accountId, accountId, new BigDecimal("100"));

        // When & Then - 斷言當執行這個轉帳操作時，會拋出我們自訂的 AccountException。
        assertThrows(AccountException.class, () -> {
            accountService.transferFunds(transferFundDTO);
        });

        // 驗證在這種錯誤情況下，save 方法從未被呼叫。
        verify(accountRepository, never()).save(any());
    }
}