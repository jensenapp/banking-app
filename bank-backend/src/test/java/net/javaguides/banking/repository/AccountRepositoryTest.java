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
}