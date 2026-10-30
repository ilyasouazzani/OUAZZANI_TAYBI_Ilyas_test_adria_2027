package com.adria.bank.account.web.graphql;

import com.adria.bank.account.domain.BankAccount;
import com.adria.bank.account.domain.AccountStatus;
import com.adria.bank.account.service.AccountService;
import com.adria.bank.account.service.BankException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.stereotype.Controller;
import java.util.List;
import java.util.UUID;
import static com.adria.bank.account.service.AccountCommands.*;

@Controller
public class AccountGraphqlController {
    private final AccountService service;
    public AccountGraphqlController(AccountService service) { this.service = service; }
    @QueryMapping public List<BankAccount> accounts(@Argument String customerReference) { return service.list(customerReference); }
    @QueryMapping public BankAccount account(@Argument UUID id) { return service.get(id); }
    @QueryMapping public Balance accountBalance(@Argument UUID id) { return service.balance(id); }
    @MutationMapping public BankAccount createAccount(@Argument CreateAccount input) { return service.create(input); }
    @MutationMapping public BankAccount changeAccountStatus(@Argument UUID id, @Argument AccountStatus status) {
        return service.changeStatus(id, new ChangeStatus(status));
    }
    @GraphQlExceptionHandler
    public GraphQLError business(BankException exception) {
        return GraphqlErrorBuilder.newError().message(exception.getMessage()).errorType(ErrorType.BAD_REQUEST).build();
    }
}
