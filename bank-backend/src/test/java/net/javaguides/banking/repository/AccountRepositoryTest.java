package net.javaguides.banking.repository;

import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import net.javaguides.banking.entity.Account;
import net.javaguides.banking.entity.User;
import net.javaguides.banking.exception.AccountNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@DataJpaTest
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("測試-儲存 Account 並依 ID 查詢成功")
    void save_and_findById_success() {

        // Arrange
        User user = new User();
        user.setUsername("Tom");
        user.setEmail("tom@test.com");
        user.setRealName("Tom");

        entityManager.persist(user);
        entityManager.flush();

        Account account = new Account();
        account.setAccountHolderName("Tom");
        account.setBalance(new BigDecimal("1000.00"));
        account.setUser(user);

        // Act
        accountRepository.save(account);

        entityManager.flush();
        entityManager.clear();

        Account savedAccount = accountRepository.findById(account.getId())
                .orElseThrow();

        // Assert
        assertEquals(
                "Tom",
                savedAccount.getAccountHolderName()
        );

        assertEquals(
                0,
                new BigDecimal("1000.00")
                        .compareTo(savedAccount.getBalance())
        );

        assertEquals(
                user.getUserId(),
                savedAccount.getUser().getUserId()
        );
    }

    @Test
    @DisplayName("測試-依 User ID 查詢所有 Account 成功")
    void findAllByUserUserId_success() {

        // =========================
        // Arrange
        // =========================

        // 建立 User A
        User userA = new User();
        userA.setUsername("Tom");
        userA.setEmail("tom@test.com");
        userA.setRealName("Tom");

        entityManager.persist(userA);
        entityManager.flush();

        Long userAId = userA.getUserId();

        assertNotNull(userAId);


        // 建立 User B
        User userB = new User();
        userB.setUsername("John");
        userB.setEmail("john@test.com");
        userB.setRealName("John");

        entityManager.persist(userB);
        entityManager.flush();

        Long userBId = userB.getUserId();

        assertNotNull(userBId);


        // 建立 User A 的第一個帳戶
        Account accountA1 = new Account();
        accountA1.setAccountHolderName("Tom Account 1");
        accountA1.setBalance(new BigDecimal("1000.00"));
        accountA1.setUser(userA);


        // 建立 User A 的第二個帳戶
        Account accountA2 = new Account();
        accountA2.setAccountHolderName("Tom Account 2");
        accountA2.setBalance(new BigDecimal("2000.00"));
        accountA2.setUser(userA);


        // 建立 User B 的帳戶
        Account accountB1 = new Account();
        accountB1.setAccountHolderName("John Account 1");
        accountB1.setBalance(new BigDecimal("3000.00"));
        accountB1.setUser(userB);


        // 儲存 Account
        accountRepository.save(accountA1);
        accountRepository.save(accountA2);
        accountRepository.save(accountB1);

        entityManager.flush();

        // 清除 Persistence Context
        // 確保接下來的查詢是重新從 DB 取得資料
        entityManager.clear();


        // =========================
        // Act
        // =========================

        List<Account> accounts =
                accountRepository.findAllByUserUserId(userAId);


        // =========================
        // Assert
        // =========================

        // User A 應該有 2 個 Account
        assertEquals(2, accounts.size());


        // 確認所有查詢結果都屬於 User A
        assertTrue(accounts.stream()
                .allMatch(account ->
                        account.getUser().getUserId()
                                .equals(userAId)
                ));


        // 確認沒有查到 User B 的 Account
        assertFalse(accounts.stream()
                .anyMatch(account ->
                        account.getUser().getUserId()
                                .equals(userBId)
                ));


        // 確認第一個 Account 存在
        assertTrue(accounts.stream()
                .anyMatch(account ->
                        account.getAccountHolderName()
                                .equals("Tom Account 1")
                ));


        // 確認第二個 Account 存在
        assertTrue(accounts.stream()
                .anyMatch(account ->
                        account.getAccountHolderName()
                                .equals("Tom Account 2")
                ));


        // 確認 User B 的 Account 不存在
        assertFalse(accounts.stream()
                .anyMatch(account ->
                        account.getAccountHolderName()
                                .equals("John Account 1")
                ));
    }

    @Test
    @DisplayName("測試-依不存在的 User ID 查詢 Account，應回傳空 List")
    void findAllByUserUserId_noResult() {

        // =========================
        // Arrange
        // =========================

        // 建立一個不存在於資料庫中的 User ID
        Long nonExistingUserId = 999999L;

        // =========================
        // Act
        // =========================

        List<Account> accounts =
                accountRepository.findAllByUserUserId(nonExistingUserId);

        // =========================
        // Assert
        // =========================

        // 查詢結果不應該是 null
        assertNotNull(accounts);

        // 不應該查到任何 Account
        assertTrue(accounts.isEmpty());
    }

    @Test
    @DisplayName("測試-Pagination 分頁查詢 Account 成功")
    void pagination_success() {

        // =========================
        // Arrange
        // =========================

        // 建立 User
        User user = new User();
        user.setUsername("Tom");
        user.setEmail("tom@test.com");
        user.setRealName("Tom");

        entityManager.persist(user);
        entityManager.flush();

        // 建立 5 個 Account
        for (int i = 1; i <= 5; i++) {

            Account account = new Account();
            account.setAccountHolderName("Tom Account " + i);
            account.setBalance(new BigDecimal(i + "000.00"));
            account.setUser(user);

            accountRepository.save(account);
        }

        entityManager.flush();
        entityManager.clear();


        // =========================
        // Act
        // =========================

        // page = 0
        // size = 2
        Pageable pageable = PageRequest.of(
                0,
                2,
                Sort.by("id").ascending()
        );

        Page<Account> accountPage =
                accountRepository.findAll(pageable);


        // =========================
        // Assert
        // =========================

        // 這一頁應該只有 2 筆
        assertEquals(2, accountPage.getContent().size());

        // 總共有 5 筆
        assertEquals(5, accountPage.getTotalElements());

        // 5 筆、每頁 2 筆 => 總共 3 頁
        assertEquals(3, accountPage.getTotalPages());

        // 現在是第 0 頁
        assertEquals(0, accountPage.getNumber());

        // 每頁大小為 2
        assertEquals(2, accountPage.getSize());

        // 因為 5 筆資料，第 0 頁後面還有資料
        assertTrue(accountPage.hasNext());

        // 第 0 頁不是最後一頁
        assertFalse(accountPage.isLast());

        // 確認第一頁內容
        assertEquals(
                "Tom Account 1",
                accountPage.getContent().get(0).getAccountHolderName()
        );

        assertEquals(
                "Tom Account 2",
                accountPage.getContent().get(1).getAccountHolderName()
        );
    }
}