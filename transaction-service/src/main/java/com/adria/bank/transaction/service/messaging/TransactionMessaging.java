package com.adria.bank.transaction.service.messaging;

import com.adria.bank.transaction.domain.TransactionCreated;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class TransactionMessaging {
    public static final String TOPIC = "bank.transactions.created";
    private static final Logger log = LoggerFactory.getLogger(TransactionMessaging.class);
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json;

    public TransactionMessaging(KafkaTemplate<String, String> kafka, ObjectMapper json) {
        this.kafka = kafka;
        this.json = json;
    }

    @Bean
    public NewTopic transactionsCreatedTopic() {
        return TopicBuilder.name(TOPIC).partitions(3).replicas(1).build();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(TransactionCreated event) {
        try {
            String payload = json.writeValueAsString(event);
            kafka.send(TOPIC, event.accountId().toString(), payload).whenComplete((result, error) -> {
                if (error != null) log.error("Kafka publication failed for committed transaction {}", event.transactionId(), error);
                else log.info("Published TransactionCreated transactionId={} key={}", event.transactionId(), event.accountId());
            });
        } catch (JsonProcessingException | RuntimeException exception) {
            log.error("Kafka publication failed for committed transaction {}", event.transactionId(), exception);
        }
    }

    @KafkaListener(topics = TOPIC)
    public void consume(ConsumerRecord<String, String> record) {
        log.info("Received TransactionCreated topic={} key={} payload={}", record.topic(), record.key(), record.value());
    }
}
