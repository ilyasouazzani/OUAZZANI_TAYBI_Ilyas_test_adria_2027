package com.adria.bank.transaction.service;

import com.adria.bank.transaction.domain.BankTransaction;
import com.adria.bank.transaction.repository.TransactionRepository;
import com.adria.bank.transaction.service.client.AccountClient;
import com.adria.bank.transaction.service.client.AccountClient.AccountSnapshot;
import feign.FeignException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import static com.adria.bank.transaction.service.TransactionCommands.*;
import static com.adria.bank.transaction.service.BankException.Reason.*;

@Service
@Validated
@Transactional(readOnly = true)
public class TransactionService {
    private final TransactionRepository transactions;
    private final AccountClient accounts;
    private final BigDecimal maxAmount;

    public TransactionService(TransactionRepository transactions, AccountClient accounts,
                              @Value("${bank.transaction.max-amount:1000}") BigDecimal maxAmount) {
        if (maxAmount.signum() <= 0) throw new IllegalArgumentException("Transaction ceiling must be positive");
        this.transactions = transactions;
        this.accounts = accounts;
        this.maxAmount = maxAmount;
    }

    public List<BankTransaction> list(UUID accountId) {
        return accountId == null ? transactions.findAllByOrderByCreatedAtDesc()
                : transactions.findByAccountIdOrDestinationAccountIdOrderByCreatedAtDesc(accountId, accountId);
    }

    public BankTransaction get(@NotNull UUID id) {
        return transactions.findById(id).orElseThrow(() -> new BankException(NOT_FOUND, "Transaction not found"));
    }

    @Transactional
    public BankTransaction create(@NotNull @Valid CreateTransaction request) {
        checkAmount(request.amount());
        AccountSnapshot account = remote(() -> accounts.get(request.accountId()));
        checkAccount(account, request.currency());
        checkFunds(account, request.amount());
        BankTransaction transaction = transactions.saveAndFlush(new BankTransaction(request.accountId(), null,
                request.amount(), request.currency(), request.merchantCategory(), request.country()));
        remote(() -> {
            accounts.debit(request.accountId(), new AccountClient.Debit(request.amount(), request.currency()));
            return null;
        });
        return transaction;
    }

    @Transactional
    public BankTransaction transfer(@NotNull @Valid Transfer request) {
        checkAmount(request.amount());
        if (request.sourceAccountId().equals(request.destinationAccountId())) {
            throw new BankException(REJECTED, "Source and destination must be different");
        }
        AccountSnapshot source = remote(() -> accounts.get(request.sourceAccountId()));
        AccountSnapshot destination = remote(() -> accounts.get(request.destinationAccountId()));
        checkAccount(source, request.currency());
        checkAccount(destination, request.currency());
        checkFunds(source, request.amount());
        BankTransaction transaction = transactions.saveAndFlush(new BankTransaction(request.sourceAccountId(),
                request.destinationAccountId(), request.amount(), request.currency(), "TRANSFER", request.country()));
        remote(() -> {
            accounts.transfer(new AccountClient.Transfer(request.sourceAccountId(), request.destinationAccountId(),
                    request.amount(), request.currency()));
            return null;
        });
        return transaction;
    }

    private void checkAmount(BigDecimal amount) {
        if (amount.signum() <= 0 || amount.compareTo(maxAmount) > 0) {
            throw new BankException(REJECTED, "Amount must be positive and at most " + maxAmount);
        }
    }

    private void checkAccount(AccountSnapshot account, String currency) {
        if (account == null) throw new BankException(UNAVAILABLE, "Account service returned an empty response");
        if (!"ACTIVE".equals(account.status())) throw new BankException(REJECTED, "Account is blocked");
        if (!currency.equals(account.currency())) throw new BankException(REJECTED, "Currency mismatch");
    }

    private void checkFunds(AccountSnapshot account, BigDecimal amount) {
        if (!Double.isFinite(account.balance())) throw new BankException(UNAVAILABLE, "Invalid account balance");
        if (BigDecimal.valueOf(account.balance()).compareTo(amount) < 0) {
            throw new BankException(REJECTED, "Insufficient balance");
        }
    }

    private <T> T remote(Supplier<T> call) {
        try {
            return call.get();
        } catch (FeignException exception) {
            if (exception.status() == 404) throw new BankException(NOT_FOUND, "Account not found");
            if (exception.status() >= 400 && exception.status() < 500) {
                throw new BankException(REJECTED, "Account service rejected the operation");
            }
            throw new BankException(UNAVAILABLE, "Account service is unavailable; operation not confirmed");
        }
    }
}
