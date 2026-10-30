package com.adria.bank.transaction.web.graphql;

import com.adria.bank.transaction.domain.BankTransaction;
import com.adria.bank.transaction.service.TransactionService;
import com.adria.bank.transaction.service.BankException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.stereotype.Controller;
import java.util.List;
import java.util.UUID;
import static com.adria.bank.transaction.service.TransactionCommands.*;

@Controller
public class TransactionGraphqlController {
    private final TransactionService service;
    public TransactionGraphqlController(TransactionService service) { this.service = service; }
    @QueryMapping public List<BankTransaction> transactions(@Argument UUID accountId) { return service.list(accountId); }
    @QueryMapping public BankTransaction transaction(@Argument UUID id) { return service.get(id); }
    @MutationMapping public BankTransaction createTransaction(@Argument CreateTransaction input) { return service.create(input); }
    @MutationMapping public BankTransaction transferMoney(@Argument Transfer input) { return service.transfer(input); }
    @GraphQlExceptionHandler
    public GraphQLError business(BankException exception) {
        return GraphqlErrorBuilder.newError().message(exception.getMessage()).errorType(ErrorType.BAD_REQUEST).build();
    }
}
