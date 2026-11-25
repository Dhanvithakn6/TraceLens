package com.tracelens.mainbackend.kafka;

import java.util.Map;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class IncidentKafkaConfiguration {
    private static final Logger log = LoggerFactory.getLogger(IncidentKafkaConfiguration.class);

    @Bean
    ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> consumerFactory,
            CommonErrorHandler incidentEventErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, consumerFactory);
        factory.setCommonErrorHandler(incidentEventErrorHandler);
        return factory;
    }

    @Bean
    CommonErrorHandler incidentEventErrorHandler(
            KafkaTemplate<Object, Object> kafkaTemplate,
            @Value("${tracelens.kafka.dead-letter-topic}") String deadLetterTopic) {
        DeadLetterPublishingRecoverer deadLetterRecoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate, (record, exception) -> new TopicPartition(deadLetterTopic, 0));
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (record, exception) -> {
                    log.error("Sending failed incident event from topic {} partition {} offset {} to dead letter topic {}",
                            record.topic(), record.partition(), record.offset(), deadLetterTopic, exception);
                    deadLetterRecoverer.accept(record, exception);
                },
                new FixedBackOff(1_000L, 3L));
        errorHandler.setClassifications(Map.of(DataAccessException.class, true), false);
        errorHandler.setRetryListeners((record, exception, deliveryAttempt) -> log.warn(
                "Retrying incident event from topic {} partition {} offset {} (attempt {})",
                record.topic(), record.partition(), record.offset(), deliveryAttempt, exception));
        return errorHandler;
    }
}
