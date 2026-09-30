package fr.games.square.api.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Service
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

    public String login(String username, String password) {
        try {
            java.util.Map<String, String> body = java.util.Map.of("username", username, "password", password);
            java.util.Map response = restClient.post()
                    .uri("/auth/login")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(java.util.Map.class);
            if (response != null) {
                Object token = response.get("token");
                if (token == null) {
                    token = response.get("Token");
                }
                return token != null ? token.toString() : null;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public java.util.Map<String, Object> register(String username, String email, String password) {
        try {
            java.util.Map<String, String> body = java.util.Map.of(
                    "username", username,
                    "email", email,
                    "password", password
            );
            return restClient.post()
                    .uri("/users")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(java.util.Map.class);
        } catch (Exception e) {
            return null;
        }
    }

    public record UserInfo(UUID id, String username, String email) {}

    public UserInfo findUser(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return null;
        }
        String trimmed = identifier.trim();
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/users/search")
                            .queryParam("q", trimmed)
                            .build())
                    .retrieve()
                    .body(UserInfo.class);
        } catch (Exception e) {
            return null;
        }
    }
}
