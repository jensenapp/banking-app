package net.javaguides.banking.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import net.javaguides.banking.dto.*;
import net.javaguides.banking.service.AccountService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Validated
@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Accounts", description = "銀行帳戶管理 API")
public class AccountController {

    private AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "建立新帳戶", description = "建立一個新的銀行帳戶 (需要 USER 權限)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "帳戶建立成功"),
            @ApiResponse(responseCode = "400", description = "輸入資料格式錯誤"),
            @ApiResponse(responseCode = "401", description = "未授權 (需登入)")
    })
    public ResponseEntity<AccountDto> addAccount(@Valid @RequestBody AccountDto accountDto) {

        AccountDto account = accountService.createAccount(accountDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(account);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @accountSecurityService.isOwner(authentication,#id)")
    @Operation(summary = "查詢單一帳戶", description = "根據 ID 查詢帳戶詳情 (僅限 ADMIN 或帳戶本人)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功取得帳戶資訊"),
            @ApiResponse(responseCode = "403", description = "無權限存取此帳戶"),
            @ApiResponse(responseCode = "404", description = "帳戶不存在")
    })
    public ResponseEntity<AccountDto> getAccountById(@PathVariable Long id) {
        AccountDto accountById = accountService.getAccountById(id);
        return ResponseEntity.status(HttpStatus.OK).body(accountById);
    }

    @PutMapping("/{id}/deposit")
    @PreAuthorize("@accountSecurityService.isOwner(authentication,#id)")
    @Operation(summary = "帳戶存款", description = "存入資金到指定帳戶 (僅限帳戶本人)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "存款成功"),
            @ApiResponse(responseCode = "400", description = "金額必須為正數")
    })
    public ResponseEntity<AccountDto> deposit(@PathVariable Long id, @Valid @RequestBody AmountRequestDto amountRequestDto) {

        BigDecimal amount = amountRequestDto.amount();

        AccountDto deposit = accountService.deposit(id, amount);

        return ResponseEntity.status(HttpStatus.OK).body(deposit);
    }

    @PutMapping("/{id}/withdraw")
    @PreAuthorize("@accountSecurityService.isOwner(authentication,#id)")
    @Operation(summary = "帳戶提款", description = "從指定帳戶提取資金 (僅限帳戶本人)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "提款成功"),
            @ApiResponse(responseCode = "400", description = "餘額不足或金額錯誤")
    })
    public ResponseEntity<AccountDto> withdraw(@PathVariable Long id, @Valid @RequestBody AmountRequestDto amountRequestDto) {
        BigDecimal amount = amountRequestDto.amount();
        AccountDto accountDto = accountService.withdraw(id, amount);
        return ResponseEntity.status(HttpStatus.OK).body(accountDto);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "取得所有帳戶列表", description = "分頁查詢系統內所有帳戶 (僅限 ADMIN)")
    public ResponseEntity<PageResponseDTO<AccountDto>> getAllAccounts(@RequestParam(defaultValue = "0") @Min(0) int pageNo,
                                                                      @RequestParam(defaultValue = "3") @Min(1) @Max(100) int pageSize,
                                                                      @RequestParam(defaultValue = "id") String sortBy,
                                                                      @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();


        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<AccountDto> allAccounts = accountService.getAllAccounts(pageable);

        PageResponseDTO<AccountDto> pageResponseDTO = new PageResponseDTO<>(
                allAccounts.getContent(),
                allAccounts.getNumber(),
                allAccounts.getSize(),
                allAccounts.getTotalElements(),
                allAccounts.getTotalPages(),
                allAccounts.isLast());

        return ResponseEntity.ok(pageResponseDTO);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "刪除帳戶", description = "永久刪除指定 ID 的帳戶 (僅限 ADMIN)")
    public ResponseEntity<String> deleteById(@PathVariable Long id) {
        accountService.deleteAccount(id);
        return ResponseEntity.ok("Account deleted successfully");
    }

    @PostMapping("/transfer")
    @PreAuthorize("@accountSecurityService.isOwner(authentication,#transferFundDTO.fromAccountId())")
    @Operation(summary = "資金轉帳", description = "將資金從一個帳戶轉移到另一個帳戶 (需驗證轉出帳戶擁有權)")
    public ResponseEntity<String> transferFund(@Valid @RequestBody TransferFundDTO transferFundDTO) {
        accountService.transferFunds(transferFundDTO);
        return ResponseEntity.ok("transfer successful");
    }

    @GetMapping("/{id}/transactions")
    @PreAuthorize("hasRole('ADMIN') or @accountSecurityService.isOwner(authentication,#id)")
    @Operation(summary = "查詢交易紀錄", description = "分頁查詢指定帳戶的交易明細 (僅限 ADMIN 或帳戶本人)")
    public ResponseEntity<PageResponseDTO<TransactionDTO>> fetchAccountTransactions(@PathVariable Long id, @RequestParam(defaultValue = "0") @Min(0) int pageNo, @RequestParam(defaultValue = "3") @Min(1) @Max(100) int pageSize) {

        Pageable pageable = PageRequest.of(pageNo, pageSize);

        Page<TransactionDTO> page = accountService.getAccountTransactions(id, pageable);

        PageResponseDTO<TransactionDTO> transactionDTOPageResponseDTO =
                new PageResponseDTO<TransactionDTO>(
                        page.getContent(),
                        page.getNumber(),
                        page.getSize(),
                        page.getTotalElements(),
                        page.getTotalPages(),
                        page.isLast());

        return ResponseEntity.ok(transactionDTOPageResponseDTO);
    }
}
