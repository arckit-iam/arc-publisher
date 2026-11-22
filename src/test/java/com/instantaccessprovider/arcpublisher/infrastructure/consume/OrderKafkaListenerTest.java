// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcpublisher.infrastructure.consume;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.instantaccessprovider.arcpublisher.application.service.OrderApplicationService;
import com.instantaccessprovider.arcpublisher.domain.exception.OrderNotCancellableException;
import com.instantaccessprovider.arcpublisher.domain.exception.OrderNotFoundException;
import com.instantaccessprovider.arcpublisher.domain.model.OrderAction;
import com.instantaccessprovider.arcpublisher.domain.model.OrderStatus;
import com.instantaccessprovider.arcpublisher.dto.OrderMessage;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class OrderKafkaListenerTest {

    private OrderApplicationService service;
    private OrderKafkaListener listener;

    @BeforeEach
    void setUp() {
        service = mock(OrderApplicationService.class);
        // Même configuration que le mapper Spring Boot (propriétés inconnues ignorées).
        ObjectMapper mapper = org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.json().build();
        listener = new OrderKafkaListener(service, mapper,
                Validation.buildDefaultValidatorFactory().getValidator());
    }

    private static String submit(UUID id) {
        return """
                {"id":"%s","action":"CREATE","targetType":"IDENTITY","identityId":"zoe","identityName":"Zoe",
                 "targetSystem":"ldap://LDAP01","accountType":"standard","executeAt":"2030-01-01T10:00:00Z"}"""
                .formatted(id);
    }

    @Test
    void submit_isTheDefaultOperationAndCreatesTheOrder() {
        UUID id = UUID.randomUUID();

        listener.onMessage(submit(id));

        ArgumentCaptor<OrderMessage> captor = ArgumentCaptor.forClass(OrderMessage.class);
        verify(service).createOrder(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(id);
        assertThat(captor.getValue().getAction()).isEqualTo(OrderAction.CREATE);
        assertThat(captor.getValue().getAccountType()).isEqualTo("standard");
    }

    @Test
    void unknownFieldsAreIgnored() {
        String json = submit(UUID.randomUUID()).replace("{", "{\"extra\":1,");

        assertThatCode(() -> listener.onMessage(json)).doesNotThrowAnyException();
    }

    @Test
    void cancel_cancelsTheOrder() {
        UUID id = UUID.randomUUID();

        listener.onMessage("{\"operation\":\"CANCEL\",\"id\":\"" + id + "\"}");

        verify(service).cancelOrder(id);
    }

    @Test
    void cancel_ofUnknownOrderIsIgnored() {
        UUID id = UUID.randomUUID();
        doThrow(new OrderNotFoundException(id)).when(service).cancelOrder(id);

        assertThatCode(() -> listener.onMessage("{\"operation\":\"CANCEL\",\"id\":\"" + id + "\"}"))
                .doesNotThrowAnyException();
    }

    @Test
    void cancel_ofExecutedOrderIsIgnored() {
        UUID id = UUID.randomUUID();
        doThrow(new OrderNotCancellableException(id, OrderStatus.DONE)).when(service).cancelOrder(id);

        assertThatCode(() -> listener.onMessage("{\"operation\":\"CANCEL\",\"id\":\"" + id + "\"}"))
                .doesNotThrowAnyException();
    }

    @Test
    void cancel_withoutIdIsRejected() {
        assertThatThrownBy(() -> listener.onMessage("{\"operation\":\"CANCEL\"}"))
                .isInstanceOf(InvalidOrderMessageException.class);
        verify(service, never()).cancelOrder(any());
    }

    @Test
    void invalidSubmitIsRejected() {
        assertThatThrownBy(() -> listener.onMessage("{\"action\":\"CREATE\"}"))
                .isInstanceOf(InvalidOrderMessageException.class)
                .hasMessageContaining("identityId");
        verify(service, never()).createOrder(any());
    }

    @Test
    void unreadablePayloadIsRejected() {
        assertThatThrownBy(() -> listener.onMessage("pas du json"))
                .isInstanceOf(InvalidOrderMessageException.class);
    }

    @Test
    void technicalFailuresPropagateSoThatTheMessageIsRetried() {
        doThrow(new IllegalStateException("db down")).when(service).createOrder(any());

        assertThatThrownBy(() -> listener.onMessage(submit(UUID.randomUUID())))
                .isInstanceOf(IllegalStateException.class);
    }
}
