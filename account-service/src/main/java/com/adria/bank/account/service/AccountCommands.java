package com.adria.bank.account.service;

import com.adria.bank.account.domain.AccountStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public final class AccountCommands {
    private AccountCommands() {}

    public record CreateAccount(
            @NotBlank @Size(max = 255) String customerReference,
            @NotNull AccountStatus status,
            @NotNull @Pattern(regexp = "[A-Z]{3}") String currency,
            @NotNull @DecimalMin("0") BigDecimal balance) {}

    public record DebitAccount(
            @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal amount,
            @NotNull @Pattern(regexp = "[A-Z]{3}") String currency) {}

    public record TransferAccounts(
            @NotNull UUID sourceAccountId,
            @NotNull UUID destinationAccountId,
            @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal amount,
            @NotNull @Pattern(regexp = "[A-Z]{3}") String currency) {}

    public record ChangeStatus(@NotNull AccountStatus status) {}

    public record Balance(UUID accountId, String currency, double balance) {}

    public record TransferBalances(Balance source, Balance destination) {}
}
