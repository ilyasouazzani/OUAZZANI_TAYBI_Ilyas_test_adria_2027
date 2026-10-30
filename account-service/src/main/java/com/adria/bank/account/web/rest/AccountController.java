package com.adria.bank.account.web.rest;

import com.adria.bank.account.domain.BankAccount;
import com.adria.bank.account.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
import static com.adria.bank.account.service.AccountCommands.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService service;

    public AccountController(AccountService service) { this.service = service; }

    @GetMapping
    public List<BankAccount> list(@RequestParam(required = false) String customerReference) {
        return service.list(customerReference);
    }

    @GetMapping("/{id}")
    public BankAccount get(@PathVariable UUID id) { return service.get(id); }

    @GetMapping("/{id}/balance")
    public Balance balance(@PathVariable UUID id) { return service.balance(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BankAccount create(@Valid @RequestBody CreateAccount request) { return service.create(request); }

    @PatchMapping("/{id}/status")
    public BankAccount status(@PathVariable UUID id, @Valid @RequestBody ChangeStatus request) {
        return service.changeStatus(id, request);
    }

    @PostMapping("/{id}/debit")
    public Balance debit(@PathVariable UUID id, @Valid @RequestBody DebitAccount request) {
        return service.debit(id, request);
    }

    @PostMapping("/transfers")
    public TransferBalances transfer(@Valid @RequestBody TransferAccounts request) {
        return service.transfer(request);
    }
}
