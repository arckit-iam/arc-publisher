// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcpublisher.application.service;

import com.instantaccessprovider.arcpublisher.domain.exception.OrderNotCancellableException;
import com.instantaccessprovider.arcpublisher.domain.exception.OrderNotFoundException;
import com.instantaccessprovider.arcpublisher.domain.model.Order;
import com.instantaccessprovider.arcpublisher.domain.model.OrderAction;
import com.instantaccessprovider.arcpublisher.domain.model.OrderStatus;
import com.instantaccessprovider.arcpublisher.domain.model.OrderTargetType;
import com.instantaccessprovider.arcpublisher.domain.port.OrderRepository;
import com.instantaccessprovider.arcpublisher.dto.CreateOrderRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderApplicationServiceTest {

    private OrderRepository repository;
    private OrderApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(OrderRepository.class);
        service = new OrderApplicationService(repository);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private static Order order(UUID id, OrderStatus status) {
        return new Order(id, OrderTargetType.IDENTITY, OrderAction.CREATE, "u1", "U One", "ldap://x",
                null, null, null, "standard", null, null,
                OffsetDateTime.now().plusDays(1), OffsetDateTime.now(), "account:u1", status);
    }

    private static CreateOrderRequest request(UUID id) {
        CreateOrderRequest r = new CreateOrderRequest();
        r.setId(id);
        r.setAction(OrderAction.CREATE);
        r.setTargetType(OrderTargetType.IDENTITY);
        r.setIdentityId("u1");
        r.setIdentityName("U One");
        r.setTargetSystem("ldap://x");
        r.setAccountType("standard");
        r.setExecuteAt(OffsetDateTime.now().plusDays(1));
        return r;
    }

    @Test
    void createOrder_usesClientIdAndKeepsAccountType() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        Order created = service.createOrder(request(id));

        assertThat(created.getId()).isEqualTo(id);
        assertThat(created.getAccountType()).isEqualTo("standard");
        assertThat(created.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void createOrder_generatesIdWhenAbsent() {
        Order created = service.createOrder(request(null));

        assertThat(created.getId()).isNotNull();
    }

    @Test
    void createOrder_isIdempotentForKnownId() {
        UUID id = UUID.randomUUID();
        Order existing = order(id, OrderStatus.PENDING);
        when(repository.findById(id)).thenReturn(Optional.of(existing));

        Order created = service.createOrder(request(id));

        assertThat(created).isSameAs(existing);
        verify(repository, never()).save(any());
    }

    @Test
    void getById_throwsWhenUnknown() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id)).isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void cancelOrder_cancelsPendingOrder() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(order(id, OrderStatus.PENDING)));
        when(repository.transition(id, OrderStatus.PENDING, OrderStatus.CANCELLED)).thenReturn(true);

        service.cancelOrder(id);

        verify(repository).transition(id, OrderStatus.PENDING, OrderStatus.CANCELLED);
    }

    @Test
    void cancelOrder_isIdempotentWhenAlreadyCancelled() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(order(id, OrderStatus.CANCELLED)));

        service.cancelOrder(id);

        verify(repository, never()).transition(any(), any(), any());
    }

    @Test
    void cancelOrder_throwsWhenOrderAlreadyExecuted() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(order(id, OrderStatus.DONE)));
        when(repository.transition(id, OrderStatus.PENDING, OrderStatus.CANCELLED)).thenReturn(false);

        assertThatThrownBy(() -> service.cancelOrder(id)).isInstanceOf(OrderNotCancellableException.class);
    }

    @Test
    void cancelOrder_throwsWhenSchedulerWonTheRace() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(
                Optional.of(order(id, OrderStatus.PENDING)),
                Optional.of(order(id, OrderStatus.EXECUTING)));
        when(repository.transition(id, OrderStatus.PENDING, OrderStatus.CANCELLED)).thenReturn(false);

        assertThatThrownBy(() -> service.cancelOrder(id)).isInstanceOf(OrderNotCancellableException.class);
    }

    @Test
    void cancelOrder_throwsWhenUnknown() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelOrder(id)).isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void createOrder_savesOrderWithGivenFields() {
        service.createOrder(request(null));

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getIdentityId()).isEqualTo("u1");
        assertThat(captor.getValue().getCorrelationId()).isNull();
    }
}
