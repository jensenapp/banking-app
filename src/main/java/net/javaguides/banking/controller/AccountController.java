package net.javaguides.banking.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import net.javaguides.banking.dto.AccountDto;
import net.javaguides.banking.dto.PageResponseDTO;
import net.javaguides.banking.dto.TransactionDTO;
import net.javaguides.banking.dto.TransferFundDTO;
import net.javaguides.banking.service.AccountService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountDto> addAccount(@Valid @RequestBody AccountDto accountDto) {
        AccountDto account = accountService.createAccount(accountDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(account);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountDto> getAccountById(@PathVariable Long id) {
        AccountDto accountById = accountService.getAccountById(id);
        return ResponseEntity.status(HttpStatus.OK).body(accountById);
    }

    @PutMapping("/{id}/deposit")
    public ResponseEntity<AccountDto> deposit(@PathVariable Long id, @RequestBody Map<String, BigDecimal> request) {

        BigDecimal amount = request.get("amount");

        AccountDto deposit = accountService.deposit(id, amount);

        return ResponseEntity.status(HttpStatus.OK).body(deposit);
    }

    @PutMapping("/{id}/withdraw")
    public ResponseEntity<AccountDto> withdraw(@PathVariable Long id, @RequestBody Map<String, BigDecimal> request) {
        BigDecimal amount = request.get("amount");
        AccountDto accountDto = accountService.withdraw(id, amount);
        return ResponseEntity.status(HttpStatus.OK).body(accountDto);
    }

    @GetMapping
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
    public ResponseEntity<String> deleteById(@PathVariable Long id) {
        accountService.deleteAccount(id);
        return ResponseEntity.ok("Account deleted successfully");
    }

    @PostMapping("/transfer")
    public ResponseEntity<String> transferFund(@Valid @RequestBody TransferFundDTO transferFundDTO) {
        accountService.transferFunds(transferFundDTO);
        return ResponseEntity.ok("transfer successful");
    }

    @GetMapping("/{accountId}/transactions")
    public ResponseEntity<PageResponseDTO<TransactionDTO>> fetchAccountTransactions(@PathVariable Long accountId, @RequestParam(defaultValue = "0") @Min(0) int pageNo, @RequestParam(defaultValue = "3") @Min(1) @Max(100) int pageSize) {

        Pageable pageable = PageRequest.of(pageNo, pageSize);

        Page<TransactionDTO> page = accountService.getAccountTransactions(accountId, pageable);

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
