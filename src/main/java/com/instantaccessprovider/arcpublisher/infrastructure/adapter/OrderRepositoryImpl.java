// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcpublisher.infrastructure.adapter;

import com.instantaccessprovider.arcpublisher.domain.model.Order;
import com.instantaccessprovider.arcpublisher.domain.model.OrderAction;
import com.instantaccessprovider.arcpublisher.domain.model.OrderStatus;
import com.instantaccessprovider.arcpublisher.domain.model.OrderTargetType;
import com.instantaccessprovider.arcpublisher.domain.port.OrderRepository;
import com.instantaccessprovider.arcpublisher.infrastructure.persistence.OrderEntity;
import com.instantaccessprovider.arcpublisher.infrastructure.persistence.SpringOrderRepository;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class OrderRepositoryImpl implements OrderRepository {

    private final SpringOrderRepository springOrderRepository;

    public OrderRepositoryImpl(SpringOrderRepository springOrderRepository) {
        this.springOrderRepository = springOrderRepository;
    }

    @Override
    public Order save(Order order) {
        OrderEntity entity = toEntity(order);
        springOrderRepository.save(entity);
        return order;
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return springOrderRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Order> findDueOrders(OffsetDateTime now) {
        return springOrderRepository.findDueOrders(now)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> findAll() {
        return springOrderRepository.findAll()
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void updateStatus(UUID id, OrderStatus status) {
        springOrderRepository.findById(id).ifPresent(entity -> {
            entity.setStatus(status);
            springOrderRepository.save(entity);
        });
    }

    @Override
    public boolean transition(UUID id, OrderStatus expected, OrderStatus next) {
        return springOrderRepository.transition(id, expected, next) > 0;
    }

    private OrderEntity toEntity(Order order) {
        OrderEntity e = new OrderEntity();
        e.setId(order.getId());
        e.setAction(order.getAction().name());
        e.setTargetType(order.getTargetType().name());
        e.setIdentityId(order.getIdentityId());
        e.setIdentityName(order.getIdentityName());
        e.setTargetSystem(order.getTargetSystem());
        e.setEntitlementId(order.getEntitlementId());
        e.setEntitlementName(order.getEntitlementName());
        e.setScope(order.getScope());
        e.setAccountType(order.getAccountType());
        e.setSource(order.getSource());
        e.setAttributes(order.getAttributes());
        e.setExecuteAt(order.getExecuteAt());
        e.setCreatedAt(order.getCreatedAt());
        e.setCorrelationId(order.getCorrelationId());
        e.setStatus(order.getStatus());
        return e;
    }

    private Order toDomain(OrderEntity e) {
        return new Order(
                e.getId(),
                OrderTargetType.valueOf(e.getTargetType()),
                OrderAction.valueOf(e.getAction()),
                e.getIdentityId(),
                e.getIdentityName(),
                e.getTargetSystem(),
                e.getEntitlementId(),
                e.getEntitlementName(),
                e.getScope(),
                e.getAccountType(),
                e.getSource(),
                e.getAttributes(),
                e.getExecuteAt(),
                e.getCreatedAt(),
                e.getCorrelationId(),
                e.getStatus()
        );
    }
}
