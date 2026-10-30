package com.adria.bank.transaction.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bank_transactions")
public class BankTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID accountId;

    private UUID destinationAccountId;

    @Column(nullable = false, precision = 38, scale = 12)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private String merchantCategory;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected BankTransaction() {}

    public BankTransaction(UUID accountId, UUID destinationAccountId, BigDecimal amount,
                           String currency, String merchantCategory, String country) {
        this.accountId = accountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.currency = currency;
        this.merchantCategory = merchantCategory;
        this.country = country;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getAccountId() { return accountId; }
    public UUID getDestinationAccountId() { return destinationAccountId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getMerchantCategory() { return merchantCategory; }
    public String getCountry() { return country; }
    public Instant getCreatedAt() { return createdAt; }
}
