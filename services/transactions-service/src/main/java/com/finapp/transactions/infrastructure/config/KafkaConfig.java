package com.finapp.transactions.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka infrastructure configuration — ensures the required topics
 * exist when the application starts.
 */
@Configuration
public class KafkaConfig {

    @Value("${app.kafka.topic.transactions}")
    private String transactionsTopic;

    @Bean
    public NewTopic transactionEventsTopic() {
        return TopicBuilder.name(transactionsTopic)
                .partitions(6)
                .replicas(1)
                .build();
    }
}
