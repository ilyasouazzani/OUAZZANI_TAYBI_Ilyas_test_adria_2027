package com.adria.bank.account.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "bank_accounts")
public class BankAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String customerReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private double balance;

    protected BankAccount() {}

    public BankAccount(String customerReference, AccountStatus status, String currency, double balance) {
        this.customerReference = customerReference;
        this.status = status;
        this.currency = currency;
        this.balance = balance;
    }

    public UUID getId() { return id; }
    public String getCustomerReference() { return customerReference; }
    public AccountStatus getStatus() { return status; }
    public String getCurrency() { return currency; }
    public double getBalance() { return balance; }

    public void changeStatus(AccountStatus status) { this.status = status; }

    public void updateBalance(BigDecimal balance) {
        this.balance = balance.doubleValue();
    }
}
