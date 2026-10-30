package com.adria.bank.account.service;

import com.adria.bank.account.domain.AccountStatus;
import com.adria.bank.account.domain.BankAccount;
import com.adria.bank.account.repository.AccountRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import static com.adria.bank.account.service.AccountCommands.*;
import static com.adria.bank.account.service.BankException.Reason.*;

@Service
@Validated
@Transactional(readOnly = true)
public class AccountService {
    private final AccountRepository accounts;

    public AccountService(AccountRepository accounts) { this.accounts = accounts; }

    public List<BankAccount> list(String customerReference) {
        return customerReference == null ? accounts.findAll() : accounts.findByCustomerReference(customerReference);
    }

    public BankAccount get(@NotNull UUID id) {
        return accounts.findById(id).orElseThrow(() -> new BankException(NOT_FOUND, "Account not found"));
    }

    public Balance balance(@NotNull UUID id) { return balanceOf(get(id)); }

    @Transactional
    public BankAccount create(@NotNull @Valid CreateAccount request) {
        requireRepresentable(request.balance());
        return accounts.save(new BankAccount(request.customerReference(), request.status(),
                request.currency(), request.balance().doubleValue()));
    }

    @Transactional
    public BankAccount changeStatus(@NotNull UUID id, @NotNull @Valid ChangeStatus request) {
        BankAccount account = locked(id);
        account.changeStatus(request.status());
        return account;
    }

    @Transactional
    public Balance debit(@NotNull UUID id, @NotNull @Valid DebitAccount request) {
        BankAccount account = locked(id);
        requireUsable(account, request.currency());
        BigDecimal remaining = remaining(account, request.amount());
        account.updateBalance(remaining);
        return balanceOf(account);
    }

    @Transactional
    public TransferBalances transfer(@NotNull @Valid TransferAccounts request) {
        if (request.sourceAccountId().equals(request.destinationAccountId())) {
            throw new BankException(REJECTED, "Source and destination must be different");
        }
        // Stable lock order avoids deadlocks between opposite transfers.
        boolean sourceFirst = request.sourceAccountId().compareTo(request.destinationAccountId()) < 0;
        BankAccount first = locked(sourceFirst ? request.sourceAccountId() : request.destinationAccountId());
        BankAccount second = locked(sourceFirst ? request.destinationAccountId() : request.sourceAccountId());
        BankAccount source = sourceFirst ? first : second;
        BankAccount destination = sourceFirst ? second : first;
        requireUsable(source, request.currency());
        requireUsable(destination, request.currency());
        BigDecimal sourceBalance = remaining(source, request.amount());
        BigDecimal destinationBalance = BigDecimal.valueOf(destination.getBalance()).add(request.amount());
        requireRepresentable(destinationBalance);
        source.updateBalance(sourceBalance);
        destination.updateBalance(destinationBalance);
        return new TransferBalances(balanceOf(source), balanceOf(destination));
    }

    private BankAccount locked(UUID id) {
        return accounts.findForUpdate(id).orElseThrow(() -> new BankException(NOT_FOUND, "Account not found"));
    }

    private void requireUsable(BankAccount account, String currency) {
        if (account.getStatus() != AccountStatus.ACTIVE) throw new BankException(REJECTED, "Account is blocked");
        if (!account.getCurrency().equals(currency)) throw new BankException(REJECTED, "Currency mismatch");
    }

    private BigDecimal remaining(BankAccount account, BigDecimal amount) {
        BigDecimal result = BigDecimal.valueOf(account.getBalance()).subtract(amount);
        if (result.signum() < 0) throw new BankException(REJECTED, "Insufficient balance");
        requireRepresentable(result);
        return result;
    }

    private void requireRepresentable(BigDecimal balance) {
        double value = balance.doubleValue();
        if (!Double.isFinite(value) || BigDecimal.valueOf(value).compareTo(balance) != 0) {
            throw new BankException(REJECTED, "Balance exceeds supported double precision");
        }
    }

    private Balance balanceOf(BankAccount account) {
        return new Balance(account.getId(), account.getCurrency(), account.getBalance());
    }
}
