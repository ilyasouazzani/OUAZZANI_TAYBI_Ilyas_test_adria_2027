package com.adria.bank.transaction.service.client;

import com.adria.bank.transaction.service.BankException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.UUID;
import java.util.function.Supplier;
import static com.adria.bank.transaction.service.BankException.Reason.*;

@Component
public class AccountGateway {
    private static final Logger log = LoggerFactory.getLogger(AccountGateway.class);
    private final AccountClient client;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public AccountGateway(AccountClient client, CircuitBreakerRegistry breakers, RetryRegistry retries) {
        this.client = client;
        this.circuitBreaker = breakers.circuitBreaker("accountService");
        this.retry = retries.retry("accountService");
        circuitBreaker.getEventPublisher().onStateTransition(event -> log.info("Account circuit: {}", event.getStateTransition()));
        retry.getEventPublisher().onRetry(event -> log.warn("Account lookup retry {}", event.getNumberOfRetryAttempts()));
    }

    public AccountClient.AccountSnapshot get(UUID id) {
        return guarded(Retry.decorateSupplier(retry, () -> client.get(id)));
    }

    public void debit(UUID id, AccountClient.Debit request) {
        // Retrying a debit after a lost response could debit the account twice.
        guarded(() -> { client.debit(id, request); return null; });
    }

    public void transfer(AccountClient.Transfer request) {
        guarded(() -> { client.transfer(request); return null; });
    }

    private <T> T guarded(Supplier<T> operation) {
        try {
            return circuitBreaker.executeSupplier(operation);
        } catch (FeignException exception) {
            if (exception.status() == 404) throw new BankException(NOT_FOUND, "Account not found");
            if (exception.status() >= 400 && exception.status() < 500) {
                throw new BankException(REJECTED, "Account service rejected the operation");
            }
            return fallback("account service unavailable");
        } catch (CallNotPermittedException exception) {
            return fallback("circuit open");
        }
    }

    private <T> T fallback(String reason) {
        log.warn("Account service fallback: {}; operation not confirmed", reason);
        throw new BankException(UNAVAILABLE, "Account service unavailable (" + reason + "); operation not confirmed");
    }
}
