package com.finapp.transactions.infrastructure.config;

import com.finapp.transactions.application.port.output.EventPublisher;
import com.finapp.transactions.application.port.output.TransactionRepository;
import com.finapp.transactions.application.usecase.CreateTransaction;
import com.finapp.transactions.application.usecase.GetTransactionHistory;
import com.finapp.transactions.application.usecase.ReverseTransaction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration — wires application-layer interactors via
 * constructor injection, bridging the Dependency Inversion boundary.
 *
 * <p>The interactors themselves are pure Java classes with no Spring annotations.
 * This configuration is the single point where Spring's DI container
 * supplies the infrastructure implementations of the output ports.</p>
 */
@Configuration
public class UseCaseConfig {

    @Bean
    public CreateTransaction createTransaction(TransactionRepository transactionRepository,
                                               EventPublisher eventPublisher) {
        return new CreateTransaction(transactionRepository, eventPublisher);
    }

    @Bean
    public ReverseTransaction reverseTransaction(TransactionRepository transactionRepository,
                                                  EventPublisher eventPublisher) {
        return new ReverseTransaction(transactionRepository, eventPublisher);
    }

    @Bean
    public GetTransactionHistory getTransactionHistory(TransactionRepository transactionRepository) {
        return new GetTransactionHistory(transactionRepository);
    }
}
