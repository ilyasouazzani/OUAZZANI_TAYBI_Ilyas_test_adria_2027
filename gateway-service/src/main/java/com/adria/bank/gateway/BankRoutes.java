package com.adria.bank.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BankRoutes {
    @Bean
    public RouteLocator bankRouteLocator(RouteLocatorBuilder builder,
            @Value("${bank.routes.accounts}") String accounts,
            @Value("${bank.routes.transactions}") String transactions,
            @Value("${bank.routes.assistant}") String assistant) {
        return builder.routes()
                .route("accounts-rest", r -> r.path("/api/accounts", "/api/accounts/**").uri(accounts))
                .route("transactions-rest", r -> r.path("/api/transactions", "/api/transactions/**").uri(transactions))
                .route("assistant-rest", r -> r.path("/api/assistant/**").uri(assistant))
                .route("accounts-graphql", r -> r.path("/accounts/graphql").filters(f -> f.setPath("/graphql")).uri(accounts))
                .route("transactions-graphql", r -> r.path("/transactions/graphql").filters(f -> f.setPath("/graphql")).uri(transactions))
                .route("accounts-mcp", r -> r.path("/accounts/mcp").filters(f -> f.setPath("/mcp")).uri(accounts))
                .route("transactions-mcp", r -> r.path("/transactions/mcp").filters(f -> f.setPath("/mcp")).uri(transactions))
                .build();
    }
}
