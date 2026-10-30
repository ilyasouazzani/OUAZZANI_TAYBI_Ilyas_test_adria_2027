package com.adria.bank.account.mcp;

import com.adria.bank.account.domain.BankAccount;
import com.adria.bank.account.service.AccountCommands.Balance;
import com.adria.bank.account.service.AccountService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.UUID;

@Component
public class AccountTools {
    private final AccountService service;

    public AccountTools(AccountService service) { this.service = service; }

    @Tool(name = "listAccounts", description = "List all bank accounts and their identifiers")
    public List<BankAccount> listAccounts() { return service.list(null); }

    @Tool(name = "getAccount", description = "Get an existing account, its status, currency and balance by UUID")
    public BankAccount getAccount(UUID accountId) { return service.get(accountId); }

    @Tool(name = "getAccountBalance", description = "Read the current balance and currency of a bank account by UUID")
    public Balance getAccountBalance(UUID accountId) { return service.balance(accountId); }
}
