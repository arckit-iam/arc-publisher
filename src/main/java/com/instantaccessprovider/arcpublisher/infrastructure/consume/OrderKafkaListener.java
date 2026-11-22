// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcpublisher.infrastructure.consume;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.instantaccessprovider.arcpublisher.application.service.OrderApplicationService;
import com.instantaccessprovider.arcpublisher.domain.exception.OrderNotCancellableException;
import com.instantaccessprovider.arcpublisher.domain.exception.OrderNotFoundException;
import com.instantaccessprovider.arcpublisher.dto.OrderMessage;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adapter entrant Kafka : consomme la file d'ordres alimentée par les managers.
 * Actif quand {@code arc.publisher.mode} vaut {@code kafka} ou {@code both}.
 */
@Component
@ConditionalOnExpression("'${arc.publisher.mode:api}' != 'api'")
public class OrderKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(OrderKafkaListener.class);

    private final OrderApplicationService orderService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public OrderKafkaListener(OrderApplicationService orderService, ObjectMapper objectMapper, Validator validator) {
        this.orderService = orderService;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @KafkaListener(topics = "${arc.orders.topic:arc.orders}")
    public void onMessage(String payload) {
        OrderMessage message = parse(payload);
        switch (message.getOperation()) {
            case SUBMIT -> submit(message);
            case CANCEL -> cancel(message);
        }
    }

    private void submit(OrderMessage message) {
        Set<ConstraintViolation<OrderMessage>> violations = validator.validate(message);
        if (!violations.isEmpty()) {
            String detail = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining(", "));
            throw new InvalidOrderMessageException("Invalid SUBMIT order: " + detail);
        }
        orderService.createOrder(message);
        log.info("Order submitted from queue: orderId={} action={} executeAt={}",
                message.getId(), message.getAction(), message.getExecuteAt());
    }

    private void cancel(OrderMessage message) {
        if (message.getId() == null) {
            throw new InvalidOrderMessageException("CANCEL order without id");
        }
        try {
            orderService.cancelOrder(message.getId());
            log.info("Order cancelled from queue: orderId={}", message.getId());
        } catch (OrderNotFoundException | OrderNotCancellableException e) {
            // Cas métier attendu (ordre déjà exécuté, ou jamais reçu) : on n'insiste pas.
            log.warn("Cancel ignored: {}", e.getMessage());
        }
    }

    private OrderMessage parse(String payload) {
        try {
            return objectMapper.readValue(payload, OrderMessage.class);
        } catch (JsonProcessingException e) {
            throw new InvalidOrderMessageException("Unreadable order message: " + e.getOriginalMessage(), e);
        }
    }
}
