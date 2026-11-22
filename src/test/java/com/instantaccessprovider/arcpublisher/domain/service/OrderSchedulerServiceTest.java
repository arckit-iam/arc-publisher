// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcpublisher.domain.service;

import com.instantaccessprovider.arcpublisher.domain.model.Order;
import com.instantaccessprovider.arcpublisher.domain.model.OrderAction;
import com.instantaccessprovider.arcpublisher.domain.model.OrderStatus;
import com.instantaccessprovider.arcpublisher.domain.model.OrderTargetType;
import com.instantaccessprovider.arcpublisher.domain.port.OrderRepository;
import com.instantaccessprovider.arcpublisher.domain.port.ProvisioningEventPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OrderSchedulerServiceTest {

    private OrderRepository orderRepository;
    private ProvisioningEventPort provisioningEventPort;
    private OrderSchedulerService service;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        provisioningEventPort = mock(ProvisioningEventPort.class);
        when(orderRepository.transition(any(), eq(OrderStatus.PENDING), eq(OrderStatus.EXECUTING))).thenReturn(true);
        service = new OrderSchedulerService(orderRepository, provisioningEventPort);
    }

    private Order createOrder(OrderStatus status, OffsetDateTime executeAt) {
        return new Order(
                UUID.randomUUID(),
                OrderTargetType.IDENTITY,
                OrderAction.CREATE,
                "S-1-5-21-123",
                "jdupont",
                "active-directory",
                null, null, null, null, null, null,
                executeAt,
                OffsetDateTime.now().minusMinutes(1),
                "corr-001",
                status
        );
    }

    @Test
    void processDueOrders_publishesAndMarksDone_whenOrderIsDue() {
        Order order = createOrder(OrderStatus.PENDING, OffsetDateTime.now().minusSeconds(10));
        when(orderRepository.findDueOrders(any())).thenReturn(List.of(order));

        service.processDueOrders();

        verify(provisioningEventPort).publish(order);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DONE);
        verify(orderRepository, atLeastOnce()).updateStatus(eq(order.getId()), any());
    }

    @Test
    void processDueOrders_marksFailedOnPublishError() {
        Order order = createOrder(OrderStatus.PENDING, OffsetDateTime.now().minusSeconds(5));
        when(orderRepository.findDueOrders(any())).thenReturn(List.of(order));
        doThrow(new RuntimeException("Kafka down")).when(provisioningEventPort).publish(order);

        service.processDueOrders();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
    }

    @Test
    void processDueOrders_skipsOrderCancelledInTheMeantime() {
        Order order = createOrder(OrderStatus.PENDING, OffsetDateTime.now().minusSeconds(10));
        when(orderRepository.findDueOrders(any())).thenReturn(List.of(order));
        when(orderRepository.transition(order.getId(), OrderStatus.PENDING, OrderStatus.EXECUTING)).thenReturn(false);

        service.processDueOrders();

        verifyNoInteractions(provisioningEventPort);
        verify(orderRepository, never()).updateStatus(any(), any());
    }

    @Test
    void processDueOrders_doesNothing_whenNoDueOrders() {
        when(orderRepository.findDueOrders(any())).thenReturn(List.of());

        service.processDueOrders();

        verifyNoInteractions(provisioningEventPort);
    }

    @Test
    void order_isDue_returnsFalse_whenExecuteAtIsInFuture() {
        Order order = createOrder(OrderStatus.PENDING, OffsetDateTime.now().plusHours(1));
        assertThat(order.isDue(OffsetDateTime.now())).isFalse();
    }

    @Test
    void order_cancel_throwsException_whenNotPending() {
        Order order = createOrder(OrderStatus.DONE, OffsetDateTime.now());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, order::cancel);
    }
}
