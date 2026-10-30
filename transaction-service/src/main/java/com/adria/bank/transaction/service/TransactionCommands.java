package com.adria.bank.transaction.service;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public final class TransactionCommands {
    private TransactionCommands() {}

    public record CreateTransaction(
            @NotNull UUID accountId,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 26, fraction = 12) BigDecimal amount,
            @NotNull @Pattern(regexp = "[A-Z]{3}") String currency,
            @NotBlank @Size(max = 255) String merchantCategory,
            @NotBlank @Size(max = 255) String country) {}

    public record Transfer(
            @NotNull UUID sourceAccountId,
            @NotNull UUID destinationAccountId,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 26, fraction = 12) BigDecimal amount,
            @NotNull @Pattern(regexp = "[A-Z]{3}") String currency,
            @NotBlank @Size(max = 255) String country) {}
}
