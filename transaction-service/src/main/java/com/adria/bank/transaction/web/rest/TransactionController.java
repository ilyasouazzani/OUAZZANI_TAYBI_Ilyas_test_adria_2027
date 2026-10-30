package com.adria.bank.transaction.web.rest;

import com.adria.bank.transaction.domain.BankTransaction;
import com.adria.bank.transaction.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
import static com.adria.bank.transaction.service.TransactionCommands.*;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private final TransactionService service;

    public TransactionController(TransactionService service) { this.service = service; }

    @GetMapping
    public List<BankTransaction> list(@RequestParam(required = false) UUID accountId) {
        return service.list(accountId);
    }

    @GetMapping("/{id}")
    public BankTransaction get(@PathVariable UUID id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BankTransaction create(@Valid @RequestBody CreateTransaction request) { return service.create(request); }

    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    public BankTransaction transfer(@Valid @RequestBody Transfer request) { return service.transfer(request); }
}
