package com.adria.bank.transaction;

import com.adria.bank.transaction.repository.TransactionRepository;
import com.adria.bank.transaction.service.TransactionService;
import com.adria.bank.transaction.service.TransactionCommands.CreateTransaction;
import com.adria.bank.transaction.service.client.AccountClient;
import com.adria.bank.transaction.service.messaging.TransactionMessaging;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "bank.transaction.max-amount=1000",
        "spring.datasource.url=jdbc:h2:mem:kafka-tests;DB_CLOSE_DELAY=-1",
        "spring.kafka.consumer.group-id=bank-logger-test",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.listener.auto-startup=true",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer"
})
@EmbeddedKafka(kraft = true, partitions = 3, topics = TransactionMessaging.TOPIC, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@DirtiesContext
@ExtendWith(OutputCaptureExtension.class)
class KafkaTransactionTest {
    @Autowired EmbeddedKafkaBroker broker;
    @Autowired TransactionService service;
    @Autowired TransactionRepository transactions;
    @Autowired ObjectMapper json;
    @MockitoBean AccountClient accounts;

    @Test
    void publishesAfterCommitWithAccountKeyAndConsumerOnlyLogs(CapturedOutput output) throws Exception {
        UUID accountId = UUID.randomUUID();
        when(accounts.get(accountId)).thenReturn(new AccountClient.AccountSnapshot(accountId, "A", "ACTIVE", "EUR", 1000));
        var properties = KafkaTestUtils.consumerProps("event-verifier", "false", broker);
        properties.put("auto.offset.reset", "earliest");
        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(properties,
                new StringDeserializer(), new StringDeserializer()).createConsumer()) {
            broker.consumeFromAnEmbeddedTopic(consumer, TransactionMessaging.TOPIC);
            var transaction = service.create(new CreateTransaction(accountId, new BigDecimal("250"), "EUR", "GROCERY", "FR"));
            var record = KafkaTestUtils.getSingleRecord(consumer, TransactionMessaging.TOPIC, Duration.ofSeconds(20));
            var event = json.readTree(record.value());
            assertThat(record.key()).isEqualTo(accountId.toString());
            assertThat(event.size()).isEqualTo(7);
            assertThat(event.get("eventType").asText()).isEqualTo("TransactionCreated");
            assertThat(event.get("transactionId").asText()).isEqualTo(transaction.getId().toString());
            assertThat(event.get("accountId").asText()).isEqualTo(accountId.toString());
            assertThat(event.get("amount").decimalValue()).isEqualByComparingTo("250");
            assertThat(event.get("currency").asText()).isEqualTo("EUR");
            assertThat(event.get("eventId").asText()).satisfies(UUID::fromString);
            assertThat(event.get("occurredAt").isTextual()).isTrue();
            await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                    assertThat(output.getOut()).contains("Received TransactionCreated").contains(transaction.getId().toString()));
            assertThat(transactions.count()).isEqualTo(1);
        }
    }
}
