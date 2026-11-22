// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcpublisher.infrastructure.config;

import com.instantaccessprovider.arcpublisher.infrastructure.consume.InvalidOrderMessageException;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Gestion des erreurs du consommateur d'ordres : les messages invalides partent directement
 * en {@code <topic>.DLT} ; les erreurs techniques (base indisponible...) sont rejouées 5 fois
 * (1 s d'écart) avant d'y partir aussi.
 */
@Configuration
@ConditionalOnExpression("'${arc.publisher.mode:api}' != 'api'")
public class KafkaConsumerConfig {

    @Bean
    public CommonErrorHandler ordersErrorHandler(KafkaProperties kafkaProperties) {
        DefaultKafkaProducerFactory<String, String> deadLetterFactory = new DefaultKafkaProducerFactory<>(
                kafkaProperties.buildProducerProperties(null), new StringSerializer(), new StringSerializer());
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                new KafkaTemplate<>(deadLetterFactory),
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", -1));
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 5L));
        handler.addNotRetryableExceptions(InvalidOrderMessageException.class);
        return handler;
    }
}
