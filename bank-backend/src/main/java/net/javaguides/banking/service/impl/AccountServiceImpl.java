package net.javaguides.banking.service.impl;

import net.javaguides.banking.dto.AccountDto;
import net.javaguides.banking.dto.CreateAccountRequest;
import net.javaguides.banking.dto.TransactionDTO;
import net.javaguides.banking.dto.TransferFundDTO;
import net.javaguides.banking.entity.Account;
import net.javaguides.banking.entity.IdempotencyRecord;
import net.javaguides.banking.entity.Transaction;
import net.javaguides.banking.entity.User;
import net.javaguides.banking.enums.TransactionType;
import net.javaguides.banking.exception.AccountException;
import net.javaguides.banking.exception.AccountNotFoundException;
import net.javaguides.banking.exception.InsufficientAmountException;
import net.javaguides.banking.mapper.AccountMapper;
import net.javaguides.banking.repository.AccountRepository;
import net.javaguides.banking.repository.IdempotencyRepository;
import net.javaguides.banking.repository.TransactionRepository;
import net.javaguides.banking.repository.UserRepository;
import net.javaguides.banking.security.services.UserDetailsImpl;
import net.javaguides.banking.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;


@Service
public class AccountServiceImpl implements AccountService {

    private AccountRepository accountRepository;

    private TransactionRepository transactionRepository;

    private UserRepository userRepository;

    private AccountMapper accountMapper;

    private TransactionTemplate transactionTemplate;

    private IdempotencyRepository idempotencyRepository;


    private static final Logger logger = LoggerFactory.getLogger(AccountServiceImpl.class);

//    private static final String TRANSACTION_TYPE_DEPOSIT = "deposit";
//    private static final String TRANSACTION_TYPE_WITHDRAW = "withdraw";
//    private static final String TRANSACTION_TYPE_TRANSACTION = "transaction";


    public AccountServiceImpl(AccountRepository accountRepository,
                              TransactionRepository transactionRepository,
                              UserRepository userRepository,
                              AccountMapper accountMapper,
                              TransactionTemplate transactionTemplate,
                              IdempotencyRepository idempotencyRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.accountMapper = accountMapper;
        this.transactionTemplate = transactionTemplate;
        this.idempotencyRepository = idempotencyRepository;
    }

    @Override
    public AccountDto createAccount(CreateAccountRequest request) {

        // 1. 取得當前登入者 (Admin)，僅用來做 Log 紀錄追蹤
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl adminDetails = (UserDetailsImpl) auth.getPrincipal();
        logger.info("管理員 ID:{} 正在嘗試為客戶 ID:{} 建立新帳戶", adminDetails.getId(), request.userId());

        // 2. 針對「目標客戶」檢查帳戶數量上限
        final int MAX_ACCOUNT_LIMIT = 3;
        long currentAccountCount = accountRepository.countByUser_UserId(request.userId());

        if (currentAccountCount >= MAX_ACCOUNT_LIMIT) {
            logger.warn("創建失敗：客戶 ID:{} 已達到最大帳戶數量上限 ({})", request.userId(), MAX_ACCOUNT_LIMIT);
            throw new AccountException("該客戶已達到帳戶數量上限 (" + MAX_ACCOUNT_LIMIT + " 個)。");
        }

        // 3. 從資料庫抓出「目標客戶」，而不是抓管理員自己
        User targetUser = userRepository.findById(request.userId())
                .orElseThrow(() -> new RuntimeException("Target user not found with id: " + request.userId()));

        // 4. 建立帳戶並綁定給目標客戶
        Account account = new Account();
        account.setAccountHolderName(targetUser.getRealName());
        account.setBalance(request.balance());
        account.setUser(targetUser); // 綁定給客戶！

        Account saveAccount = accountRepository.save(account);

        logger.info("成功為客戶 ID:{} 啟用新帳戶, 帳戶 ID 為 {}", request.userId(), saveAccount.getId());
        return accountMapper.mapTOAccountDto(saveAccount);
    }



