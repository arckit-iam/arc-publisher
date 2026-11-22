// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcpublisher.application.api;

import com.instantaccessprovider.arcpublisher.application.service.OrderApplicationService;
import com.instantaccessprovider.arcpublisher.domain.model.Order;
import com.instantaccessprovider.arcpublisher.dto.CreateOrderRequest;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@ConditionalOnExpression("'${arc.publisher.mode:api}' != 'kafka'")
@RequestMapping("/orders")
public class OrderController {

    private final OrderApplicationService orderApplicationService;

    public OrderController(OrderApplicationService orderApplicationService) {
        this.orderApplicationService = orderApplicationService;
    }

    /**
     * Crée un nouveau order de provisioning planifié.
     * POST /orders
     */
    @PostMapping
    public ResponseEntity<UUID> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Order order = orderApplicationService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order.getId());
    }

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderApplicationService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable UUID id) {
        return ResponseEntity.ok(orderApplicationService.getById(id));
    }

    /**
     * Annule un order encore en attente.
     * DELETE /orders/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelOrder(@PathVariable UUID id) {
        orderApplicationService.cancelOrder(id);
        return ResponseEntity.noContent().build();
    }
}
