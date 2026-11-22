// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcpublisher.infrastructure.persistence;

import com.instantaccessprovider.arcpublisher.domain.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface SpringOrderRepository extends JpaRepository<OrderEntity, UUID> {

    @Query("SELECT j FROM OrderEntity j WHERE j.status = 'PENDING' AND j.executeAt <= :now")
    List<OrderEntity> findDueOrders(@Param("now") OffsetDateTime now);

    @Modifying
    @Transactional
    @Query("UPDATE OrderEntity o SET o.status = :next WHERE o.id = :id AND o.status = :expected")
    int transition(@Param("id") UUID id, @Param("expected") OrderStatus expected, @Param("next") OrderStatus next);
}
