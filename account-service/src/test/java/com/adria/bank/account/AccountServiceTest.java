package com.adria.bank.account;

import com.adria.bank.account.domain.AccountStatus;
import com.adria.bank.account.domain.BankAccount;
import com.adria.bank.account.repository.AccountRepository;
import com.adria.bank.account.service.AccountService;
import com.adria.bank.account.service.BankException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.UUID;
import static com.adria.bank.account.service.AccountCommands.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:account-tests;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class AccountServiceTest {
    @Autowired AccountService service;
    @Autowired AccountRepository repository;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @BeforeEach
    void clean() { repository.deleteAll(); }

    private BankAccount account(String reference, String balance, String currency, AccountStatus status) {
        return service.create(new CreateAccount(reference, status, currency, new BigDecimal(balance)));
    }

    @Test
    void createsAndReadsAccountUsingRest() throws Exception {
        var request = new CreateAccount("CLIENT-001", AccountStatus.ACTIVE, "EUR", new BigDecimal("1500"));
        String response = mvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();
        String id = json.readTree(response).get("id").asText();
        mvc.perform(get("/api/accounts/{id}/balance", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1500));
        mvc.perform(get("/api/accounts").param("customerReference", "CLIENT-001"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void debitsUsingDecimalArithmetic() {
        var source = account("A", "0.30", "EUR", AccountStatus.ACTIVE);
        service.debit(source.getId(), new DebitAccount(new BigDecimal("0.10"), "EUR"));
        assertThat(service.balance(source.getId()).balance()).isEqualTo(0.20);
    }

    @Test
    void transfersBothBalancesAtomically() {
        var source = account("A", "1000", "EUR", AccountStatus.ACTIVE);
        var target = account("B", "200", "EUR", AccountStatus.ACTIVE);
        service.transfer(new TransferAccounts(source.getId(), target.getId(), new BigDecimal("250"), "EUR"));
        assertThat(service.balance(source.getId()).balance()).isEqualTo(750);
        assertThat(service.balance(target.getId()).balance()).isEqualTo(450);
    }

    @Test
    void invalidDestinationLeavesBothBalancesUnchanged() {
        var source = account("A", "1000", "EUR", AccountStatus.ACTIVE);
        var target = account("B", "200", "EUR", AccountStatus.BLOCKED);
        assertThatThrownBy(() -> service.transfer(new TransferAccounts(source.getId(), target.getId(),
                new BigDecimal("250"), "EUR"))).isInstanceOf(BankException.class);
        assertThat(service.balance(source.getId()).balance()).isEqualTo(1000);
        assertThat(service.balance(target.getId()).balance()).isEqualTo(200);
    }

    @Test
    void rejectsBlockedAccountWrongCurrencyInsufficientFundsAndSameAccount() {
        var active = account("A", "50", "EUR", AccountStatus.ACTIVE);
        var blocked = account("B", "50", "EUR", AccountStatus.BLOCKED);
        assertThatThrownBy(() -> service.debit(blocked.getId(), new DebitAccount(BigDecimal.ONE, "EUR")))
                .isInstanceOf(BankException.class).hasMessage("Account is blocked");
        assertThatThrownBy(() -> service.debit(active.getId(), new DebitAccount(BigDecimal.ONE, "MAD")))
                .isInstanceOf(BankException.class).hasMessage("Currency mismatch");
        assertThatThrownBy(() -> service.debit(active.getId(), new DebitAccount(new BigDecimal("51"), "EUR")))
                .isInstanceOf(BankException.class).hasMessage("Insufficient balance");
        assertThatThrownBy(() -> service.transfer(new TransferAccounts(active.getId(), active.getId(),
                BigDecimal.ONE, "EUR"))).isInstanceOf(BankException.class);
        assertThat(service.balance(active.getId()).balance()).isEqualTo(50);
    }

    @Test
    void validatesRequestsAtServiceBoundaryToo() {
        var source = account("A", "100", "EUR", AccountStatus.ACTIVE);
        assertThatThrownBy(() -> service.debit(source.getId(), new DebitAccount(BigDecimal.ZERO, "EUR")))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> service.create(new CreateAccount("A", AccountStatus.ACTIVE, "EUR", new BigDecimal("-1"))))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void refusesSilentLossOfPrecisionInDoubleBalance() {
        var source = account("A", "1500", "EUR", AccountStatus.ACTIVE);
        assertThatThrownBy(() -> service.debit(source.getId(), new DebitAccount(new BigDecimal("0.00000000000001"), "EUR")))
                .isInstanceOf(BankException.class).hasMessageContaining("precision");
        assertThat(service.balance(source.getId()).balance()).isEqualTo(1500);
    }

    @Test
    void restReturnsUsefulErrorStatuses() throws Exception {
        mvc.perform(get("/api/accounts/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
        mvc.perform(get("/api/accounts/not-a-uuid")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        var active = account("A", "50", "EUR", AccountStatus.ACTIVE);
        mvc.perform(patch("/api/accounts/{id}/status", active.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"BLOCKED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("BLOCKED"));
        mvc.perform(post("/api/accounts/{id}/debit", active.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10,\"currency\":\"EUR\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value("Account is blocked"));
    }
}
