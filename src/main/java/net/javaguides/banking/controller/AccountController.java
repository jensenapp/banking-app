package net.javaguides.banking.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import net.javaguides.banking.dto.AccountDto;
import net.javaguides.banking.dto.TransactionDTO;
import net.javaguides.banking.dto.TransferFundDTO;
import net.javaguides.banking.service.AccountService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<AccountDto> addAccount(@RequestBody AccountDto accountDto) {
        AccountDto account = accountService.createAccount(accountDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(account);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountDto> getAccountById(@PathVariable Long id) {
        AccountDto accountById = accountService.getAccountById(id);
        return ResponseEntity.status(HttpStatus.OK).body(accountById);
    }

    @PutMapping("/{id}/deposit")
    public ResponseEntity<AccountDto> deposit(@PathVariable Long id, @RequestBody Map<String, Double> request) {

        Double amount = request.get("amount");

        AccountDto deposit = accountService.deposit(id, amount);

        return ResponseEntity.status(HttpStatus.OK).body(deposit);
    }

    @PutMapping("/{id}/withdraw")
    public ResponseEntity<AccountDto> withdraw(@PathVariable Long id, @RequestBody Map<String, Double> request) {
        Double withdraw = request.get("withdraw");
        AccountDto accountDto = accountService.withdraw(id, withdraw);
        return ResponseEntity.status(HttpStatus.OK).body(accountDto);
    }

    @GetMapping
    public ResponseEntity<List<AccountDto>> getAllAccounts() {
        List<AccountDto> allAccounts = accountService.getAllAccounts();
        return ResponseEntity.ok(allAccounts);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteById(@PathVariable Long id) {
        accountService.deleteAccount(id);
        return ResponseEntity.ok("Account deleted successfully");
    }

    @PostMapping("/transfer")
    public ResponseEntity<String> transferFund(@RequestBody TransferFundDTO transferFundDTO) {
        accountService.transferFunds(transferFundDTO);
        return ResponseEntity.ok("transfer successful");
    }

    @GetMapping("/{accountId}/transactions")
    public ResponseEntity<Page<TransactionDTO>> fetchAccountTransactions(@PathVariable Long accountId, @RequestParam(defaultValue = "0") @Min(0)  int pageNo, @RequestParam(defaultValue = "3") @Min(1)@Max(100) int pageSize) {
        Pageable pageable = PageRequest.of(pageNo, pageSize);

        Page<TransactionDTO> accountTransactions = accountService.getAccountTransactions(accountId, pageable);
        return ResponseEntity.ok(accountTransactions);
    }
}
