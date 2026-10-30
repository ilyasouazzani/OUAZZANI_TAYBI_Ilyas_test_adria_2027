package com.adria.bank.transaction.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.UUID;

@FeignClient(name = "account-service", url = "${bank.account-service.url:}")
public interface AccountClient {
    @GetMapping("/api/accounts/{id}")
    AccountSnapshot get(@PathVariable("id") UUID id);

    @PostMapping("/api/accounts/{id}/debit")
    void debit(@PathVariable("id") UUID id, @RequestBody Debit request);

    @PostMapping("/api/accounts/transfers")
    void transfer(@RequestBody Transfer request);

    record AccountSnapshot(UUID id, String customerReference, String status, String currency, double balance) {}
    record Debit(BigDecimal amount, String currency) {}
    record Transfer(UUID sourceAccountId, UUID destinationAccountId, BigDecimal amount, String currency) {}
}
