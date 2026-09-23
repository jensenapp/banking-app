package net.javaguides.banking;

import net.javaguides.banking.dto.TransferFundDTO;
import net.javaguides.banking.entity.Account;
import net.javaguides.banking.entity.User;
import net.javaguides.banking.repository.AccountRepository;
import net.javaguides.banking.repository.IdempotencyRepository;
import net.javaguides.banking.repository.TransactionRepository;
import net.javaguides.banking.repository.UserRepository;
import net.javaguides.banking.service.AccountService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class ConcurrentTransferTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IdempotencyRepository idempotencyRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {

        idempotencyRepository.deleteAll();
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Step 6 - 10 Threads Concurrent Transfer")
    void concurrentTransfer_tenThreads() throws Exception {

        // =========================
        // Arrange
        // =========================

        int threadCount = 10;

        BigDecimal transferAmount =
                new BigDecimal("1000.00");


        User user = new User();

        user.setUsername("concurrentUser");
        user.setEmail("concurrent@test.com");
        user.setRealName("Concurrent User");

        userRepository.save(user);


        Account fromAccount = new Account();

        fromAccount.setAccountHolderName("Account A");
        fromAccount.setBalance(
                new BigDecimal("100000.00")
        );
        fromAccount.setUser(user);

        accountRepository.save(fromAccount);


        Account toAccount = new Account();

        toAccount.setAccountHolderName("Account B");
        toAccount.setBalance(
                new BigDecimal("50000.00")
        );
        toAccount.setUser(user);

        accountRepository.save(toAccount);


        Long fromAccountId =
                fromAccount.getId();

        Long toAccountId =
                toAccount.getId();


        // =========================
        // Concurrent Setup
        // =========================

        ExecutorService executor =
                Executors.newFixedThreadPool(
                        threadCount
                );

        CountDownLatch startLatch =
                new CountDownLatch(1);

        CountDownLatch finishLatch =
                new CountDownLatch(threadCount);


        try {

            // =========================
            // 建立 10 個 Thread
            // =========================

            for (int i = 0; i < threadCount; i++) {

                final int threadNumber = i;

                executor.submit(() -> {

                    try {

                        // 等待所有 Thread
                        // 一起開始
                        startLatch.await();


                        System.out.println(
                                "Thread "
                                        + threadNumber
                                        + " 開始轉帳"
                        );


                        TransferFundDTO transfer =
                                new TransferFundDTO(
                                        fromAccountId,
                                        toAccountId,
                                        transferAmount,
                                        "concurrent-key-"
                                                + threadNumber
                                );


                        accountService.transferFunds(
                                transfer
                        );


                        System.out.println(
                                "Thread "
                                        + threadNumber
                                        + " 完成"
                        );

                    } catch (Exception e) {

                        e.printStackTrace();

                    } finally {

                        finishLatch.countDown();
                    }
                });
            }


            // =========================
            // Start
            // =========================

            System.out.println(
                    "主 Thread："
                            + threadCount
                            + " 個 Thread 準備開始"
            );


            startLatch.countDown();


            // 等待全部完成
            finishLatch.await();


            // =========================
            // Verify Account Balance
            // =========================

            Account actualFromAccount =
                    accountRepository
                            .findById(fromAccountId)
                            .orElseThrow();


            Account actualToAccount =
                    accountRepository
                            .findById(toAccountId)
                            .orElseThrow();


            // A:
            // 100000 - (10 × 1000)
            assertEquals(
                    new BigDecimal("90000.00"),
                    actualFromAccount.getBalance()
            );


            // B:
            // 50000 + (10 × 1000)
            assertEquals(
                    new BigDecimal("60000.00"),
                    actualToAccount.getBalance()
            );


            // =========================
            // Verify Transactions
            // =========================

            long transactionCount =
                    transactionRepository.count();


            // 10 transfers × 2 transactions
            assertEquals(
                    20,
                    transactionCount
            );

        } finally {

            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Step 5 - 反向 Concurrent Transfer")
    void concurrentTransfer_reverseDirection() throws Exception {

        // =========================
        // Arrange
        // =========================

        User user = new User();
        user.setUsername("concurrentUser");
        user.setEmail("concurrent@test.com");
        user.setRealName("Concurrent User");

        userRepository.save(user);


        Account accountA = new Account();
        accountA.setAccountHolderName("Account A");
        accountA.setBalance(new BigDecimal("10000.00"));
        accountA.setUser(user);

        accountRepository.save(accountA);


        Account accountB = new Account();
        accountB.setAccountHolderName("Account B");
        accountB.setBalance(new BigDecimal("5000.00"));
        accountB.setUser(user);

        accountRepository.save(accountB);


        Long accountAId = accountA.getId();
        Long accountBId = accountB.getId();


        // =========================
        // Concurrent Setup
        // =========================

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch startLatch =
                new CountDownLatch(1);

        CountDownLatch finishLatch =
                new CountDownLatch(2);


        try {

            // =========================
            // Thread 1
            // A → B
            // =========================

            executor.submit(() -> {

                try {

                    startLatch.await();

                    System.out.println(
                            "Thread 1：A → B $1000"
                    );

                    TransferFundDTO transfer =
                            new TransferFundDTO(
                                    accountAId,
                                    accountBId,
                                    new BigDecimal("1000.00"),
                                    "reverse-key-1"
                            );

                    accountService.transferFunds(transfer);

                    System.out.println(
                            "Thread 1：完成"
                    );

                } catch (Exception e) {

                    e.printStackTrace();

                } finally {

                    finishLatch.countDown();
                }
            });


            // =========================
            // Thread 2
            // B → A
            // =========================

            executor.submit(() -> {

                try {

                    startLatch.await();

                    System.out.println(
                            "Thread 2：B → A $500"
                    );

                    TransferFundDTO transfer =
                            new TransferFundDTO(
                                    accountBId,
                                    accountAId,
                                    new BigDecimal("500.00"),
                                    "reverse-key-2"
                            );

                    accountService.transferFunds(transfer);

                    System.out.println(
                            "Thread 2：完成"
                    );

                } catch (Exception e) {

                    e.printStackTrace();

                } finally {

                    finishLatch.countDown();
                }
            });


            // =========================
            // Start
            // =========================

            System.out.println(
                    "主 Thread：同時開始兩個轉帳"
            );

            startLatch.countDown();

            finishLatch.await();


            // =========================
            // Verify Account Balance
            // =========================

            Account actualA =
                    accountRepository.findById(accountAId)
                            .orElseThrow();

            Account actualB =
                    accountRepository.findById(accountBId)
                            .orElseThrow();


            assertEquals(
                    new BigDecimal("9500.00"),
                    actualA.getBalance()
            );

            assertEquals(
                    new BigDecimal("5500.00"),
                    actualB.getBalance()
            );


            // =========================
            // Verify Transactions
            // =========================

            long transactionCount =
                    transactionRepository.count();

            assertEquals(
                    4,
                    transactionCount
            );

        } finally {

            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Step 4 - Concurrent Transfer 驗證 Transaction 數量")
    void concurrentTransfer_transactionRecords() throws Exception {

        // =========================
        // Arrange
        // =========================

        User user = new User();
        user.setUsername("concurrentUser");
        user.setEmail("concurrent@test.com");
        user.setRealName("Concurrent User");

        userRepository.save(user);


        Account fromAccount = new Account();
        fromAccount.setAccountHolderName("Account A");
        fromAccount.setBalance(new BigDecimal("10000.00"));
        fromAccount.setUser(user);

        accountRepository.save(fromAccount);


        Account toAccount = new Account();
        toAccount.setAccountHolderName("Account B");
        toAccount.setBalance(new BigDecimal("5000.00"));
        toAccount.setUser(user);

        accountRepository.save(toAccount);


        Long fromAccountId = fromAccount.getId();
        Long toAccountId = toAccount.getId();


        // =========================
        // Concurrent Setup
        // =========================

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch startLatch =
                new CountDownLatch(1);

        CountDownLatch finishLatch =
                new CountDownLatch(2);


        try {

            // =========================
            // Thread 1
            // =========================

            executor.submit(() -> {

                try {

                    startLatch.await();

                    TransferFundDTO transfer =
                            new TransferFundDTO(
                                    fromAccountId,
                                    toAccountId,
                                    new BigDecimal("1000.00"),
                                    "concurrent-key-1"
                            );

                    accountService.transferFunds(transfer);

                } catch (Exception e) {

                    e.printStackTrace();

                } finally {

                    finishLatch.countDown();
                }
            });


            // =========================
            // Thread 2
            // =========================

            executor.submit(() -> {

                try {

                    startLatch.await();

                    TransferFundDTO transfer =
                            new TransferFundDTO(
                                    fromAccountId,
                                    toAccountId,
                                    new BigDecimal("2000.00"),
                                    "concurrent-key-2"
                            );

                    accountService.transferFunds(transfer);

                } catch (Exception e) {

                    e.printStackTrace();

                } finally {

                    finishLatch.countDown();
                }
            });


            // =========================
            // Start
            // =========================

            startLatch.countDown();

            finishLatch.await();


            // =========================
            // Step 3
            // 驗證最終 Account Balance
            // =========================

            Account actualFromAccount =
                    accountRepository.findById(fromAccountId)
                            .orElseThrow();

            Account actualToAccount =
                    accountRepository.findById(toAccountId)
                            .orElseThrow();


            assertEquals(
                    new BigDecimal("7000.00"),
                    actualFromAccount.getBalance()
            );

            assertEquals(
                    new BigDecimal("8000.00"),
                    actualToAccount.getBalance()
            );


            // =========================
            // Step 4
            // 驗證 Transaction 數量
            // =========================

            long transactionCount =
                    transactionRepository.count();

            assertEquals(
                    4,
                    transactionCount,
                    "兩筆轉帳，每筆應產生 2 筆 Transaction"
            );

        } finally {

            executor.shutdown();
        }
    }


    @Test
    @DisplayName("Step 3 - Concurrent Transfer 後餘額正確")
    void concurrentTransfer_balanceShouldBeCorrect() throws Exception {

        // =====================================================
        // Arrange
        // =====================================================

        // 建立 User
        User user = new User();
        user.setUsername("concurrentUser");
        user.setEmail("concurrent@test.com");
        user.setRealName("Concurrent User");

        userRepository.save(user);


        // 建立 Account A
        Account fromAccount = new Account();
        fromAccount.setAccountHolderName("Account A");
        fromAccount.setBalance(new BigDecimal("10000.00"));
        fromAccount.setUser(user);

        accountRepository.save(fromAccount);


        // 建立 Account B
        Account toAccount = new Account();
        toAccount.setAccountHolderName("Account B");
        toAccount.setBalance(new BigDecimal("5000.00"));
        toAccount.setUser(user);

        accountRepository.save(toAccount);


        Long fromAccountId = fromAccount.getId();
        Long toAccountId = toAccount.getId();


        // =====================================================
        // 建立 Thread Pool
        // =====================================================

        ExecutorService executor =
                Executors.newFixedThreadPool(2);


        // 控制兩個 Thread 同時開始
        CountDownLatch startLatch =
                new CountDownLatch(1);


        // 等待兩個 Thread 完成
        CountDownLatch finishLatch =
                new CountDownLatch(2);


        try {

            // =================================================
            // Thread 1
            // =================================================

            executor.submit(() -> {

                try {

                    startLatch.await();

                    System.out.println(
                            "Thread 1 開始轉帳：1000"
                    );


                    TransferFundDTO transfer =
                            new TransferFundDTO(
                                    fromAccountId,
                                    toAccountId,
                                    new BigDecimal("1000.00"),
                                    "concurrent-key-1"
                            );


                    accountService.transferFunds(transfer);


                    System.out.println(
                            "Thread 1 轉帳完成"
                    );

                } catch (Exception e) {

                    e.printStackTrace();

                } finally {

                    finishLatch.countDown();
                }
            });


            // =================================================
            // Thread 2
            // =================================================

            executor.submit(() -> {

                try {

                    startLatch.await();

                    System.out.println(
                            "Thread 2 開始轉帳：2000"
                    );


                    TransferFundDTO transfer =
                            new TransferFundDTO(
                                    fromAccountId,
                                    toAccountId,
                                    new BigDecimal("2000.00"),
                                    "concurrent-key-2"
                            );


                    accountService.transferFunds(transfer);


                    System.out.println(
                            "Thread 2 轉帳完成"
                    );

                } catch (Exception e) {

                    e.printStackTrace();

                } finally {

                    finishLatch.countDown();
                }
            });


            // =====================================================
            // Act
            // =====================================================

            System.out.println(
                    "主 Thread：同時開始兩個轉帳"
            );

            startLatch.countDown();


            // 等待兩個 Thread 完成
            finishLatch.await();


            // =====================================================
            // Assert
            // =====================================================

            Account actualFromAccount =
                    accountRepository
                            .findById(fromAccountId)
                            .orElseThrow();


            Account actualToAccount =
                    accountRepository
                            .findById(toAccountId)
                            .orElseThrow();


            // Account A
            assertEquals(
                    new BigDecimal("7000.00"),
                    actualFromAccount.getBalance()
            );


            // Account B
            assertEquals(
                    new BigDecimal("8000.00"),
                    actualToAccount.getBalance()
            );


        } finally {

            executor.shutdown();
        }
    }


    @Test
    @DisplayName("Concurrent Transfer - Step 2 - 兩個 Thread 同時轉帳")
    void concurrentTransfer_twoThreads() throws Exception {

        // =====================================================
        // Arrange
        // =====================================================

        // 建立 User
        User user = new User();
        user.setUsername("concurrentUser");
        user.setEmail("concurrent@test.com");
        user.setRealName("Concurrent User");

        userRepository.save(user);


        // 建立 Account A
        Account fromAccount = new Account();
        fromAccount.setAccountHolderName("Account A");
        fromAccount.setBalance(new BigDecimal("10000.00"));
        fromAccount.setUser(user);

        accountRepository.save(fromAccount);


        // 建立 Account B
        Account toAccount = new Account();
        toAccount.setAccountHolderName("Account B");
        toAccount.setBalance(new BigDecimal("5000.00"));
        toAccount.setUser(user);

        accountRepository.save(toAccount);


        Long fromAccountId = fromAccount.getId();
        Long toAccountId = toAccount.getId();


        // =====================================================
        // 建立兩個 Thread
        // =====================================================

        ExecutorService executor =
                Executors.newFixedThreadPool(2);


        // 起跑線
        CountDownLatch startLatch =
                new CountDownLatch(1);


        // 等待兩個 Thread 完成
        CountDownLatch finishLatch =
                new CountDownLatch(2);


        try {

            // =================================================
            // Thread 1
            // =================================================

            executor.submit(() -> {

                try {

                    // 等待開始訊號
                    startLatch.await();

                    System.out.println(
                            "Thread 1 開始轉帳：1000"
                    );


                    TransferFundDTO transfer =
                            new TransferFundDTO(
                                    fromAccountId,
                                    toAccountId,
                                    new BigDecimal("1000.00"),
                                    "concurrent-key-1"
                            );


                    accountService.transferFunds(transfer);


                    System.out.println(
                            "Thread 1 轉帳完成"
                    );


                } catch (Exception e) {

                    e.printStackTrace();

                } finally {

                    finishLatch.countDown();
                }

            });


            // =================================================
            // Thread 2
            // =================================================

            executor.submit(() -> {

                try {

                    // 等待開始訊號
                    startLatch.await();

                    System.out.println(
                            "Thread 2 開始轉帳：2000"
                    );


                    TransferFundDTO transfer =
                            new TransferFundDTO(
                                    fromAccountId,
                                    toAccountId,
                                    new BigDecimal("2000.00"),
                                    "concurrent-key-2"
                            );


                    accountService.transferFunds(transfer);


                    System.out.println(
                            "Thread 2 轉帳完成"
                    );


                } catch (Exception e) {

                    e.printStackTrace();

                } finally {

                    finishLatch.countDown();
                }

            });


            // =================================================
            // Act
            // =================================================

            System.out.println(
                    "主 Thread：準備同時開始兩個轉帳"
            );


            // 釋放兩個 Thread
            startLatch.countDown();


            // 等待 Thread 1 + Thread 2 完成
            finishLatch.await();


            // =================================================
            // Assert
            // =================================================

            assertEquals(
                    0,
                    finishLatch.getCount()
            );


        } finally {

            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Step 1 - 兩個 Thread 同時開始執行")
    void twoThreads_startAtSameTime() throws Exception {

        // =========================================================
        // Arrange
        // =========================================================

        // 建立 2 個 Thread 的 Thread Pool
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // 起跑線
        // 兩個 Thread 都必須等待這個 latch 被 countDown()
        CountDownLatch startLatch = new CountDownLatch(1);

        // 用來記錄兩個 Thread 是否真的執行
        CountDownLatch finishLatch = new CountDownLatch(2);

        try {

            // =====================================================
            // Thread 1
            // =====================================================

            executor.submit(() -> {

                try {

                    // 等待主 Thread 發出開始訊號
                    startLatch.await();

                    System.out.println("Thread 1 START");

                } catch (InterruptedException e) {

                    // 恢復 Thread 的 interrupted 狀態
                    Thread.currentThread().interrupt();

                } finally {

                    // Thread 1 執行完成
                    finishLatch.countDown();
                }
            });


            // =====================================================
            // Thread 2
            // =====================================================

            executor.submit(() -> {

                try {

                    // 等待主 Thread 發出開始訊號
                    startLatch.await();

                    System.out.println("Thread 2 START");

                } catch (InterruptedException e) {

                    // 恢復 Thread 的 interrupted 狀態
                    Thread.currentThread().interrupt();

                } finally {

                    // Thread 2 執行完成
                    finishLatch.countDown();
                }
            });


            // =====================================================
            // Act
            // =====================================================

            System.out.println("主 Thread：準備讓兩個 Thread 開始");

            // 發出開始訊號
            //
            // startLatch 原本：
            //
            // Count = 1
            //
            // countDown() 之後：
            //
            // Count = 0
            //
            // 所有 await() 都會被釋放
            startLatch.countDown();

            System.out.println("主 Thread：START");


            // 等待 Thread 1 + Thread 2 都完成
            finishLatch.await();


            // =====================================================
            // Assert
            // =====================================================

            // 如果能走到這裡，代表兩個 Thread 都已經完成
            assertEquals(
                    0,
                    finishLatch.getCount(),
                    "兩個 Thread 都應該執行完成"
            );

        } finally {

            // =====================================================
            // Cleanup
            // =====================================================

            executor.shutdown();
        }
    }

}