// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcpublisher.infrastructure.config;

import com.instantaccessprovider.arcpublisher.domain.port.OrderRepository;
import com.instantaccessprovider.arcpublisher.domain.port.ProvisioningEventPort;
import com.instantaccessprovider.arcpublisher.domain.service.OrderSchedulerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class DomainConfig {

    @Bean
    public OrderSchedulerService orderSchedulerService(OrderRepository orderRepository,
                                                    ProvisioningEventPort provisioningEventPort) {
        return new OrderSchedulerService(orderRepository, provisioningEventPort);
    }
}
