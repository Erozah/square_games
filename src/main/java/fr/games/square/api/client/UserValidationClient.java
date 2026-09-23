package fr.games.square.api.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;

import java.util.UUID;

public class UserValidationClient {
    private final RestClient restClient;

    public UserValidationClient(@Value("${users.services.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public boolean isValidUser(UUID userId) {
        try {
            Boolean isValid = restClient.get()
                    .uri("/users/{id}/valid", userId)
                    .retrieve()
                    .body(Boolean.class);
            return Boolean.TRUE.equals(isValid);
        } catch (Exception e) {
            return false;
        }
    }
}
