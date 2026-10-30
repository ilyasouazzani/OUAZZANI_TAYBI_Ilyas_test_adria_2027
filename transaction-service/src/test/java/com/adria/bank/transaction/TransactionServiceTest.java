package com.adria.bank.transaction;

import com.adria.bank.transaction.repository.TransactionRepository;
import com.adria.bank.transaction.service.BankException;
import com.adria.bank.transaction.service.TransactionService;
import com.adria.bank.transaction.service.client.AccountClient;
import com.adria.bank.transaction.service.client.AccountClient.AccountSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Request;
import feign.Response;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import static com.adria.bank.transaction.service.TransactionCommands.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:transaction-tests;DB_CLOSE_DELAY=-1",
        "bank.transaction.max-amount=1000"
})
@AutoConfigureMockMvc
class TransactionServiceTest {
    @Autowired TransactionService service;
    @Autowired TransactionRepository repository;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean AccountClient accounts;

    private final UUID sourceId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();

    @BeforeEach
    void clean() {
        repository.deleteAll();
        when(accounts.get(sourceId)).thenReturn(new AccountSnapshot(sourceId, "A", "ACTIVE", "EUR", 2000));
        when(accounts.get(targetId)).thenReturn(new AccountSnapshot(targetId, "B", "ACTIVE", "EUR", 100));
    }

    private CreateTransaction request(String amount) {
        return new CreateTransaction(sourceId, new BigDecimal(amount), "EUR", "GROCERY", "FR");
    }

    private FeignException failure(int status) {
        Request request = Request.create(Request.HttpMethod.GET, "http://account-service", Map.of(),
                (byte[]) null, StandardCharsets.UTF_8, null);
        Response response = Response.builder().request(request).status(status).reason("Test failure").build();
        return FeignException.errorStatus("AccountClient", response);
    }

    @Test
    void persistsThenUpdatesBalanceAndExposesHistory() throws Exception {
        doAnswer(invocation -> {
            assertThat(repository.count()).isEqualTo(1);
            return null;
        }).when(accounts).debit(eq(sourceId), any());
        String response = mvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request("250"))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.amount").value(250))
                .andReturn().getResponse().getContentAsString();
        String id = json.readTree(response).get("id").asText();
        InOrder order = inOrder(accounts);
        order.verify(accounts).get(sourceId);
        order.verify(accounts).debit(eq(sourceId), any());
        mvc.perform(get("/api/transactions/{id}", id)).andExpect(status().isOk());
        mvc.perform(get("/api/transactions").param("accountId", sourceId.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void acceptsCeilingExactly() {
        assertThat(service.create(request("1000")).getAmount()).isEqualByComparingTo("1000");
        verify(accounts).debit(eq(sourceId), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "0.00000000000001"})
    void rejectsInvalidAmountsAtServiceBoundary(String amount) {
        assertThatThrownBy(() -> service.create(request(amount))).isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(accounts);
        assertThat(repository.count()).isZero();
    }

    @Test
    void rejectsAmountAboveConfiguredCeilingBeforeCallingAccountService() {
        assertThatThrownBy(() -> service.create(request("1000.01")))
                .isInstanceOf(BankException.class).hasMessageContaining("1000");
        verifyNoInteractions(accounts);
        assertThat(repository.count()).isZero();
    }

    @Test
    void rejectsBlockedAccountCurrencyMismatchAndInsufficientFunds() {
        when(accounts.get(sourceId)).thenReturn(new AccountSnapshot(sourceId, "A", "BLOCKED", "EUR", 2000));
        assertThatThrownBy(() -> service.create(request("10"))).hasMessage("Account is blocked");
        when(accounts.get(sourceId)).thenReturn(new AccountSnapshot(sourceId, "A", "ACTIVE", "MAD", 2000));
        assertThatThrownBy(() -> service.create(request("10"))).hasMessage("Currency mismatch");
        when(accounts.get(sourceId)).thenReturn(new AccountSnapshot(sourceId, "A", "ACTIVE", "EUR", 5));
        assertThatThrownBy(() -> service.create(request("10"))).hasMessage("Insufficient balance");
        verify(accounts, never()).debit(any(), any());
        assertThat(repository.count()).isZero();
    }

    @Test
    void returns404ForMissingAccount() throws Exception {
        when(accounts.get(sourceId)).thenThrow(failure(404));
        mvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request("10"))))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.detail").value("Account not found"));
        assertThat(repository.count()).isZero();
    }

    @Test
    void rollsBackLocalTransactionWhenDebitFails() throws Exception {
        doThrow(failure(503)).when(accounts).debit(any(), any());
        mvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request("10"))))
                .andExpect(status().isServiceUnavailable());
        assertThat(repository.count()).isZero();
    }

    @Test
    void performsTransferAndIncludesItInDestinationHistory() {
        var saved = service.transfer(new Transfer(sourceId, targetId, new BigDecimal("250"), "EUR", "FR"));
        assertThat(saved.getDestinationAccountId()).isEqualTo(targetId);
        assertThat(saved.getMerchantCategory()).isEqualTo("TRANSFER");
        assertThat(service.list(targetId)).hasSize(1);
        verify(accounts).get(sourceId);
        verify(accounts).get(targetId);
        verify(accounts).transfer(new AccountClient.Transfer(sourceId, targetId, new BigDecimal("250"), "EUR"));
        verify(accounts, never()).debit(any(), any());
    }

    @Test
    void rejectsTransferToSameAccount() {
        assertThatThrownBy(() -> service.transfer(new Transfer(sourceId, sourceId, BigDecimal.ONE, "EUR", "FR")))
                .isInstanceOf(BankException.class);
        assertThat(repository.count()).isZero();
        verifyNoInteractions(accounts);
    }

    @Test
    void invalidDestinationDoesNotCreateTransactionOrMoveMoney() {
        when(accounts.get(targetId)).thenReturn(new AccountSnapshot(targetId, "B", "BLOCKED", "EUR", 100));
        assertThatThrownBy(() -> service.transfer(new Transfer(sourceId, targetId, BigDecimal.ONE, "EUR", "FR")))
                .hasMessage("Account is blocked");
        verify(accounts, never()).transfer(any());
        assertThat(repository.count()).isZero();
    }

    @Test
    void failedRemoteTransferRollsBackTransactionRecord() {
        doThrow(failure(409)).when(accounts).transfer(any());
        assertThatThrownBy(() -> service.transfer(new Transfer(sourceId, targetId, BigDecimal.ONE, "EUR", "FR")))
                .isInstanceOf(BankException.class);
        assertThat(repository.count()).isZero();
    }

    @Test
    void rejectsMalformedRestRequestsAndUnknownTransaction() throws Exception {
        mvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/transactions/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
    }
}
