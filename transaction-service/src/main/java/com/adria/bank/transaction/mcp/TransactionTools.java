package com.adria.bank.transaction.mcp;

import com.adria.bank.transaction.domain.BankTransaction;
import com.adria.bank.transaction.service.TransactionService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import static com.adria.bank.transaction.service.TransactionCommands.*;

@Component
public class TransactionTools {
    private final TransactionService service;

    public TransactionTools(TransactionService service) { this.service = service; }

    @Tool(name = "listAccountTransactions", description = "List transactions and transfers involving an account UUID")
    public List<BankTransaction> listAccountTransactions(UUID accountId) { return service.list(accountId); }

    @Tool(name = "getTransaction", description = "Get one bank transaction by its UUID")
    public BankTransaction getTransaction(UUID transactionId) { return service.get(transactionId); }

    @Tool(name = "createTransaction", description = "Debit an active account after checking currency, balance and configured ceiling; only execute an explicitly requested debit")
    public BankTransaction createTransaction(UUID accountId, BigDecimal amount, String currency, String merchantCategory, String country) {
        return service.create(new CreateTransaction(accountId, amount, currency, merchantCategory, country));
    }

    @Tool(name = "transferMoney", description = "Transfer money between two distinct active accounts in the same currency; only execute an explicitly requested transfer")
    public BankTransaction transferMoney(UUID sourceAccountId, UUID destinationAccountId, BigDecimal amount, String currency, String country) {
        return service.transfer(new Transfer(sourceAccountId, destinationAccountId, amount, currency, country));
    }
}