    @Override
    public List<AccountDto> getMyAccounts() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails=(UserDetailsImpl) authentication.getPrincipal();
        Long userId = userDetails.getId();
        List<AccountDto> accountLists = accountRepository.findAllAccountsByUserId(userId).stream().map(account -> accountMapper.mapTOAccountDto(account)).toList();
        return accountLists;
    }

    @Transactional(readOnly = true)
    @Override
    public AccountDto getAccountById(Long id) {
        logger.info("使用ID：{}查詢帳戶", id);
        Account account = accountRepository.findById(id).orElseThrow(() ->
        {
            logger.error("查無ID:{}", id);
            return new AccountNotFoundException("Account does not exist");
        });
        logger.info("成功取得帳號:{}", id);
        return accountMapper.mapTOAccountDto(account);
    }

    @Override
    public AccountDto deposit(Long id, BigDecimal amount) {

        final int MAX_ATTEMPS = 3;

        for (int attemp = 1; attemp <= MAX_ATTEMPS; attemp++) {

            try {
              return  transactionTemplate.execute(status -> {
                  logger.info("嘗試儲蓄{}進入帳號:{}", amount, id);
                  Account account = accountRepository.
                          findById(id).orElseThrow(() -> {
                              logger.error("儲蓄失敗,查無ID:{}", id);
                              return new AccountNotFoundException("Account does not exist");
                          });

                  account.setBalance(account.getBalance().add(amount));

                  Account saveAccount = accountRepository.saveAndFlush(account);
                  logger.info("儲蓄成功,帳號:{},新餘額:{}", id, saveAccount.getBalance());


                  // 記錄交易
                  Transaction transaction = new Transaction();
                  transaction.setAccountId(id);
                  transaction.setAmount(amount);
                  transaction.setTimestamp(LocalDateTime.now());
                  transaction.setTransactionType(TransactionType.DEPOSIT);
                  transactionRepository.save(transaction);

                  AccountDto accountDto = accountMapper.mapTOAccountDto(saveAccount);

                  return accountDto;
              });

            } catch (ObjectOptimisticLockingFailureException e) {
                // 發生衝突，記錄日誌後，迴圈將自動重試
                logger.warn("帳戶 {} 存款發生併發衝突，準備重試...", id);
            }
        }
        // 如果重試全部失敗，則拋出例外
        throw new AccountException("存款操作因高併發衝突而失敗，請稍後再試。");
    }


    @Override
    public AccountDto withdraw(Long accountId, BigDecimal amount) {

        final int MAX_ATTEMP=3;

        for (int attemp = 1; attemp <= MAX_ATTEMP; attemp++) {


            try {
               return transactionTemplate.execute(status -> {
                    logger.info("嘗試取款:{},扣款帳號:{}", amount, accountId);
                    Account account = accountRepository.findById(accountId).orElseThrow(() -> {
                        logger.error("取款失敗,查無帳號{}", accountId);
                        return new AccountNotFoundException("Account does not exist");
                    });

                    if (account.getBalance().compareTo(amount) < 0) {
                        logger.error("帳號{}餘額不足,取款失敗,帳戶餘額:{},取款金額{}", accountId, account.getBalance(), amount);
                        throw new InsufficientAmountException("Insufficient amount");
                    }


                    account.setBalance(account.getBalance().subtract(amount));
                    accountRepository.saveAndFlush(account);
                    logger.info("帳號{}取款成功，新餘額{}", accountId, account.getBalance());


                    // 記錄交易
                    Transaction transaction = new Transaction();
                    transaction.setAccountId(accountId);
                    transaction.setAmount(amount);
                    transaction.setTimestamp(LocalDateTime.now());
                    transaction.setTransactionType(TransactionType.WITHDRAW);

                    transactionRepository.save(transaction);


                    AccountDto accountDto = accountMapper.mapTOAccountDto(account);

                    return accountDto;
                });
            } catch (ObjectOptimisticLockingFailureException e) {
                logger.warn("帳戶{} 存款發生併發衝突，準備重試...", accountId);
            }
        }
        throw new AccountException("提款操作因高併發衝突而失敗，請稍後再試。");
    }

    @Transactional(readOnly = true)
    @Override
    public Page<AccountDto> getAllAccounts(Pageable pageable) {

        Page<Account> accounts = accountRepository.findAll(pageable);

        Page<AccountDto> accountDtoPage = accounts.map(account -> accountMapper.mapTOAccountDto(account));

        return accountDtoPage;
    }

    @Override
    public void deleteAccount(Long id) {
        logger.info("嘗試刪除帳戶,帳號:{}", id);
        Account account = accountRepository.findById(id).orElseThrow(() -> {
            logger.error("刪除失敗,查無帳號:{}", id);
            return new AccountNotFoundException("Account does not exist");
        });
        accountRepository.deleteById(id);
        logger.info("刪除成功,帳號{}", id);
    }

    @Override
    @Transactional
    public void transferFunds(TransferFundDTO transferFundDTO) {
        logger.info("從帳號{}向帳號{},發起金額為{}的轉帳", transferFundDTO.fromAccountId(), transferFundDTO.toAccountId(), transferFundDTO.amount());
        Long fromAccountId = transferFundDTO.fromAccountId();
        Long toAccountId = transferFundDTO.toAccountId();


        if (fromAccountId.equals(toAccountId)) {
            logger.error("轉帳失敗,不能轉帳給相同的帳號{}", fromAccountId);
            throw new AccountException("不能轉帳到相同帳戶");
        }

        IdempotencyRecord idempotencyRecord = new IdempotencyRecord();
        idempotencyRecord.setIdempotencyKey(transferFundDTO.idempotencyKey());
        idempotencyRecord.setCreatedAt(LocalDateTime.now());

        try {
            idempotencyRepository.saveAndFlush(idempotencyRecord);
        } catch (DataIntegrityViolationException e) {
            logger.warn("偵測到重複轉帳請求，idempotencyKey={}",transferFundDTO.idempotencyKey());
            throw new AccountException("Duplicate transfer request");
        }

        Account account1, account2;

        if (fromAccountId < toAccountId) {
            account1 = accountRepository.findByIdForUpdate(fromAccountId).orElseThrow(() -> new AccountNotFoundException("Account does not exist"));
            account2 = accountRepository.findByIdForUpdate(toAccountId).orElseThrow(() -> new AccountNotFoundException("Account does not exist"));
        } else {
            account2 = accountRepository.findByIdForUpdate(toAccountId).orElseThrow(() -> new AccountNotFoundException("Account does not exist"));
            account1 = accountRepository.findByIdForUpdate(fromAccountId).orElseThrow(() -> new AccountNotFoundException("Account does not exist"));
        }
        // 找出哪個是轉出帳戶，哪個是轉入帳戶

        Account fromAccount = account1.getId().equals(fromAccountId) ? account1 : account2;
        Account toAccount = account2.getId().equals(toAccountId) ? account2 : account1;


//        // 1. 檢索轉出帳戶
//        Account fromAccount = accountRepository.findById(transferFundDTO.fromAccountId()).orElseThrow(() -> new AccountException("Account does not exist"));
//        // 2. 檢索轉入帳戶
//         Account toAccount = accountRepository.findById(transferFundDTO.toAccountId()).orElseThrow(() -> new AccountException("Account does not exist"));


        if (fromAccount.getBalance().compareTo(transferFundDTO.amount()) < 0) {
            logger.error("轉帳失敗,帳戶{}餘額{}小於欲轉金額{}", fromAccountId, fromAccount.getBalance(), transferFundDTO.amount());
            throw new InsufficientAmountException("Insufficient amount");
        }

        // 3. 從轉出帳戶扣款
        fromAccount.setBalance(fromAccount.getBalance().subtract(transferFundDTO.amount()));
        // 4. 轉入帳戶存入金額
        toAccount.setBalance(toAccount.getBalance().add(transferFundDTO.amount()));
        // 5. 儲存更新
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        // 記錄轉出方交易（TRANSFER_OUT）
        Transaction fromTransaction = new Transaction();

        fromTransaction.setAccountId(transferFundDTO.fromAccountId());
        fromTransaction.setAmount(transferFundDTO.amount());
        fromTransaction.setTimestamp(LocalDateTime.now());
        fromTransaction.setTransactionType(TransactionType.TRANSFER_OUT);
        transactionRepository.save(fromTransaction);

        // 記錄轉入方交易（TRANSFER_IN）
        Transaction toTransaction = new Transaction();

        toTransaction.setAccountId(transferFundDTO.toAccountId());
        toTransaction.setAmount(transferFundDTO.amount());
        toTransaction.setTimestamp(LocalDateTime.now());
        toTransaction.setTransactionType(TransactionType.TRANSFER_IN);
        transactionRepository.save(toTransaction);
        logger.info("資金從帳戶 {} 轉至帳戶 {} 已成功完成", fromAccountId, toAccountId);

    }


    @Transactional(readOnly = true)
    @Override
    public Page<TransactionDTO> getAccountTransactions(Long accountId, Pageable pageable) {

        Page<Transaction> transactions = transactionRepository.findByAccountIdOrderByTimestampDesc(accountId, pageable);
        Page<TransactionDTO> transactionDTOPage = transactions.map(this::convertEntityToDTO);
//        List<TransactionDTO> transactionDTOList = new ArrayList<>();
//
//        for (Transaction transaction : transactionList) {
//            TransactionDTO transactionDTO = convertEntityToDTO(transaction);
//            transactionDTOList.add(transactionDTO);
//        }

//        List<TransactionDTO> collect =
//                transactions.stream().
//                map(transaction -> convertEntityToDTO(transaction)).
//                collect(Collectors.toList());


        return transactionDTOPage;
    }


    private TransactionDTO convertEntityToDTO(Transaction transaction) {
        return new TransactionDTO(
                transaction.getId(),
                transaction.getAccountId(),
                transaction.getAmount(),
                transaction.getTransactionType(),
                transaction.getTimestamp());
    }


}
