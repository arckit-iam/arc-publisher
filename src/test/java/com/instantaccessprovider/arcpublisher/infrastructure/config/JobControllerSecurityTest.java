// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcpublisher.infrastructure.config;

import com.instantaccessprovider.arcpublisher.application.api.JobController;
import com.instantaccessprovider.arcpublisher.application.service.JobApplicationService;
import com.instantaccessprovider.arcpublisher.domain.model.Job;
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
@WebMvcTest(JobController.class)
@Import({SecurityConfig.class, JobControllerSecurityTest.TestDecoderConfig.class})
class JobControllerSecurityTest {

    private static final String VALID_JOB = """
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
    private JobApplicationService service;

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void rejectsRequestsWithoutToken() throws Exception {
        mvc.perform(get("/jobs")).andExpect(status().isUnauthorized());
        mvc.perform(get("/jobs/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
        mvc.perform(post("/jobs").contentType(MediaType.APPLICATION_JSON).content(VALID_JOB))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/jobs/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidToken() throws Exception {
        mvc.perform(get("/jobs").header("Authorization", bearer("forged")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsBasicAuthentication() throws Exception {
        mvc.perform(get("/jobs").header("Authorization", "Basic YWxpY2U6cHc="))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void readerCanReadButNotWrite() throws Exception {
        when(service.getAll()).thenReturn(List.of());

        mvc.perform(get("/jobs").header("Authorization", bearer("reader"))).andExpect(status().isOk());
        mvc.perform(post("/jobs").header("Authorization", bearer("reader"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_JOB))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/jobs/" + UUID.randomUUID()).header("Authorization", bearer("reader")))
                .andExpect(status().isForbidden());
    }

    @Test
    void publisherCanCreateReadAndCancel() throws Exception {
        Job job = mock(Job.class);
        when(job.getId()).thenReturn(UUID.randomUUID());
        when(service.createJob(any())).thenReturn(job);
        when(service.getAll()).thenReturn(List.of());

        mvc.perform(post("/jobs").header("Authorization", bearer("publisher"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_JOB))
                .andExpect(status().isCreated());
        mvc.perform(get("/jobs").header("Authorization", bearer("publisher"))).andExpect(status().isOk());
        mvc.perform(delete("/jobs/" + UUID.randomUUID()).header("Authorization", bearer("publisher")))
                .andExpect(status().isNoContent());
    }

    @Test
    void adminCanDoEverything() throws Exception {
        Job job = mock(Job.class);
        when(job.getId()).thenReturn(UUID.randomUUID());
        when(service.createJob(any())).thenReturn(job);
        when(service.getAll()).thenReturn(List.of());

        mvc.perform(post("/jobs").header("Authorization", bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_JOB))
                .andExpect(status().isCreated());
        mvc.perform(get("/jobs").header("Authorization", bearer("admin"))).andExpect(status().isOk());
        mvc.perform(delete("/jobs/" + UUID.randomUUID()).header("Authorization", bearer("admin")))
                .andExpect(status().isNoContent());
    }

    @Test
    void tokenWithoutArcRoleIsForbidden() throws Exception {
        mvc.perform(get("/jobs").header("Authorization", bearer("norole"))).andExpect(status().isForbidden());
        mvc.perform(get("/jobs").header("Authorization", bearer("unknownrole"))).andExpect(status().isForbidden());
    }

    @Test
    void unknownPathsAreDeniedEvenWhenAuthenticated() throws Exception {
        mvc.perform(get("/other").header("Authorization", bearer("admin"))).andExpect(status().isForbidden());
    }
}
