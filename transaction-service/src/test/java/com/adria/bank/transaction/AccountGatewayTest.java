package com.adria.bank.transaction;

import com.adria.bank.transaction.service.BankException;
import com.adria.bank.transaction.service.client.AccountClient;
import com.adria.bank.transaction.service.client.AccountGateway;
import feign.FeignException;
import feign.Request;
import feign.Response;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountGatewayTest {
    private AccountClient client;
    private AccountGateway gateway;
    private CircuitBreakerRegistry breakers;
    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void prepare() {
        client = mock(AccountClient.class);
        breakers = CircuitBreakerRegistry.of(CircuitBreakerConfig.custom()
                .slidingWindowSize(2).minimumNumberOfCalls(2).failureRateThreshold(50)
                .ignoreExceptions(FeignException.FeignClientException.class).build());
        RetryRegistry retries = RetryRegistry.of(RetryConfig.custom().maxAttempts(2)
                .waitDuration(Duration.ofMillis(1))
                .retryExceptions(FeignException.FeignServerException.class)
                .ignoreExceptions(FeignException.FeignClientException.class).build());
        gateway = new AccountGateway(client, breakers, retries);
    }

    private FeignException failure(int status) {
        Request request = Request.create(Request.HttpMethod.GET, "http://account-service", Map.of(),
                (byte[]) null, StandardCharsets.UTF_8, null);
        return FeignException.errorStatus("AccountClient", Response.builder().request(request)
                .status(status).reason("Test failure").build());
    }

    @Test
    void retriesAReadAfterTransientFailure() {
        var snapshot = new AccountClient.AccountSnapshot(id, "A", "ACTIVE", "EUR", 1000);
        when(client.get(id)).thenThrow(failure(503)).thenReturn(snapshot);
        assertThat(gateway.get(id)).isEqualTo(snapshot);
        verify(client, times(2)).get(id);
    }

    @Test
    void opensCircuitAndFallsBackWithoutCallingRemoteAgain() {
        when(client.get(id)).thenThrow(failure(503));
        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> gateway.get(id)).isInstanceOf(BankException.class)
                    .hasMessageContaining("operation not confirmed");
        }
        assertThat(breakers.circuitBreaker("accountService").getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThatThrownBy(() -> gateway.get(id)).hasMessageContaining("circuit open");
        verify(client, times(4)).get(id);
    }

    @Test
    void doesNotRetryOrCountMissingAccountAsAnOutage() {
        when(client.get(id)).thenThrow(failure(404));
        assertThatThrownBy(() -> gateway.get(id)).isInstanceOf(BankException.class)
                .hasMessage("Account not found");
        verify(client).get(id);
        assertThat(breakers.circuitBreaker("accountService").getMetrics().getNumberOfFailedCalls()).isZero();
    }

    @Test
    void neverRetriesDebitOrTransfer() {
        var debit = new AccountClient.Debit(BigDecimal.TEN, "EUR");
        var transfer = new AccountClient.Transfer(id, UUID.randomUUID(), BigDecimal.TEN, "EUR");
        doThrow(failure(503)).when(client).debit(id, debit);
        doThrow(failure(503)).when(client).transfer(transfer);
        assertThatThrownBy(() -> gateway.debit(id, debit)).isInstanceOf(BankException.class);
        assertThatThrownBy(() -> gateway.transfer(transfer)).isInstanceOf(BankException.class);
        verify(client).debit(id, debit);
        verify(client).transfer(transfer);
    }
}
