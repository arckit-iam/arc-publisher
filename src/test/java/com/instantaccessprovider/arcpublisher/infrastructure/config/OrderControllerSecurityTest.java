// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcpublisher.infrastructure.config;

import com.instantaccessprovider.arcpublisher.application.api.OrderController;
import com.instantaccessprovider.arcpublisher.application.service.OrderApplicationService;
import com.instantaccessprovider.arcpublisher.domain.model.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Vérifie, de la validation du jeton jusqu'au contrôleur, qui peut appeler quoi.
 * Le décodeur est remplacé par un décodeur de test : la signature n'est pas l'objet ici.
 */
@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, OrderControllerSecurityTest.TestDecoderConfig.class})
class OrderControllerSecurityTest {

    private static final String VALID_ORDER = """
            {"action":"CREATE","targetType":"IDENTITY","identityId":"zoe","identityName":"Zoe",
             "targetSystem":"ldap://LDAP01","executeAt":"2030-01-01T10:00:00Z"}""";

    @TestConfiguration
    static class TestDecoderConfig {

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> switch (token) {
                case "admin" -> jwt(List.of("ARC_ADMIN"));
                case "publisher" -> jwt(List.of("ARC_PUBLISHER"));
                case "reader" -> jwt(List.of("ARC_READER"));
                case "norole" -> jwt(List.of());
                case "unknownrole" -> jwt(List.of("FINANCE"));
                default -> throw new BadJwtException("jeton invalide");
            };
        }

        private static Jwt jwt(List<String> roles) {
            return Jwt.withTokenValue("t")
                    .header("alg", "RS256")
                    .subject("alice")
                    .claim("roles", roles)
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(300))
                    .build();
        }
    }

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private OrderApplicationService service;

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void rejectsRequestsWithoutToken() throws Exception {
        mvc.perform(get("/orders")).andExpect(status().isUnauthorized());
        mvc.perform(get("/orders/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
        mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(VALID_ORDER))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/orders/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidToken() throws Exception {
        mvc.perform(get("/orders").header("Authorization", bearer("forged")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsBasicAuthentication() throws Exception {
        mvc.perform(get("/orders").header("Authorization", "Basic YWxpY2U6cHc="))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void readerCanReadButNotWrite() throws Exception {
        when(service.getAll()).thenReturn(List.of());

        mvc.perform(get("/orders").header("Authorization", bearer("reader"))).andExpect(status().isOk());
        mvc.perform(post("/orders").header("Authorization", bearer("reader"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_ORDER))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/orders/" + UUID.randomUUID()).header("Authorization", bearer("reader")))
                .andExpect(status().isForbidden());
    }

    @Test
    void publisherCanCreateReadAndCancel() throws Exception {
        Order order = mock(Order.class);
        when(order.getId()).thenReturn(UUID.randomUUID());
        when(service.createOrder(any())).thenReturn(order);
        when(service.getAll()).thenReturn(List.of());

        mvc.perform(post("/orders").header("Authorization", bearer("publisher"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_ORDER))
                .andExpect(status().isCreated());
        mvc.perform(get("/orders").header("Authorization", bearer("publisher"))).andExpect(status().isOk());
        mvc.perform(delete("/orders/" + UUID.randomUUID()).header("Authorization", bearer("publisher")))
                .andExpect(status().isNoContent());
    }

    @Test
    void adminCanDoEverything() throws Exception {
        Order order = mock(Order.class);
        when(order.getId()).thenReturn(UUID.randomUUID());
        when(service.createOrder(any())).thenReturn(order);
        when(service.getAll()).thenReturn(List.of());

        mvc.perform(post("/orders").header("Authorization", bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_ORDER))
                .andExpect(status().isCreated());
        mvc.perform(get("/orders").header("Authorization", bearer("admin"))).andExpect(status().isOk());
        mvc.perform(delete("/orders/" + UUID.randomUUID()).header("Authorization", bearer("admin")))
                .andExpect(status().isNoContent());
    }

    @Test
    void cancelMapsBusinessErrorsToHttpStatus() throws Exception {
        UUID unknown = UUID.randomUUID();
        UUID executed = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new com.instantaccessprovider.arcpublisher.domain.exception.OrderNotFoundException(unknown))
                .when(service).cancelOrder(unknown);
        org.mockito.Mockito.doThrow(new com.instantaccessprovider.arcpublisher.domain.exception.OrderNotCancellableException(
                        executed, com.instantaccessprovider.arcpublisher.domain.model.OrderStatus.DONE))
                .when(service).cancelOrder(executed);

        mvc.perform(delete("/orders/" + unknown).header("Authorization", bearer("publisher")))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/orders/" + executed).header("Authorization", bearer("publisher")))
                .andExpect(status().isConflict());
    }

    @Test
    void tokenWithoutArcRoleIsForbidden() throws Exception {
        mvc.perform(get("/orders").header("Authorization", bearer("norole"))).andExpect(status().isForbidden());
        mvc.perform(get("/orders").header("Authorization", bearer("unknownrole"))).andExpect(status().isForbidden());
    }

    @Test
    void unknownPathsAreDeniedEvenWhenAuthenticated() throws Exception {
        mvc.perform(get("/other").header("Authorization", bearer("admin"))).andExpect(status().isForbidden());
    }
}
