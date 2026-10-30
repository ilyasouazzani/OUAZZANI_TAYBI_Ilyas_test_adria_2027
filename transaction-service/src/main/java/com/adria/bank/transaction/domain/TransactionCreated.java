package com.adria.bank.transaction.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionCreated(UUID eventId, String eventType, UUID transactionId,
                                 UUID accountId, BigDecimal amount, String currency, Instant occurredAt) {
    public static TransactionCreated from(BankTransaction transaction) {
        return new TransactionCreated(UUID.randomUUID(), "TransactionCreated", transaction.getId(),
                transaction.getAccountId(), transaction.getAmount(), transaction.getCurrency(), Instant.now());
    }
}
